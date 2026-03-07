package com.dev_high.auction.application.dto

import com.dev_high.auction.domain.AuctionBidHistory
import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionBidMessage(
    val type: String?,
    val auctionId: String?,
    val highestUserId: String?,
    val bidPrice: BigDecimal?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    val bidAt: OffsetDateTime?,
    val bidSrno: Long?,
    val currentUsers: Int,
) {
    companion object {
        fun fromEntity(history: AuctionBidHistory): AuctionBidMessage {
            return AuctionBidMessage(
                type = history.type?.toString(),
                auctionId = history.auctionId,
                highestUserId = history.userId,
                bidPrice = history.bid,
                bidAt = history.createdAt,
                bidSrno = history.id,
                currentUsers = 0,
            )
        }
    }

    fun withCurrentUsers(currentUsers: Int): AuctionBidMessage {
        return copy(currentUsers = currentUsers)
    }
}
