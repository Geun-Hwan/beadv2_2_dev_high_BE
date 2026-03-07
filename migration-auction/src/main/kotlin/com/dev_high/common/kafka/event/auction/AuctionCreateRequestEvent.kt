package com.dev_high.common.kafka.event.auction

import java.math.BigDecimal

data class AuctionCreateRequestEvent(
    val productId: String,
    val productName: String,
    val sellerId: String,
    val startBid: BigDecimal,
    val durationHours: Int,
)
