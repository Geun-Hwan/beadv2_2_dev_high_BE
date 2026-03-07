package com.dev_high.auction.presentation.dto

import com.dev_high.auction.domain.AuctionStatus

data class UserAuctionListRequest(
    val status: List<AuctionStatus>,
)
