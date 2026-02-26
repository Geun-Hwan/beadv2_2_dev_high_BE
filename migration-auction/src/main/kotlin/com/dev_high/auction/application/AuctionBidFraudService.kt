package com.dev_high.auction.application

import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionBidHistory
import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.auction.domain.BidType
import com.dev_high.auction.infrastructure.bid.AuctionBidHistoryJpaRepository
import com.dev_high.common.kafka.event.auction.AuctionBidSuccessEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime

@Service
class AuctionBidFraudService(
    private val bidHistoryRepository: AuctionBidHistoryJpaRepository,
    private val auctionRepository: AuctionRepository,
    private val aiService: AuctionBidFraudAiService,
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun checkAndBan(event: AuctionBidSuccessEvent?) {
        if (event == null) return

        val recentBids: List<AuctionBidHistory> = bidHistoryRepository
            .findByAuctionIdAndType(
                event.auctionId,
                BidType.BID_SUCCESS,
                PageRequest.of(0, MAX_RECENT_BIDS, Sort.by(Sort.Direction.DESC, "createdAt")),
            )
            .content

        val startBid = if (recentBids.size <= 1) findStartBid(event.auctionId) else null
        val result = aiService.assess(startBid, recentBids)

        if (result?.suspected != true) return

        val banMinutes = normalizeBanMinutes(result.banMinutes)
        val now = OffsetDateTime.now().withSecond(0).withNano(0)
        val until = now.plusMinutes(banMinutes.toLong())
        val key = AuctionBidBanSupport.banKey(event.auctionId, event.userId)

        try {
            val payload = hashMapOf<String, Any>(
                "auctionId" to event.auctionId,
                "userId" to event.userId,
                "bidPrice" to safe(event.bidPrice),
                "reason" to safeString(result.reason),
                "bannedUntil" to until.toString(),
                "banMinutes" to banMinutes,
                "checkedAt" to now.toString(),
            )
            val value = objectMapper.writeValueAsString(payload)
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofMinutes(banMinutes.toLong()))
        } catch (e: Exception) {
            log.warn("failed to write fraud ban: {}", e.message)
        }
    }

    private fun normalizeBanMinutes(banMinutes: Int?): Int {
        if (banMinutes == null) return MIN_BAN_MINUTES
        if (banMinutes < MIN_BAN_MINUTES) return MIN_BAN_MINUTES
        if (banMinutes > MAX_BAN_MINUTES) return MAX_BAN_MINUTES
        return banMinutes
    }

    private fun safe(value: BigDecimal?): String = value?.toPlainString() ?: ""

    private fun findStartBid(auctionId: String?): String {
        if (auctionId == null) return ""
        return auctionRepository.findById(auctionId)
            .map(Auction::startBid)
            .map { it?.toPlainString() ?: "" }
            .orElse("")
    }

    private fun safeString(value: String?): String = value ?: ""

    companion object {
        private val log = LoggerFactory.getLogger(AuctionBidFraudService::class.java)
        private const val MAX_RECENT_BIDS = 30
        private const val MIN_BAN_MINUTES = 1
        private const val MAX_BAN_MINUTES = 30
    }
}
