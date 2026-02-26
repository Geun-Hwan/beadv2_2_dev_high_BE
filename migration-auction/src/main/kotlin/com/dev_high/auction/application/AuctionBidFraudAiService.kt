package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionBidFraudAiResult
import com.dev_high.auction.domain.AuctionBidHistory
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class AuctionBidFraudAiService(
    private val chatClientProvider: ObjectProvider<ChatClient>,
    @Qualifier("auctionBidFraudTemplate")
    private val auctionBidFraudTemplateProvider: ObjectProvider<PromptTemplate>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun assess(startBid: String?, recentBids: List<AuctionBidHistory>?): AuctionBidFraudAiResult? {
        val chatClient = chatClientProvider.ifAvailable ?: return null
        val auctionBidFraudTemplate = auctionBidFraudTemplateProvider.ifAvailable ?: return null
        return try {
            val prompt: Prompt = auctionBidFraudTemplate.create(
                mapOf(
                    "startBid" to safe(startBid),
                    "recentBidsJson" to toJson(recentBids),
                ),
            )
            val response = chatClient.prompt(prompt).call().chatResponse() ?: return null
            val generation = response.result ?: return null
            val content = generation.output?.text
            if (content.isNullOrBlank()) return null

            val json = extractJson(content)
            if (json.isNullOrBlank()) return null

            objectMapper.readValue(json, AuctionBidFraudAiResult::class.java)
        } catch (e: Exception) {
            log.warn("ai fraud check failed: {}", e.message)
            null
        }
    }

    private fun safe(value: Any?): String = value?.toString() ?: "N/A"

    private fun toJson(recentBids: List<AuctionBidHistory>?): String {
        if (recentBids.isNullOrEmpty()) return "[]"
        return try {
            objectMapper.writeValueAsString(
                recentBids.map { bid ->
                    mapOf(
                        "userId" to bid.userId,
                        "bidPrice" to bid.bid,
                        "bidAt" to bid.createdAt?.toEpochSecond(),
                    )
                },
            )
        } catch (_: Exception) {
            "[]"
        }
    }

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
}
