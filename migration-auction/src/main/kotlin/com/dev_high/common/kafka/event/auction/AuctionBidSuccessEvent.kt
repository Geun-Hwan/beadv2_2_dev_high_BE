package com.dev_high.common.kafka.event.auction

import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionBidSuccessEvent(
    val auctionId: String,
    val userId: String,
    val bidPrice: BigDecimal,
    val bidAt: OffsetDateTime,
)
