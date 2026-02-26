package com.dev_high.common.dto

data class SimilarProductResponse(
    val productId: String,
    val auctionId: String?,
    val imageUrl: String?,
    val score: Double,
)
