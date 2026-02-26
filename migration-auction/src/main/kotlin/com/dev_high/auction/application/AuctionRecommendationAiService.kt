package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionRecommendationResponse
import com.dev_high.auction.application.dto.AuctionRecommendationResponse.AuctionAiRecommendationResult
import com.dev_high.auction.application.dto.ProductInfoSummary
import com.dev_high.config.AuctionRecommendationProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class AuctionRecommendationAiService(
    private val chatClientProvider: ObjectProvider<ChatClient>,
    @Qualifier("auctionRecommendationTemplate")
    private val auctionRecommendationTemplateProvider: ObjectProvider<PromptTemplate>,
    private val properties: AuctionRecommendationProperties,
    private val objectMapper: ObjectMapper,
    private val auctionRecommendationTool: AuctionRecommendationTool,
) {
    fun buildResult(
        response: AuctionRecommendationResponse,
        productInfo: ProductInfoSummary?,
    ): AuctionAiRecommendationResult? {
        if (!properties.aiEnabled) {
            return null
        }
        val chatClient = chatClientProvider.ifAvailable ?: return null
        val auctionRecommendationTemplate = auctionRecommendationTemplateProvider.ifAvailable ?: return null

        return try {
            val prompt = auctionRecommendationTemplate.create(
                mapOf(
                    "productId" to safe(response.productId),
                    "productName" to safe(productInfo?.name),
                    "productDescription" to safe(productInfo?.description),
                    "categoryNames" to safe(buildCategoryNames(productInfo)),
                    "referencePrice" to safe(response.referencePrice),
                    "priceRangeMin" to safe(response.priceRangeMin),
                    "priceRangeMax" to safe(response.priceRangeMax),
                    "winningPriceMin" to safe(response.winningPriceMin),
                    "winningPriceMax" to safe(response.winningPriceMax),
                    "winningPriceAvg" to safe(response.winningPriceAvg),
                    "winningPriceMedian" to safe(response.winningPriceMedian),
                    "auctionStartBidMin" to safe(response.auctionStartBidMin),
                    "auctionStartBidMax" to safe(response.auctionStartBidMax),
                    "auctionStartBidAvg" to safe(response.auctionStartBidAvg),
                    "auctionStartBidMedian" to safe(response.auctionStartBidMedian),
                    "similarCount" to response.similarProductCount,
                    "winningCount" to response.winningOrderCount,
                    "auctionCount" to response.auctionCount,
                    "dataNotes" to buildDataNotes(response),
                ),
            )

            val chatResponse = chatClient.prompt(prompt)
                .tools(auctionRecommendationTool)
                .call()
                .chatResponse()
                ?: return null
            val content = chatResponse.result?.output?.text
            if (content.isNullOrBlank()) {
                return null
            }
            val json = extractJson(content) ?: return null
            val result = objectMapper.readValue(json, AuctionAiRecommendationResult::class.java)
            if (result.reason.isNullOrBlank()) return null
            result
        } catch (e: Exception) {
            log.warn("ai recommendation failed: {}", e.message)
            null
        }
    }

    private fun buildDataNotes(response: AuctionRecommendationResponse): String {
        val sb = StringBuilder()
        if (response.similarProductCount <= 0) {
            sb.append("유사 상품 데이터가 부족합니다. ")
        }
        if (response.winningOrderCount <= 0) {
            sb.append("낙찰 데이터가 부족합니다. ")
        }
        if (response.auctionCount <= 0) {
            sb.append("등록된 경매 데이터가 부족합니다. ")
        }
        return sb.toString().trim()
    }

    private fun buildCategoryNames(productInfo: ProductInfoSummary?): String {
        if (productInfo?.categories == null) {
            return ""
        }
        return productInfo.categories
            .mapNotNull { it.name }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
    }

    private fun safe(value: Any?): String = value?.toString() ?: ""

    private fun extractJson(content: String): String? {
        val trimmed = content.trim()
        if (trimmed.startsWith("```")) {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuctionRecommendationAiService::class.java)
    }
}
