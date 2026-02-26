package com.dev_high.admin.applicaiton.dto

import com.dev_high.auction.domain.AuctionStatus

data class DashboardAuctionStatusRatioItem(
    val status: AuctionStatus,
    val count: Long,
)
