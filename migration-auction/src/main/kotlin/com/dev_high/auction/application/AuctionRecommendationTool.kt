package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionRecommendationResponse.AuctionAiRecommendationResult
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class AuctionRecommendationTool {
    @Tool(
        name = "auction_recommendation",
        description = "Return auction start bid recommendation with price and reason.",
        returnDirect = true,
    )
    fun recommend(
        @ToolParam(description = "Recommended start bid price as an integer.") price: BigDecimal,
        @ToolParam(description = "Reason in Korean, 2-3 sentences.") reason: String,
    ): AuctionAiRecommendationResult {
        return AuctionAiRecommendationResult(price, reason)
    }
}
