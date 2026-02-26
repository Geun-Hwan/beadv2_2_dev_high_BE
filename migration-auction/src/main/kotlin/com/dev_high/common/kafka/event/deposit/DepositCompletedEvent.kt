package com.dev_high.common.kafka.event.deposit

import java.math.BigDecimal

data class DepositCompletedEvent(
    val userIds: List<String>,
    val auctionId: String,
    val amount: BigDecimal,
    val type: String,
) {
    companion object {
        fun of(
            userIds: List<String>,
            auctionId: String,
            amount: BigDecimal,
            type: String,
        ): DepositCompletedEvent = DepositCompletedEvent(userIds, auctionId, amount, type)
    }
}
