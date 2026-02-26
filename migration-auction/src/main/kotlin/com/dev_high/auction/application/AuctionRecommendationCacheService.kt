package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionRecommendationResponse
import com.dev_high.config.AuctionRecommendationProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class AuctionRecommendationCacheService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val properties: AuctionRecommendationProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun get(productId: String?): AuctionRecommendationResponse? {
        if (productId.isNullOrBlank()) return null

        val key = cacheKey(productId)
        return try {
            val value = stringRedisTemplate.opsForValue().get(key)
            if (value.isNullOrBlank()) null else objectMapper.readValue(value, AuctionRecommendationResponse::class.java)
        } catch (e: Exception) {
            log.warn("failed to read recommendation cache: {}", e.message)
            null
        }
    }

    fun put(productId: String?, response: AuctionRecommendationResponse?) {
        if (productId.isNullOrBlank() || response == null) return

        val key = cacheKey(productId)
        try {
            val value = objectMapper.writeValueAsString(response)
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofMinutes(properties.cacheTtlMinutes))
        } catch (e: Exception) {
            log.warn("failed to write recommendation cache: {}", e.message)
        }
    }

    private fun cacheKey(productId: String): String = "auction:recommendation:$productId"
}
