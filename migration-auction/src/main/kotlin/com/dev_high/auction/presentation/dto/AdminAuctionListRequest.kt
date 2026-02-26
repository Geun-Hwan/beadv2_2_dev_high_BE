package com.dev_high.auction.presentation.dto

import com.dev_high.auction.domain.AuctionStatus
import org.springframework.format.annotation.DateTimeFormat
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AdminAuctionListRequest(
    val status: AuctionStatus?,
    val deletedYn: String?,
    val productId: String?,
    val sellerId: String?,
    val minBid: BigDecimal?,
    val maxBid: BigDecimal?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val startFrom: OffsetDateTime?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val startTo: OffsetDateTime?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val endFrom: OffsetDateTime?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val endTo: OffsetDateTime?,
)
