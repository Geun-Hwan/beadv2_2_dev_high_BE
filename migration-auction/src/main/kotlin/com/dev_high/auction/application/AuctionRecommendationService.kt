package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionRecommendationResponse
import com.dev_high.auction.application.dto.AuctionRecommendationResponse.AuctionAiRecommendationResult
import com.dev_high.auction.application.dto.ProductInfoSummary
import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.common.dto.ApiResponseDto
import com.dev_high.common.dto.SimilarProductResponse
import com.dev_high.common.dto.WinningOrderRecommendationResponse
import com.dev_high.config.AuctionRecommendationProperties
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.OffsetDateTime
import java.util.LinkedHashSet
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max

@Service
class AuctionRecommendationService(
    private val restTemplate: RestTemplate,
    private val properties: AuctionRecommendationProperties,
    private val auctionRepository: AuctionRepository,
    private val auctionRecommendationAiService: AuctionRecommendationAiService,
    private val cacheService: AuctionRecommendationCacheService,
    private val assembler: AuctionRecommendationAssembler,
) {
    fun recommend(productId: String): AuctionRecommendationResponse {
        val cached = cacheService.get(productId)
        if (cached != null) {
            return cached
        }

        val productInfo = fetchProductInfo(productId)
        var similars = fetchSimilarProducts(productId)
        log.info("recommendation: productId={}, similars={}", productId, similars.size)

        val minSimilarity = properties.minSimilarity
        if (minSimilarity > 0.0) {
            similars = similars.filter { it.score >= minSimilarity }
        }

        val similarProductIds = buildSimilarProductIds(similars)
        log.info("recommendation: similarProductIds={}", similarProductIds)
        val auctionCountIds = buildAuctionCountIds(productId, similars)
        val auctionCount = countAuctions(auctionCountIds)
        log.info("recommendation: auctionCountIds={}, auctionCount={}", auctionCountIds, auctionCount)

        if (similars.isEmpty()) {
            val response = assembler.baseResponse(
                productId = productId,
                available = false,
                message = "유사 상품 데이터가 없습니다.",
                referencePrice = null,
                recommendedStartBid = null,
                priceRangeMin = null,
                priceRangeMax = null,
                recommendedStartAt = null,
                recommendedEndAt = null,
                winningPriceMin = null,
                winningPriceMax = null,
                winningPriceAvg = null,
                winningPriceMedian = null,
                auctionStartBidMin = null,
                auctionStartBidMax = null,
                auctionStartBidAvg = null,
                auctionStartBidMedian = null,
                similarProductCount = 0,
                winningOrderCount = 0,
                auctionCount = auctionCount,
                winningOrderCountPaidLike = 0,
            )
            return finalizeAndCache(productId, response, productInfo)
        }

        val auctionStartBids = fetchAuctionStartBids(similarProductIds)
        val auctionStartBidSummary = summarizePrices(auctionStartBids)
        val auctionStartBidRange = summarizeRange(auctionStartBids)
        val orders = fetchWinningOrders(similarProductIds)

        if (orders.isEmpty()) {
            val startBidBaseline = roundToHundreds(calculateStartBidFloor(auctionStartBids))
            val recommendedStartBid = roundToHundreds(buildStartBid(null, startBidBaseline))
            var rangeMin = auctionStartBidRange?.min
            var rangeMax = auctionStartBidRange?.max

            if (rangeMin == null || rangeMax == null) {
                if (recommendedStartBid != null) {
                    val percent = max(0.0, properties.rangePercent)
                    rangeMin = roundToHundreds(recommendedStartBid.multiply(BigDecimal.valueOf(1.0 - percent)))
                    rangeMax = roundToHundreds(recommendedStartBid.multiply(BigDecimal.valueOf(1.0 + percent)))
                }
            }

            val response = assembler.baseResponse(
                productId = productId,
                available = true,
                message = "낙찰 데이터가 없어 유사 경매 시작가를 기준으로 추천했습니다.",
                referencePrice = null,
                recommendedStartBid = recommendedStartBid,
                priceRangeMin = rangeMin,
                priceRangeMax = rangeMax,
                recommendedStartAt = null,
                recommendedEndAt = null,
                winningPriceMin = null,
                winningPriceMax = null,
                winningPriceAvg = null,
                winningPriceMedian = null,
                auctionStartBidMin = auctionStartBidSummary?.min,
                auctionStartBidMax = auctionStartBidSummary?.max,
                auctionStartBidAvg = auctionStartBidSummary?.avg,
                auctionStartBidMedian = auctionStartBidSummary?.median,
                similarProductCount = similars.size,
                winningOrderCount = 0,
                auctionCount = auctionCount,
                winningOrderCountPaidLike = 0,
            )
            return finalizeAndCache(productId, response, productInfo)
        }

        val similarityMap = similars.associate { it.productId to it.score }
        val winningSummary = summarizePrices(
            orders.mapNotNull { it.winningAmount }
                .filter { it > BigDecimal.ZERO },
        )

        val referencePrice = roundToHundreds(calculateReferencePrice(orders, similarityMap))
        val startBidBaseline = roundToHundreds(calculateStartBidFloor(auctionStartBids))
        val paidLikeCount = countPaidLikeOrders(orders)
        val recommendedStartBid = roundToHundreds(buildStartBid(referencePrice, startBidBaseline))
        var rangeMin = auctionStartBidRange?.min
        var rangeMax = auctionStartBidRange?.max

        if (rangeMin == null || rangeMax == null) {
            if (recommendedStartBid != null) {
                val percent = max(0.0, properties.rangePercent)
                rangeMin = roundToHundreds(recommendedStartBid.multiply(BigDecimal.valueOf(1.0 - percent)))
                rangeMax = roundToHundreds(recommendedStartBid.multiply(BigDecimal.valueOf(1.0 + percent)))
            }
        }
        if (referencePrice != null && rangeMax != null && rangeMax < referencePrice) {
            rangeMax = roundToHundreds(referencePrice)
        }

        val response = assembler.baseResponse(
            productId = productId,
            available = true,
            message = "OK",
            referencePrice = referencePrice,
            recommendedStartBid = recommendedStartBid,
            priceRangeMin = rangeMin,
            priceRangeMax = rangeMax,
            recommendedStartAt = null,
            recommendedEndAt = null,
            winningPriceMin = winningSummary?.min,
            winningPriceMax = winningSummary?.max,
            winningPriceAvg = winningSummary?.avg,
            winningPriceMedian = winningSummary?.median,
            auctionStartBidMin = auctionStartBidSummary?.min,
            auctionStartBidMax = auctionStartBidSummary?.max,
            auctionStartBidAvg = auctionStartBidSummary?.avg,
            auctionStartBidMedian = auctionStartBidSummary?.median,
            similarProductCount = similars.size,
            winningOrderCount = orders.size,
            auctionCount = auctionCount,
            winningOrderCountPaidLike = paidLikeCount,
        )
        return finalizeAndCache(productId, response, productInfo)
    }

    private fun buildStartBid(
        winningReference: BigDecimal?,
        auctionBaseline: BigDecimal?,
    ): BigDecimal? {
        if (winningReference == null && auctionBaseline == null) {
            return null
        }

        var reference = winningReference ?: auctionBaseline
        if (auctionBaseline != null && winningReference != null) {
            val winWeight = max(0.0, properties.winningBlendWeight)
            val auctionWeight = max(0.0, properties.auctionBlendWeight)
            val sum = winWeight + auctionWeight
            if (sum > 0.0) {
                val normalizedWin = winWeight / sum
                val normalizedAuction = auctionWeight / sum
                reference = winningReference.multiply(BigDecimal.valueOf(normalizedWin))
                    .add(auctionBaseline.multiply(BigDecimal.valueOf(normalizedAuction)))
            }
        }

        return reference?.multiply(BigDecimal.valueOf(properties.startBidRatio))
            ?.setScale(0, RoundingMode.HALF_UP)
    }

    private fun fetchSimilarProducts(productId: String): List<SimilarProductResponse> {
        val url = "http://SEARCH-SERVICE/api/v1/search/similar?productId=%s&limit=%d"
            .format(productId, properties.similarLimit)

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponseDto<List<SimilarProductResponse>>>() {},
            )
            response.body?.data ?: emptyList()
        } catch (e: Exception) {
            log.warn("failed to fetch similar products: {}", e.message)
            emptyList()
        }
    }

    private fun fetchProductInfo(productId: String): ProductInfoSummary? {
        if (productId.isBlank()) return null
        val url = "http://PRODUCT-SERVICE/api/v1/products/$productId"

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponseDto<ProductInfoSummary>>() {},
            )
            response.body?.data
        } catch (e: Exception) {
            log.warn("failed to fetch product info: {}", e.message)
            null
        }
    }

    private fun fetchWinningOrders(productIds: List<String>): List<WinningOrderRecommendationResponse> {
        val joined = productIds.joinToString(",")
        val url = "http://SETTLEMENT-SERVICE/api/v1/orders/winning/recommendation" +
            "?productIds=$joined" +
            "&limit=${properties.winningLimit}" +
            "&days=${properties.lookbackDays}"

        return try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponseDto<List<WinningOrderRecommendationResponse>>>() {},
            )
            response.body?.data ?: emptyList()
        } catch (e: Exception) {
            log.warn("failed to fetch winning orders: {}", e.message)
            emptyList()
        }
    }

    private fun calculateReferencePrice(
        orders: List<WinningOrderRecommendationResponse>,
        similarityMap: Map<String, Double>,
    ): BigDecimal? {
        val now = OffsetDateTime.now()
        var totalWeight = 0.0
        var weightedSum = 0.0

        for (order in orders) {
            val similarity = similarityMap[order.productId] ?: 0.0
            val winningAmount = order.winningAmount
            val winningDate = order.winningDate
            if (similarity <= 0.0 || winningAmount == null || winningDate == null) {
                continue
            }

            val days = max(0.0, Duration.between(winningDate, now).toHours().toDouble() / 24.0)
            val timeWeight = exp(-days / properties.timeDecayDays)
            val statusWeight = resolveStatusWeight(order.status)
            val weight = similarity * timeWeight * statusWeight

            weightedSum += weight * winningAmount.toDouble()
            totalWeight += weight
        }

        if (totalWeight > 0.0) {
            return BigDecimal.valueOf(weightedSum / totalWeight).setScale(0, RoundingMode.HALF_UP)
        }

        val amounts = orders.mapNotNull { it.winningAmount }
            .filter { it > BigDecimal.ZERO }
            .sorted()
        if (amounts.isEmpty()) {
            return null
        }

        val mid = amounts.size / 2
        return if (amounts.size % 2 == 0) {
            amounts[mid - 1].add(amounts[mid]).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
        } else {
            amounts[mid]
        }
    }

    private fun calculateStartBidFloor(bids: List<BigDecimal>?): BigDecimal? {
        val filteredBids = bids.orEmpty()
            .filter { it > BigDecimal.ZERO }
            .sorted()
        if (filteredBids.isEmpty()) {
            return null
        }

        val values = filteredBids.map { it.toDouble() }.sorted()
        val q1 = percentile(values, 0.25)
        val q3 = percentile(values, 0.75)
        val iqr = q3 - q1
        val lower = q1 - (1.5 * iqr)
        val upper = q3 + (1.5 * iqr)

        val filtered = values.filter { it >= lower && it <= upper }
        val target = if (filtered.isEmpty()) values else filtered
        val median = percentile(target, 0.5)
        return BigDecimal.valueOf(median).setScale(0, RoundingMode.HALF_UP)
    }

    private fun fetchAuctionStartBids(productIds: List<String>): List<BigDecimal> {
        if (productIds.isEmpty()) return emptyList()
        return auctionRepository.findByProductIdIn(productIds)
            .mapNotNull { it.startBid }
            .filter { it > BigDecimal.ZERO }
    }

    private fun summarizePrices(amounts: List<BigDecimal>?): PriceSummary? {
        val values = amounts.orEmpty()
            .filter { it > BigDecimal.ZERO }
            .sorted()
        if (values.isEmpty()) {
            return null
        }

        val min = values.first()
        val max = values.last()
        val sum = values.fold(BigDecimal.ZERO, BigDecimal::add)
        val avg = sum.divide(BigDecimal.valueOf(values.size.toLong()), 0, RoundingMode.HALF_UP)
        val mid = values.size / 2
        val median = if (values.size % 2 == 0) {
            values[mid - 1].add(values[mid]).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
        } else {
            values[mid]
        }

        return PriceSummary(
            min = roundToHundreds(min),
            max = roundToHundreds(max),
            avg = roundToHundreds(avg),
            median = roundToHundreds(median),
        )
    }

    private fun summarizeRange(amounts: List<BigDecimal>?): PriceRange? {
        val values = amounts.orEmpty()
            .filter { it > BigDecimal.ZERO }
            .sorted()
        if (values.isEmpty()) {
            return null
        }

        val raw = values.map { it.toDouble() }.sorted()
        val q1 = percentile(raw, 0.25)
        val q3 = percentile(raw, 0.75)
        val iqr = q3 - q1
        val lower = q1 - (1.5 * iqr)
        val upper = q3 + (1.5 * iqr)
        val filtered = raw.filter { it >= lower && it <= upper }
        val target = if (filtered.isEmpty()) raw else filtered
        var min = target.first()
        var max = target.last()

        val percent = max(0.0, properties.rangePercent)
        if (percent > 0.0) {
            min *= 1.0 - percent
            max *= 1.0 + percent
        }

        return PriceRange(
            min = roundToHundreds(BigDecimal.valueOf(min)),
            max = roundToHundreds(BigDecimal.valueOf(max)),
        )
    }

    private fun roundToHundreds(value: BigDecimal?): BigDecimal? {
        if (value == null) return null
        val hundred = BigDecimal.valueOf(100)
        return value.divide(hundred, 0, RoundingMode.HALF_UP).multiply(hundred)
    }

    private fun countAuctions(productIds: List<String>): Int {
        if (productIds.isEmpty()) return 0
        return auctionRepository.findByProductIdIn(productIds).size
    }

    private fun countPaidLikeOrders(orders: List<WinningOrderRecommendationResponse>?): Int {
        var paidLike = 0
        for (order in orders.orEmpty()) {
            when (order.status) {
                "PAID", "SHIP_STARTED", "SHIP_COMPLETED", "CONFIRM_BUY" -> paidLike++
            }
        }
        return paidLike
    }

    private fun percentile(values: List<Double>, percentile: Double): Double {
        if (values.isEmpty()) {
            return 0.0
        }
        val position = percentile * (values.size - 1)
        val lowerIndex = floor(position).toInt()
        val upperIndex = ceil(position).toInt()
        if (lowerIndex == upperIndex) {
            return values[lowerIndex]
        }
        val lower = values[lowerIndex]
        val upper = values[upperIndex]
        return lower + (upper - lower) * (position - lowerIndex)
    }

    private fun resolveStatusWeight(status: String?): Double {
        if (status.isNullOrBlank()) return 1.0
        val key = status.lowercase().replace('_', '-')
        return properties.statusWeight[key] ?: 1.0
    }

    private fun buildSimilarProductIds(similars: List<SimilarProductResponse>): List<String> {
        val ids = LinkedHashSet<String>()
        similars.forEach { ids.add(it.productId) }
        return ids.toList()
    }

    private fun buildAuctionCountIds(
        productId: String?,
        similars: List<SimilarProductResponse>,
    ): List<String> {
        val ids = LinkedHashSet<String>()
        if (!productId.isNullOrBlank()) {
            ids.add(productId)
        }
        similars.forEach { ids.add(it.productId) }
        return ids.toList()
    }

    private fun finalizeAndCache(
        productId: String,
        response: AuctionRecommendationResponse,
        productInfo: ProductInfoSummary?,
    ): AuctionRecommendationResponse {
        val aiResult = auctionRecommendationAiService.buildResult(response, productInfo)
        val finalResponse = withAiOptional(response, aiResult)
        if (aiResult != null) {
            cacheService.put(productId, finalResponse)
        }
        return finalResponse
    }

    private fun withAiOptional(
        base: AuctionRecommendationResponse,
        aiResult: AuctionAiRecommendationResult?,
    ): AuctionRecommendationResponse {
        if (aiResult == null) return base
        val normalized = if (aiResult.price == null) {
            aiResult
        } else {
            AuctionAiRecommendationResult(
                price = roundToHundreds(aiResult.price),
                reason = aiResult.reason,
            )
        }
        return assembler.withAi(base, normalized)
    }

    private data class PriceSummary(
        val min: BigDecimal?,
        val max: BigDecimal?,
        val avg: BigDecimal?,
        val median: BigDecimal?,
    )

    private data class PriceRange(
        val min: BigDecimal?,
        val max: BigDecimal?,
    )

    companion object {
        private val log = LoggerFactory.getLogger(AuctionRecommendationService::class.java)
    }
}
