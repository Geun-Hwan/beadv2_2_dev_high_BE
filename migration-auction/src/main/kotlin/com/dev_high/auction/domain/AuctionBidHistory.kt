package com.dev_high.auction.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "auction_bid_history", schema = "auction")
class AuctionBidHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auction_bid_history_seq")
    @SequenceGenerator(
        name = "auction_bid_history_seq",
        sequenceName = "auction.auction_bid_history_seq",
        allocationSize = 1,
    )
    var id: Long? = null

    @Column(name = "auction_id", length = 20, nullable = false)
    var auctionId: String? = null

    @Column(name = "bid")
    var bid: BigDecimal? = null

    @Column(name = "user_id", length = 20, nullable = false)
    var userId: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @Column(name = "type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    var type: BidType? = null

    constructor()

    constructor(auctionId: String, bid: BigDecimal, userId: String, type: BidType) {
        this.auctionId = auctionId
        this.bid = bid
        this.userId = userId
        this.type = type
    }

    @PrePersist
    fun prePersist() {
        createdAt = OffsetDateTime.now()
    }

    fun changeType(type: BidType) {
        this.type = type
    }
}
