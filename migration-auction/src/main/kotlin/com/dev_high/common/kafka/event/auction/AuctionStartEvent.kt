package com.dev_high.common.kafka.event.auction

data class AuctionStartEvent(
    val productId: String,
    val auctionId: String,
)
