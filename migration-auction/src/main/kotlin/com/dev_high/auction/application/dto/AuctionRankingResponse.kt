package com.dev_high.auction.application.dto

data class AuctionRankingResponse(
    val bidCount: Long,
    val viewCount: Long,
    val bidderCount: Long,
    val score: Double,
    val auction: AuctionResponse,
)
