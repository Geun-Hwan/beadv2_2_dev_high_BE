package com.dev_high.common.kafka.event.product

data class ProductCreateSearchRequestEvent(
    val productId: String,
    val productName: String,
    val categories: List<String>,
    val description: String,
    val imageUrl: String,
    val status: String,
    val sellerId: String,
)
