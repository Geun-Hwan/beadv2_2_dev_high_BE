package com.dev_high.auction.application.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionRecommendationResponse(
    val productId: String,
    val available: Boolean,
    val message: String,
    val referencePrice: BigDecimal?,
    val recommendedStartBid: BigDecimal?,
    val priceRangeMin: BigDecimal?,
    val priceRangeMax: BigDecimal?,
    val aiResult: AuctionAiRecommendationResult?,
    val recommendedStartAt: OffsetDateTime?,
    val recommendedEndAt: OffsetDateTime?,
    val winningPriceMin: BigDecimal?,
    val winningPriceMax: BigDecimal?,
    val winningPriceAvg: BigDecimal?,
    val winningPriceMedian: BigDecimal?,
    val auctionStartBidMin: BigDecimal?,
    val auctionStartBidMax: BigDecimal?,
    val auctionStartBidAvg: BigDecimal?,
    val auctionStartBidMedian: BigDecimal?,
    val similarProductCount: Int,
    val winningOrderCount: Int,
    val auctionCount: Int,
    val winningOrderCountPaidLike: Int,
) {
    data class AuctionAiRecommendationResult(
        val price: BigDecimal?,
        val reason: String?,
    )
}
