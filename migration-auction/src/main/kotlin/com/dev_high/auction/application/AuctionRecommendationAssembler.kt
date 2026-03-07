package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionRecommendationResponse
import com.dev_high.auction.application.dto.AuctionRecommendationResponse.AuctionAiRecommendationResult
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class AuctionRecommendationAssembler {
    fun baseResponse(
        productId: String,
        available: Boolean,
        message: String,
        referencePrice: BigDecimal?,
        recommendedStartBid: BigDecimal?,
        priceRangeMin: BigDecimal?,
        priceRangeMax: BigDecimal?,
        recommendedStartAt: OffsetDateTime?,
        recommendedEndAt: OffsetDateTime?,
        winningPriceMin: BigDecimal?,
        winningPriceMax: BigDecimal?,
        winningPriceAvg: BigDecimal?,
        winningPriceMedian: BigDecimal?,
        auctionStartBidMin: BigDecimal?,
        auctionStartBidMax: BigDecimal?,
        auctionStartBidAvg: BigDecimal?,
        auctionStartBidMedian: BigDecimal?,
        similarProductCount: Int,
        winningOrderCount: Int,
        auctionCount: Int,
        winningOrderCountPaidLike: Int,
    ): AuctionRecommendationResponse {
        return AuctionRecommendationResponse(
            productId = productId,
            available = available,
            message = message,
            referencePrice = referencePrice,
            recommendedStartBid = recommendedStartBid,
            priceRangeMin = priceRangeMin,
            priceRangeMax = priceRangeMax,
            aiResult = null,
            recommendedStartAt = recommendedStartAt,
            recommendedEndAt = recommendedEndAt,
            winningPriceMin = winningPriceMin,
            winningPriceMax = winningPriceMax,
            winningPriceAvg = winningPriceAvg,
            winningPriceMedian = winningPriceMedian,
            auctionStartBidMin = auctionStartBidMin,
            auctionStartBidMax = auctionStartBidMax,
            auctionStartBidAvg = auctionStartBidAvg,
            auctionStartBidMedian = auctionStartBidMedian,
            similarProductCount = similarProductCount,
            winningOrderCount = winningOrderCount,
            auctionCount = auctionCount,
            winningOrderCountPaidLike = winningOrderCountPaidLike,
        )
    }

    fun withAi(
        base: AuctionRecommendationResponse,
        aiResult: AuctionAiRecommendationResult,
    ): AuctionRecommendationResponse {
        return base.copy(aiResult = aiResult)
    }
}
