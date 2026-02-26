package com.dev_high.common.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class WinningOrderRecommendationResponse(
    val productId: String,
    val winningAmount: BigDecimal?,
    val winningDate: OffsetDateTime?,
    val status: String?,
)
