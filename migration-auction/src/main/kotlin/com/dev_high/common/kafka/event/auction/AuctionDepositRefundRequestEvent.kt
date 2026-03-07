package com.dev_high.common.kafka.event.auction

import java.math.BigDecimal

data class AuctionDepositRefundRequestEvent(
    val userIds: List<String>,
    val auctionId: String,
    val depositAmount: BigDecimal,
)
