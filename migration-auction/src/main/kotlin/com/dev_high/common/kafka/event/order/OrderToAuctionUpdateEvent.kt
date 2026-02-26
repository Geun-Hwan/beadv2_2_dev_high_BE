package com.dev_high.common.kafka.event.order

data class OrderToAuctionUpdateEvent(
    val auctionIds: List<String>,
    val status: String,
)
