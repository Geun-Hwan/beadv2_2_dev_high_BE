package com.dev_high.common.kafka.topics

object KafkaTopics {
    const val AUCTION_START_EVENT = "auction.start.event"
    const val NOTIFICATION_REQUEST = "notification.request"
    const val AUCTION_DEPOSIT_REFUND_REQUESTED = "auction.deposit.refund.requested"
    const val AUCTION_ORDER_CREATED_REQUESTED = "auction.order.created.requested"
    const val AUCTION_SEARCH_UPDATED_REQUESTED = "auction.search.updated.requested"
    const val AUCTION_CREATE_REQUESTED = "auction.create.requested"
    const val AUCTION_BID_SUCCESS_EVENT = "auction.bid.success.event"
    const val AUCTION_BID_FRAUD_CHECK_REQUESTED = "auction.bid.fraud.check.requested"
    const val DEPOSIT_AUCTION_REFUND_RESPONSE = "deposit.auction.refund.response"
    const val ORDER_AUCTION_UPDATE = "order.auction.update"
}
