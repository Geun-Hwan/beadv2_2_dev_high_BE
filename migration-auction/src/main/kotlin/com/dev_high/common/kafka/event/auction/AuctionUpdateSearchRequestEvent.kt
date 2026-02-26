package com.dev_high.common.kafka.event.auction

import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionUpdateSearchRequestEvent(
    val productId: String,
    val auctionId: String,
    val startBid: BigDecimal,
    val depositAmount: BigDecimal,
    val status: String,
    val auctionStartAt: OffsetDateTime,
    val auctionEndAt: OffsetDateTime,
)
