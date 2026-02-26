package com.dev_high.auction.application

import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionLiveState
import com.dev_high.auction.domain.AuctionParticipation
import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.auction.domain.AuctionStatus
import com.dev_high.auction.infrastructure.bid.AuctionLiveStateJpaRepository
import com.dev_high.auction.infrastructure.bid.AuctionParticipationJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class AuctionLifecycleService(
    private val auctionRepository: AuctionRepository,
    private val auctionParticipationJpaRepository: AuctionParticipationJpaRepository,
    private val auctionLiveStateRepository: AuctionLiveStateJpaRepository,
    private val eventDispatcher: AuctionEventDispatcher,
) {
    @Transactional
    fun startNow(auctionId: String, updatedBy: String): Auction? {
        val auction = auctionRepository.findById(auctionId).orElse(null) ?: return null
        auction.startNow(updatedBy)
        afterStart(auction, updatedBy)
        return auction
    }

    fun afterStart(auction: Auction, updatedBy: String) {
        val now = OffsetDateTime.now()
        val endAt = auction.auctionEndAt
        if (endAt == null || !endAt.isAfter(now)) {
            auction.rescheduleEnd(now.plusHours(1), updatedBy)
        }
        ensureLiveState(auction)
        eventDispatcher.publishSearchUpdate(auction)
        eventDispatcher.publishAuctionStart(auction)
    }

    @Transactional
    fun startBulkProcessing(auctionIds: List<String>?): List<Auction> {
        if (auctionIds.isNullOrEmpty()) return emptyList()
        val auctions = auctionRepository.findByIdIn(auctionIds)
        auctions.forEach { afterStart(it, "SYSTEM") }
        return auctions
    }

    @Transactional
    fun endNow(auctionId: String, updatedBy: String): Auction? {
        val auction = auctionRepository.findById(auctionId).orElse(null) ?: return null
        auction.endNow(updatedBy)
        afterEnd(auction, updatedBy)
        return auction
    }

    private fun afterEnd(auction: Auction, updatedBy: String) {
        val state = auction.liveState ?: return
        val sellerId = auction.createdBy
        val highestUserId = state.highestUserId

        if (highestUserId == null) {
            try {
                eventDispatcher.publishAuctionNoBidNotification(sellerId, auction)
            } catch (e: Exception) {
                log.error("kafka send failed :{}", e.message, e)
            }
            eventDispatcher.publishSearchUpdate(auction)
            auction.changeStatus(AuctionStatus.FAILED, updatedBy)
            return
        }

        val userIds = auctionParticipationJpaRepository.findByAuctionId(auction.id ?: "")
            .map(AuctionParticipation::userId)
            .filterNotNull()

        if (userIds.isNotEmpty()) {
            try {
                eventDispatcher.publishAuctionClosedNotification(userIds, auction)
            } catch (e: Exception) {
                log.error("경매 종료 알림 실패: auctionId={}", auction.id, e)
            }

            try {
                val refundUserIds = userIds.filter { it != highestUserId }
                eventDispatcher.publishDepositRefundRequest(
                    refundUserIds,
                    auction.id,
                    auction.depositAmount ?: BigDecimal.ZERO,
                )
            } catch (e: Exception) {
                log.error("환불 요청 실패: auctionId={}", auction.id, e)
            }
        }

        try {
            val bid = state.currentBid ?: BigDecimal.ZERO
            eventDispatcher.publishOrderCreateRequest(
                auctionId = auction.id,
                productId = auction.productId,
                productName = auction.productName ?: "",
                highestUserId = highestUserId,
                sellerId = sellerId,
                bid = bid,
                depositAmount = auction.depositAmount ?: BigDecimal.ZERO,
                now = OffsetDateTime.now(),
            )
        } catch (e: Exception) {
            log.error("주문 이벤트 발행 실패: auctionId={}", auction.id, e)
        }

        eventDispatcher.publishSearchUpdate(auction)
    }

    @Transactional
    fun endBulkProcessing(auctionIds: List<String>?): List<Auction> {
        if (auctionIds.isNullOrEmpty()) return emptyList()
        val auctions = auctionRepository.findByIdIn(auctionIds)
        auctions.forEach { afterEnd(it, "SYSTEM") }
        return auctions
    }

    private fun ensureLiveState(auction: Auction) {
        val liveState = auction.liveState
        if (liveState == null) {
            auctionLiveStateRepository.save(AuctionLiveState(auction))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuctionLifecycleService::class.java)
    }
}
