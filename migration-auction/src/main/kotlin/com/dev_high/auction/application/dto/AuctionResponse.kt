package com.dev_high.auction.application.dto

import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionStatus
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionResponse(
    val id: String?,
    val productId: String?,
    val productName: String?,
    val status: AuctionStatus?,
    val startBid: BigDecimal?,
    val currentBid: BigDecimal,
    val highestUserId: String,
    val auctionStartAt: OffsetDateTime?,
    val auctionEndAt: OffsetDateTime?,
    val depositAmount: BigDecimal?,
    val deletedYn: Boolean,
    val sellerId: String?,
) {
    companion object {
        fun fromEntity(auction: Auction): AuctionResponse {
            val liveState = auction.liveState
            val current = liveState?.currentBid ?: BigDecimal.ZERO
            val highestUserId = liveState?.highestUserId ?: ""
            val delYn = auction.deletedYn == "Y"

            return AuctionResponse(
                id = auction.id,
                productId = auction.productId,
                productName = auction.productName,
                status = auction.status,
                startBid = auction.startBid,
                currentBid = current,
                highestUserId = highestUserId,
                auctionStartAt = auction.auctionStartAt,
                auctionEndAt = auction.auctionEndAt,
                depositAmount = auction.depositAmount,
                deletedYn = delYn,
                sellerId = auction.sellerId,
            )
        }
    }
}
