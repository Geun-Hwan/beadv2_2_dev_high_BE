package com.dev_high.auction.application

import com.dev_high.auction.application.AuctionSummaryCacheService.AuctionSummary
import com.dev_high.auction.application.dto.AuctionRankingResponse
import com.dev_high.auction.application.dto.AuctionResponse
import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.auction.infrastructure.bid.AuctionLiveStateJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Service
import java.time.Duration
import java.math.BigDecimal
import java.util.Collections
import java.util.Objects

@Service
class AuctionRankingService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val auctionRepository: AuctionRepository,
    private val auctionSummaryCacheService: AuctionSummaryCacheService,
    private val auctionLiveStateJpaRepository: AuctionLiveStateJpaRepository,
) {
    @Value("\${auction.ranking.bid-weight:1.0}")
    private var bidWeight: Double = 1.0

    @Value("\${auction.ranking.view-weight:1.0}")
    private var viewWeight: Double = 1.0

    @Value("\${auction.ranking.bidder-weight:3.0}")
    private var bidderWeight: Double = 3.0

    @Value("\${auction.ranking.view-window-minutes:1}")
    private var viewWindowMinutes: Long = 1

    fun incrementBidCount(auctionId: String) {
        incrementStat(auctionId, FIELD_BID_COUNT, 1L, bidWeight)
    }

    fun registerBidder(auctionId: String?, userId: String?) {
        if (auctionId == null || userId == null) return

        val added = stringRedisTemplate.opsForSet().add(bidderSetKey(auctionId), userId) == 1L
        if (added) {
            incrementStat(auctionId, FIELD_BIDDER_COUNT, 1L, bidderWeight)
        }
    }

    fun incrementViewCount(auctionId: String?, dedupKey: String?) {
        if (auctionId == null || dedupKey == null) return

        val viewDedupKey = viewDedupKey(auctionId, dedupKey)
        val firstView = stringRedisTemplate.opsForValue().setIfAbsent(
            viewDedupKey,
            "1",
            Duration.ofMinutes(viewWindowMinutes),
        )
        if (firstView == true) {
            incrementStat(auctionId, FIELD_VIEW_COUNT, 1L, viewWeight)
        }
    }

    fun getTodayTop(limit: Int): List<AuctionRankingResponse> {
        if (limit <= 0) return Collections.emptyList()

        val topEntries = stringRedisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, (limit - 1).toLong())
        if (topEntries.isNullOrEmpty()) return Collections.emptyList()

        val auctionIds = topEntries.mapNotNull { it.value }
        val summaryMap = loadSummaries(auctionIds)

        return topEntries.mapNotNull { entry ->
            val auctionId = entry.value ?: return@mapNotNull null
            val summary = summaryMap[auctionId] ?: return@mapNotNull null

            val statsKey = statsKey(auctionId)
            val stats = stringRedisTemplate.opsForHash<String, Any>()
                .multiGet(statsKey, listOf(FIELD_BID_COUNT, FIELD_VIEW_COUNT, FIELD_BIDDER_COUNT))
            val bidCount = parseLong(stats, 0)
            val viewCount = parseLong(stats, 1)
            val bidderCount = parseLong(stats, 2)
            val score = entry.score ?: 0.0

            AuctionRankingResponse(
                bidCount = bidCount,
                viewCount = viewCount,
                bidderCount = bidderCount,
                score = score,
                auction = buildAuctionResponse(summary),
            )
        }
    }

    private fun incrementStat(auctionId: String, field: String, delta: Long, weight: Double) {
        val statsKey = statsKey(auctionId)
        stringRedisTemplate.opsForHash<String, String>().increment(statsKey, field, delta)
        stringRedisTemplate.opsForZSet().incrementScore(RANKING_KEY, auctionId, delta * weight)
    }

    private fun statsKey(auctionId: String): String = auctionId

    private fun viewDedupKey(auctionId: String, sessionId: String): String {
        return "$VIEW_DEDUP_KEY_PREFIX$auctionId:${hashKey(sessionId)}"
    }

    private fun bidderSetKey(auctionId: String): String = "$BIDDER_SET_KEY_PREFIX$auctionId"

    private fun parseLong(values: List<Any?>?, index: Int): Long {
        if (values == null || values.size <= index) return 0L
        val value = values[index] ?: return 0L
        if (value is Number) return value.toLong()
        return value.toString().toLongOrNull() ?: 0L
    }

    private fun hashKey(value: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(value.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder(hash.size * 2)
            for (b in hash) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (_: Exception) {
            Integer.toHexString(value.hashCode())
        }
    }

    private fun loadSummaries(auctionIds: List<String>): MutableMap<String, AuctionSummary> {
        if (auctionIds.isEmpty()) return mutableMapOf()

        val summaryMap = auctionIds
            .filter(Objects::nonNull)
            .map { id -> id to auctionSummaryCacheService.getSummary(id) }
            .filter { it.second != null }
            .associate { it.first to it.second!! }
            .toMutableMap()

        val missingIds = auctionIds.filter { !summaryMap.containsKey(it) }
        if (missingIds.isEmpty()) return summaryMap

        val auctions: List<Auction> = auctionRepository.findByIdIn(missingIds)
        for (auction in auctions) {
            val liveState = auctionLiveStateJpaRepository.findById(auction.id ?: "").orElse(null)
            auctionSummaryCacheService.upsert(auction, liveState)
            val summary = auctionSummaryCacheService.getSummary(auction.id)
            if (summary != null) {
                summaryMap[auction.id ?: ""] = summary
            }
        }
        return summaryMap
    }

    private fun buildAuctionResponse(summary: AuctionSummary): AuctionResponse {
        return AuctionResponse(
            id = summary.id,
            productId = summary.productId,
            productName = summary.productName,
            status = summary.status,
            startBid = summary.startBid,
            currentBid = summary.currentBid ?: BigDecimal.ZERO,
            highestUserId = summary.highestUserId,
            auctionStartAt = summary.auctionStartAt,
            auctionEndAt = summary.auctionEndAt,
            depositAmount = summary.depositAmount,
            deletedYn = summary.deletedYn,
            sellerId = summary.sellerId,
        )
    }

    companion object {
        private const val RANKING_KEY = "auction:ranking:today"
        private const val VIEW_DEDUP_KEY_PREFIX = "auction:viewed:"
        private const val BIDDER_SET_KEY_PREFIX = "auction:bidder:"
        private const val FIELD_BID_COUNT = "bidCount"
        private const val FIELD_VIEW_COUNT = "viewCount"
        private const val FIELD_BIDDER_COUNT = "bidderCount"
    }
}
