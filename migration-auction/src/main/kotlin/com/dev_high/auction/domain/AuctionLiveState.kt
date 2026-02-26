package com.dev_high.auction.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "auction_live_state", schema = "auction")
class AuctionLiveState {

    @Id
    @Column(name = "auction_id", length = 20)
    var auctionId: String? = null

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "auction_id", updatable = false, insertable = false)
    var auction: Auction? = null

    @Column(name = "current_bid")
    var currentBid: BigDecimal? = null

    @Column(name = "highest_user_id", length = 20)
    var highestUserId: String? = null

    @Column(name = "version", nullable = false)
    @Version
    var version: Long? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null

    constructor()

    constructor(auction: Auction) {
        this.auction = auction
        auctionId = auction.id
        currentBid = BigDecimal.ZERO
        updatedAt = OffsetDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }

    fun update(highestUserId: String, currentBid: BigDecimal) {
        this.highestUserId = highestUserId
        this.currentBid = currentBid
    }
}
