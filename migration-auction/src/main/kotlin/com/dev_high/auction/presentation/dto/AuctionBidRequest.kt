package com.dev_high.auction.presentation.dto

import java.math.BigDecimal

/**
 * 임시 마이그레이션 샘플: Java record AuctionBidRequest 대응 Kotlin 버전.
 */
data class AuctionBidRequest(
    val bidPrice: BigDecimal,
    val depositAmount: BigDecimal,
) {
    fun toBidCommand(): BigDecimal = bidPrice

    fun toDepositCommand(): BigDecimal = depositAmount
}
