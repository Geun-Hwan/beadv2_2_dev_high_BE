package com.dev_high.auction.presentation.dto

import com.dev_high.auction.domain.AuctionStatus
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionRequest(
    val productId: String,
    val startBid: BigDecimal,
    val status: List<AuctionStatus>,
    val auctionStartAt: OffsetDateTime,
    val auctionEndAt: OffsetDateTime,
    val sellerId: String,
    val productName: String,
)
