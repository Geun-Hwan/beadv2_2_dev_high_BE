package com.dev_high.auction.application.dto

data class AuctionBidBanStatusResponse(
    val banned: Boolean,
    val bannedUntil: String?,
    val remainingSeconds: Long,
    val reason: String?,
) {
    companion object {
        fun notBanned(): AuctionBidBanStatusResponse =
            AuctionBidBanStatusResponse(false, null, 0L, null)
    }
}
