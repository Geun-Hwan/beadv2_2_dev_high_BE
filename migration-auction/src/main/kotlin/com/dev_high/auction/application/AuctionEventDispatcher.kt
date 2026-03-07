package com.dev_high.auction.application

import com.dev_high.auction.domain.Auction
import com.dev_high.common.kafka.KafkaEventPublisher
import com.dev_high.common.kafka.event.NotificationRequestEvent
import com.dev_high.common.kafka.event.auction.AuctionCreateOrderRequestEvent
import com.dev_high.common.kafka.event.auction.AuctionDepositRefundRequestEvent
import com.dev_high.common.kafka.event.auction.AuctionStartEvent
import com.dev_high.common.kafka.event.auction.AuctionUpdateSearchRequestEvent
import com.dev_high.common.kafka.topics.KafkaTopics
import com.dev_high.common.type.NotificationCategory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime

@Component
class AuctionEventDispatcher(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun publishSearchUpdate(auction: Auction?) {
        if (auction == null) return
        applicationEventPublisher.publishEvent(
            AuctionUpdateSearchRequestEvent(
                productId = auction.productId ?: return,
                auctionId = auction.id ?: return,
                startBid = auction.startBid ?: BigDecimal.ZERO,
                depositAmount = auction.depositAmount ?: BigDecimal.ZERO,
                status = auction.status?.name ?: return,
                auctionStartAt = auction.auctionStartAt ?: return,
                auctionEndAt = auction.auctionEndAt ?: return,
            ),
        )
    }

    fun publishAuctionStart(auction: Auction?) {
        if (auction == null) return
        kafkaEventPublisher.publish(
            KafkaTopics.AUCTION_START_EVENT,
            AuctionStartEvent(
                productId = auction.productId ?: return,
                auctionId = auction.id ?: return,
            ),
        )
    }

    fun publishAuctionClosedNotification(userIds: List<String>?, auction: Auction?) {
        if (auction == null || userIds.isNullOrEmpty()) return
        kafkaEventPublisher.publish(
            KafkaTopics.NOTIFICATION_REQUEST,
            NotificationRequestEvent(
                userIds = userIds,
                content = "${auction.productName} 경매가 종료되었습니다.",
                targetUrl = "/auctions/${auction.id}",
                type = NotificationCategory.Type.AUCTION_CLOSED,
            ),
        )
    }

    fun publishAuctionNoBidNotification(sellerId: String?, auction: Auction?) {
        if (auction == null || sellerId == null) return
        kafkaEventPublisher.publish(
            KafkaTopics.NOTIFICATION_REQUEST,
            NotificationRequestEvent(
                userIds = listOf(sellerId),
                content = "${auction.productName} 경매가 유찰되었습니다.",
                targetUrl = "/auctions/${auction.id}",
                type = NotificationCategory.Type.AUCTION_NO_BID,
            ),
        )
    }

    fun publishDepositRefundRequest(userIds: List<String>?, auctionId: String?, depositAmount: BigDecimal) {
        if (userIds.isNullOrEmpty() || auctionId == null) return
        kafkaEventPublisher.publish(
            KafkaTopics.AUCTION_DEPOSIT_REFUND_REQUESTED,
            AuctionDepositRefundRequestEvent(userIds, auctionId, depositAmount),
        )
    }

    fun publishOrderCreateRequest(
        auctionId: String?,
        productId: String?,
        productName: String,
        highestUserId: String?,
        sellerId: String?,
        bid: BigDecimal,
        depositAmount: BigDecimal,
        now: OffsetDateTime,
    ) {
        if (auctionId == null || productId == null || highestUserId == null || sellerId == null) return
        kafkaEventPublisher.publish(
            KafkaTopics.AUCTION_ORDER_CREATED_REQUESTED,
            AuctionCreateOrderRequestEvent(
                auctionId = auctionId,
                productId = productId,
                productName = productName,
                highestUserId = highestUserId,
                sellerId = sellerId,
                bid = bid,
                depositAmount = depositAmount,
                now = now,
            ),
        )
    }
}
