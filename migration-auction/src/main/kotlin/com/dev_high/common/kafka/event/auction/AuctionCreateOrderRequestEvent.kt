package com.dev_high.common.kafka.event.auction

import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionCreateOrderRequestEvent(
    val auctionId: String,
    val productId: String,
    val productName: String,
    val highestUserId: String,
    val sellerId: String,
    val bid: BigDecimal,
    val depositAmount: BigDecimal,
    val now: OffsetDateTime,
)
