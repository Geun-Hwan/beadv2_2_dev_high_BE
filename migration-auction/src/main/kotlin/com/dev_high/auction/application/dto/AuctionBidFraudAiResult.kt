package com.dev_high.auction.application.dto

data class AuctionBidFraudAiResult(
    val suspected: Boolean?,
    val reason: String?,
    val banMinutes: Int?,
)
