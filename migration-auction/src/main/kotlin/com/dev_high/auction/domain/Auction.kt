package com.dev_high.auction.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime

@Entity
@Table(name = "auction", schema = "auction")
class Auction {

    @Id
    @Column(length = 20)
    var id: String? = null

    @Column(name = "product_id", nullable = false)
    var productId: String? = null

    @Column(name = "seller_id", length = 20)
    var sellerId: String? = null

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    var status: AuctionStatus? = null

    @OneToOne(mappedBy = "auction", fetch = FetchType.LAZY)
    var liveState: AuctionLiveState? = null

    @Column(name = "product_name")
    var productName: String? = null

    @Column(name = "start_bid", nullable = false)
    var startBid: BigDecimal? = null

    @Column(name = "auction_start_at", nullable = false)
    var auctionStartAt: OffsetDateTime? = null

    @Column(name = "auction_end_at", nullable = false)
    var auctionEndAt: OffsetDateTime? = null

    @Column(name = "deposit_amount")
    var depositAmount: BigDecimal? = null

    @Column(name = "deleted_yn", nullable = false)
    var deletedYn: String? = null

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null

    @Column(name = "created_by", length = 50, nullable = false)
    var createdBy: String? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    @Column(name = "updated_by", length = 50, nullable = false)
    var updatedBy: String? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()

    constructor()

    constructor(
        startBid: BigDecimal,
        auctionStartAt: OffsetDateTime,
        auctionEndAt: OffsetDateTime,
        creatorId: String,
        productId: String,
        productName: String,
        sellerId: String,
    ) {
        status = AuctionStatus.READY
        this.productId = productId
        this.productName = productName
        this.startBid = startBid
        this.sellerId = sellerId
        depositAmount = depositMax(startBid)
        this.auctionStartAt = auctionStartAt
        this.auctionEndAt = auctionEndAt
        createdBy = creatorId
        updatedBy = creatorId
        deletedYn = "N"
    }

    @PrePersist
    fun prePersist() {
        createdAt = OffsetDateTime.now()
        updatedAt = OffsetDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }

    private fun depositMax(startBid: BigDecimal): BigDecimal {
        return startBid
            .multiply(BigDecimal("0.05"))
            .divide(BigDecimal("10"), 0, RoundingMode.CEILING)
            .multiply(BigDecimal("10"))
    }

    fun modify(
        startBid: BigDecimal,
        auctionStartAt: OffsetDateTime,
        auctionEndAt: OffsetDateTime,
        updatedBy: String,
        productName: String,
    ) {
        this.startBid = startBid
        depositAmount = depositMax(startBid)
        this.auctionStartAt = auctionStartAt
        this.auctionEndAt = auctionEndAt
        this.updatedBy = updatedBy
        this.productName = productName
    }

    fun changeStatus(status: AuctionStatus, updatedBy: String) {
        this.status = status
        this.updatedBy = updatedBy
    }

    fun startNow(updatedBy: String) {
        status = AuctionStatus.IN_PROGRESS
        auctionStartAt = OffsetDateTime.now()
        this.updatedBy = updatedBy
    }

    fun endNow(updatedBy: String) {
        status = AuctionStatus.COMPLETED
        auctionEndAt = OffsetDateTime.now()
        this.updatedBy = updatedBy
    }

    fun rescheduleEnd(endAt: OffsetDateTime, updatedBy: String) {
        auctionEndAt = endAt
        this.updatedBy = updatedBy
    }

    fun remove(userId: String) {
        deletedYn = "Y"
        deletedAt = OffsetDateTime.now()
        updatedBy = userId
    }
}
