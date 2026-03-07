package com.dev_high.auction.domain.idclass

import java.io.Serializable

data class AuctionParticipationId(
    var userId: String? = null,
    var auctionId: String? = null,
) : Serializable
