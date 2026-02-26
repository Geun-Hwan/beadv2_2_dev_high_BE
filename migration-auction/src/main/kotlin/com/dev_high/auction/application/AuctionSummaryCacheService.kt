package com.dev_high.auction.application

import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionLiveState
import com.dev_high.auction.domain.AuctionStatus
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class AuctionSummaryCacheService(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    fun upsert(auction: Auction?, liveState: AuctionLiveState?) {
        if (auction == null) return

        val key = summaryKey(auction.id)
        val values = hashMapOf(
            "id" to toText(auction.id),
            "productId" to toText(auction.productId),
            "productName" to toText(auction.productName),
            "status" to toText(auction.status),
            "startBid" to toText(auction.startBid),
            "currentBid" to toText(liveState?.currentBid),
            "highestUserId" to toText(liveState?.highestUserId),
            "auctionStartAt" to toText(auction.auctionStartAt),
            "auctionEndAt" to toText(auction.auctionEndAt),
            "depositAmount" to toText(auction.depositAmount),
            "deletedYn" to toText(auction.deletedYn),
            "sellerId" to toText(auction.sellerId),
        )
        stringRedisTemplate.opsForHash<String, String>().putAll(key, values)
    }

    fun upsertIfRanked(auction: Auction?, liveState: AuctionLiveState?) {
        if (auction == null) return
        val score = stringRedisTemplate.opsForZSet().score(RANKING_KEY, auction.id)
        if (score == null) return
        upsert(auction, liveState)
    }

    fun getSummary(auctionId: String?): AuctionSummary? {
        if (auctionId.isNullOrBlank()) return null

        val raw = stringRedisTemplate.opsForHash<Any, Any>().entries(summaryKey(auctionId))
        if (raw.isEmpty()) return null

        return AuctionSummary(
            id = toText(raw["id"]),
            productId = toText(raw["productId"]),
            productName = toText(raw["productName"]),
            status = parseStatus(raw["status"]),
            startBid = parseBigDecimal(raw["startBid"]),
            currentBid = parseBigDecimal(raw["currentBid"]),
            highestUserId = toText(raw["highestUserId"]),
            auctionStartAt = parseOffsetDateTime(raw["auctionStartAt"]),
            auctionEndAt = parseOffsetDateTime(raw["auctionEndAt"]),
            depositAmount = parseBigDecimal(raw["depositAmount"]),
            deletedYn = toText(raw["deletedYn"]).equals("Y", ignoreCase = true),
            sellerId = toText(raw["sellerId"]),
        )
    }

    fun delete(auctionId: String?) {
        if (auctionId.isNullOrBlank()) return
        stringRedisTemplate.delete(summaryKey(auctionId))
    }

    private fun summaryKey(auctionId: String?): String = "$SUMMARY_KEY_PREFIX$auctionId"

    private fun toText(value: Any?): String = value?.toString() ?: ""

    private fun parseStatus(value: Any?): AuctionStatus? {
        val text = toText(value)
        if (text.isBlank()) return null
        return runCatching { AuctionStatus.valueOf(text) }.getOrNull()
    }

    private fun parseBigDecimal(value: Any?): BigDecimal? {
        val text = toText(value)
        if (text.isBlank()) return null
        return runCatching { BigDecimal(text) }.getOrNull()
    }

    private fun parseOffsetDateTime(value: Any?): OffsetDateTime? {
        val text = toText(value)
        if (text.isBlank()) return null
        return runCatching { OffsetDateTime.parse(text) }.getOrNull()
    }

    data class AuctionSummary(
        val id: String,
        val productId: String,
        val productName: String,
        val status: AuctionStatus?,
        val startBid: BigDecimal?,
        val currentBid: BigDecimal?,
        val highestUserId: String,
        val auctionStartAt: OffsetDateTime?,
        val auctionEndAt: OffsetDateTime?,
        val depositAmount: BigDecimal?,
        val deletedYn: Boolean,
        val sellerId: String,
    )

    companion object {
        private const val SUMMARY_KEY_PREFIX = "auction:summary:"
        private const val RANKING_KEY = "auction:ranking:today"
    }
}
