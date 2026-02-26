package com.dev_high.auction.application.dto

import com.dev_high.auction.domain.AuctionParticipation
import com.dev_high.auction.domain.AuctionStatus
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionParticipationResponse(
    val auctionId: String?,
    val isParticipated: Boolean,
    val isWithdrawn: Boolean,
    val isRefund: Boolean,
    val depositAmount: BigDecimal,
    val withdrawnAt: OffsetDateTime?,
    val refundAt: OffsetDateTime?,
    val lastBidPrice: BigDecimal?,
    val createdAt: OffsetDateTime?,
    val status: AuctionStatus?,
    val productName: String?,
) {
    companion object {
        fun isParticipated(participation: AuctionParticipation): AuctionParticipationResponse {
            val withdrawn = participation.withdrawnYn == "Y"
            val refund = participation.depositRefundedYn == "Y"
            val auction = participation.auction
            val status = auction?.status
            val productName = auction?.productName
            return AuctionParticipationResponse(
                auctionId = participation.auctionId,
                isParticipated = true,
                isWithdrawn = withdrawn,
                isRefund = refund,
                depositAmount = participation.depositAmount ?: BigDecimal.ZERO,
                withdrawnAt = participation.withdrawnAt,
                refundAt = participation.depositRefundedAt,
                lastBidPrice = participation.bidPrice,
                createdAt = participation.createdAt,
                status = status,
                productName = productName,
            )
        }

        fun isNotParticipated(auctionId: String): AuctionParticipationResponse {
            return AuctionParticipationResponse(
                auctionId = auctionId,
                isParticipated = false,
                isWithdrawn = false,
                isRefund = false,
                depositAmount = BigDecimal.ZERO,
                withdrawnAt = null,
                refundAt = null,
                lastBidPrice = null,
                createdAt = null,
                status = null,
                productName = null,
            )
        }
    }
}
