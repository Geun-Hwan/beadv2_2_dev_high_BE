package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionBidBanStatusResponse
import com.dev_high.exception.MigrationCustomException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Service
class AuctionBidBanService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getStatus(auctionId: String?, userId: String?): AuctionBidBanStatusResponse {
        if (auctionId == null || userId == null) return AuctionBidBanStatusResponse.notBanned()

        val key = AuctionBidBanSupport.banKey(auctionId, userId)
        val value = stringRedisTemplate.opsForValue().get(key)
        if (value.isNullOrBlank()) return AuctionBidBanStatusResponse.notBanned()

        return try {
            @Suppress("UNCHECKED_CAST")
            val payload = objectMapper.readValue(value, Map::class.java) as Map<String, Any>?
            val untilRaw = payload?.get("bannedUntil") as? String
            val reason = payload?.get("reason") as? String
            val until = parseUntil(untilRaw) ?: return AuctionBidBanStatusResponse.notBanned()

            val remainingSeconds = Duration.between(OffsetDateTime.now(), until).seconds
            if (remainingSeconds <= 0) {
                stringRedisTemplate.delete(key)
                return AuctionBidBanStatusResponse.notBanned()
            }

            AuctionBidBanStatusResponse(
                banned = true,
                bannedUntil = until.toString(),
                remainingSeconds = remainingSeconds,
                reason = reason,
            )
        } catch (e: Exception) {
            log.warn("failed to read ban status: {}", e.message)
            AuctionBidBanStatusResponse.notBanned()
        }
    }

    fun assertNotBanned(auctionId: String?, userId: String?) {
        val status = getStatus(auctionId, userId)
        if (!status.banned) return

        val message = "부정 입찰 의심으로 ${status.bannedUntil}까지 입찰이 제한됩니다."
        throw MigrationCustomException(HttpStatus.TOO_MANY_REQUESTS, message)
    }

    private fun parseUntil(value: String?): OffsetDateTime? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
