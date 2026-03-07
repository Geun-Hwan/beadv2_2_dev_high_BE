package com.dev_high.auction.application

import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.auction.domain.AuctionStatus
import com.dev_high.auction.presentation.dto.AuctionRequest
import com.dev_high.common.context.UserContext
import com.dev_high.common.kafka.KafkaEventEnvelope
import com.dev_high.common.kafka.KafkaEventPublisher
import com.dev_high.common.kafka.event.auction.AuctionBidSuccessEvent
import com.dev_high.common.kafka.event.auction.AuctionCreateRequestEvent
import com.dev_high.common.kafka.event.auction.AuctionUpdateSearchRequestEvent
import com.dev_high.common.kafka.event.deposit.DepositCompletedEvent
import com.dev_high.common.kafka.event.order.OrderToAuctionUpdateEvent
import com.dev_high.common.kafka.topics.KafkaTopics
import com.dev_high.common.util.JsonUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.NetworkException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.dao.TransientDataAccessException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.OffsetDateTime

@Component
@Lazy(false)
class AuctionEventListener(
    private val eventPublisher: KafkaEventPublisher,
    private val auctionService: AuctionService,
    private val auctionBidFraudService: AuctionBidFraudService,
    private val recordService: BidRecordService,
    private val auctionRepository: AuctionRepository,
) {
    @KafkaListener(topics = [KafkaTopics.DEPOSIT_AUCTION_REFUND_RESPONSE])
    @Transactional
    fun refundComplete(
        envelope: KafkaEventEnvelope<*>,
        record: ConsumerRecord<*, *>,
    ) {
        val value = JsonUtil.fromPayload(envelope.payload, DepositCompletedEvent::class.java)
        try {
            if (value.type == "REFUND") {
                recordService.markDepositRefunded(value.auctionId, value.userIds)
            }
        } catch (e: Exception) {
            handleKafkaException(e, envelope)
        }
    }

    @Deprecated("legacy")
    fun depositComplete(
        envelope: KafkaEventEnvelope<*>,
        record: ConsumerRecord<*, *>,
    ) {
        val value = JsonUtil.fromPayload(envelope.payload, DepositCompletedEvent::class.java)
        try {
            if (value.type == "DEPOSIT") {
                value.userIds.forEach { id ->
                    recordService.createParticipation(value.auctionId, value.amount, id)
                }
            }
        } catch (e: Exception) {
            handleKafkaException(e, envelope)
        }
    }

    @KafkaListener(topics = [KafkaTopics.ORDER_AUCTION_UPDATE])
    fun auctionStatusUpdate(
        envelope: KafkaEventEnvelope<*>,
        record: ConsumerRecord<*, *>,
    ) {
        val value = JsonUtil.fromPayload(envelope.payload, OrderToAuctionUpdateEvent::class.java)
        try {
            auctionRepository.bulkUpdateStatus(value.auctionIds, AuctionStatus.valueOf(value.status))
        } catch (e: Exception) {
            handleKafkaException(e, envelope)
        }
    }

    @KafkaListener(topics = [KafkaTopics.AUCTION_CREATE_REQUESTED])
    @Transactional
    fun createAuction(
        envelope: KafkaEventEnvelope<*>,
        record: ConsumerRecord<*, *>,
    ) {
        val value = JsonUtil.fromPayload(envelope.payload, AuctionCreateRequestEvent::class.java)
        try {
            val start = OffsetDateTime.now()
                .plusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            val end = start.plusHours(value.durationHours.toLong())
            val request = AuctionRequest(
                productId = value.productId,
                startBid = value.startBid,
                status = null,
                auctionStartAt = start,
                auctionEndAt = end,
                sellerId = value.sellerId,
                productName = value.productName,
            )
            UserContext.set(UserContext.UserInfo("SYSTEM", ""))
            auctionService.createAuction(request, true)
        } catch (e: Exception) {
            handleKafkaException(e, envelope)
        } finally {
            UserContext.clear()
        }
    }

    @KafkaListener(topics = [KafkaTopics.AUCTION_BID_FRAUD_CHECK_REQUESTED])
    fun fraudCheck(
        envelope: KafkaEventEnvelope<*>,
        record: ConsumerRecord<*, *>,
    ) {
        val value = JsonUtil.fromPayload(envelope.payload, AuctionBidSuccessEvent::class.java)
        try {
            auctionBidFraudService.checkAndBan(value)
        } catch (e: Exception) {
            handleKafkaException(e, envelope)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: AuctionUpdateSearchRequestEvent) {
        eventPublisher.publish(KafkaTopics.AUCTION_SEARCH_UPDATED_REQUESTED, event)
    }

    private fun handleKafkaException(
        e: Exception,
        envelope: KafkaEventEnvelope<*>,
    ) {
        if (e is TransientDataAccessException || e is NetworkException) {
            log.warn(
                "일시적 오류 발생, 재시도: {}, 메시지: {}",
                e::class.simpleName,
                envelope.payload,
            )
            throw e
        }
        throw RuntimeException(e)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuctionEventListener::class.java)
    }
}
