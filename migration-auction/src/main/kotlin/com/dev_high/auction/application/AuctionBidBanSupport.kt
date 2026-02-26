package com.dev_high.auction.application

internal object AuctionBidBanSupport {
    private const val BAN_KEY_PREFIX = "auction:bid:ban:"

    fun banKey(auctionId: String, userId: String): String {
        return "$BAN_KEY_PREFIX$auctionId:$userId"
    }
}
