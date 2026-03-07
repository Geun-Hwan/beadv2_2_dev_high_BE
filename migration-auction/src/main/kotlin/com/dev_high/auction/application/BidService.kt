package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionBidMessage
import com.dev_high.auction.application.dto.AuctionParticipationResponse
import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionBidHistory
import com.dev_high.auction.domain.AuctionLiveState
import com.dev_high.auction.domain.AuctionParticipation
import com.dev_high.auction.domain.BidType
import com.dev_high.auction.domain.idclass.AuctionParticipationId
import com.dev_high.auction.infrastructure.bid.AuctionLiveStateJpaRepository
import com.dev_high.auction.infrastructure.bid.AuctionParticipationJpaRepository
import com.dev_high.common.context.UserContext
import com.dev_high.common.kafka.KafkaEventPublisher
import com.dev_high.common.kafka.event.auction.AuctionBidSuccessEvent
import com.dev_high.common.kafka.topics.KafkaTopics
import com.dev_high.exception.AlreadyWithdrawnException
import com.dev_high.exception.AuctionNotFoundException
import com.dev_high.exception.AuctionParticipationNotFoundException
import com.dev_high.exception.AuctionTimeOutOfRangeException
import com.dev_high.exception.BidPriceTooLowException
import com.dev_high.exception.OptimisticLockBidException
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class BidService(
    private val auctionLiveStateJpaRepository: AuctionLiveStateJpaRepository,
    private val auctionParticipationJpaRepository: AuctionParticipationJpaRepository,
    private val bidRecordService: BidRecordService,
    private val auctionWebSocketService: AuctionWebSocketService,
    private val auctionRankingService: AuctionRankingService,
    private val auctionSummaryCacheService: AuctionSummaryCacheService,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val auctionBidBanService: AuctionBidBanService,
) {
    fun createOrUpdateAuctionBid(auctionId: String, bidPrice: BigDecimal): AuctionParticipationResponse {
        val userId = UserContext.get().userId
        auctionBidBanService.assertNotBanned(auctionId, userId)

        val participation = auctionParticipationJpaRepository.findById(AuctionParticipationId(userId, auctionId))
            .orElseThrow { AuctionParticipationNotFoundException() }

        if (participation.withdrawnYn == "Y") {
            throw AlreadyWithdrawnException()
        }

        var history: AuctionBidHistory? = null
        var attempts = MAX_ATTEMPTS

        try {
            while (attempts-- > 0) {
                try {
                    val liveState = getOrCreateLiveState(participation.auction ?: throw AuctionNotFoundException())
                    validateBid(participation, bidPrice, liveState)
                    history = placeBid(participation, liveState, bidPrice)
                    break
                } catch (e: OptimisticLockException) {
                    if (attempts == 0) {
                        bidRecordService.recordHistory(AuctionBidHistory(auctionId, bidPrice, userId, BidType.BID_FAIL_LOCK))
                        throw OptimisticLockBidException()
                    }
                } catch (e: OptimisticLockingFailureException) {
                    if (attempts == 0) {
                        bidRecordService.recordHistory(AuctionBidHistory(auctionId, bidPrice, userId, BidType.BID_FAIL_LOCK))
                        throw OptimisticLockBidException()
                    }
                }
            }
        } finally {
            bidRecordService.saveParticipation(participation)
        }

        try {
            if (history != null) {
                auctionRankingService.registerBidder(auctionId, userId)
                auctionRankingService.incrementBidCount(auctionId)
                publishFraudCheckRequest(history)
                broadcastBid(history)
            }
        } catch (e: Exception) {
            log.warn("Post-bid handling failed: {}", e.message)
        }

        return AuctionParticipationResponse.isParticipated(participation)
    }

    private fun getOrCreateLiveState(auction: Auction): AuctionLiveState {
        return auctionLiveStateJpaRepository.findById(auction.id ?: "")
            .orElseGet {
                val newLiveState = AuctionLiveState(auction)
                try {
                    auctionLiveStateJpaRepository.saveAndFlush(newLiveState)
                } catch (e: DataIntegrityViolationException) {
                    auctionLiveStateJpaRepository.findById(auction.id ?: "")
                        .orElseThrow { AuctionNotFoundException() }
                }
            }
    }

    private fun validateBid(participation: AuctionParticipation, bidPrice: BigDecimal, liveState: AuctionLiveState) {
        val now = OffsetDateTime.now()
        val auction = liveState.auction ?: throw AuctionNotFoundException()

        if (now.isBefore(auction.auctionStartAt) || now.isAfter(auction.auctionEndAt)) {
            bidRecordService.recordHistory(
                AuctionBidHistory(
                    auction.id ?: "",
                    bidPrice,
                    participation.userId ?: "",
                    BidType.BID_FAIL_TIME,
                ),
            )
            throw AuctionTimeOutOfRangeException()
        }

        val currentBid = liveState.currentBid ?: BigDecimal.ZERO
        if (bidPrice <= currentBid) {
            bidRecordService.recordHistory(
                AuctionBidHistory(
                    auction.id ?: "",
                    bidPrice,
                    participation.userId ?: "",
                    BidType.BID_FAIL_LOW_PRICE,
                ),
            )
            throw BidPriceTooLowException()
        }
    }

    private fun placeBid(participation: AuctionParticipation, liveState: AuctionLiveState, bidPrice: BigDecimal): AuctionBidHistory {
        liveState.update(participation.userId ?: "", bidPrice)
        auctionLiveStateJpaRepository.save(liveState)
        auctionSummaryCacheService.upsertIfRanked(liveState.auction, liveState)

        participation.placeBid(bidPrice)

        return bidRecordService.recordHistory(
            AuctionBidHistory(
                liveState.auction?.id ?: "",
                bidPrice,
                participation.userId ?: "",
                BidType.BID_SUCCESS,
            ),
        )
    }

    private fun broadcastBid(history: AuctionBidHistory) {
        auctionWebSocketService.broadcastBidSuccess(AuctionBidMessage.fromEntity(history))
    }

    private fun publishFraudCheckRequest(history: AuctionBidHistory) {
        val event = AuctionBidSuccessEvent(
            history.auctionId ?: "",
            history.userId ?: "",
            history.bid ?: BigDecimal.ZERO,
            history.createdAt ?: OffsetDateTime.now(),
        )
        kafkaEventPublisher.publish(KafkaTopics.AUCTION_BID_FRAUD_CHECK_REQUESTED, event)
    }

    companion object {
        private val log = LoggerFactory.getLogger(BidService::class.java)
        private const val MAX_ATTEMPTS = 2
    }
}
