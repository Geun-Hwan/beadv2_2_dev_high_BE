package com.dev_high.auction.domain

import com.dev_high.auction.domain.idclass.AuctionParticipationId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "auction_participation", schema = "auction")
@IdClass(AuctionParticipationId::class)
class AuctionParticipation {

    @Id
    @Column(name = "user_id", length = 20)
    var userId: String? = null

    @Id
    @Column(name = "auction_id", length = 20)
    var auctionId: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", insertable = false, updatable = false)
    var auction: Auction? = null

    @Column(name = "bid_price")
    var bidPrice: BigDecimal? = null

    @Column(name = "deposit_amount", nullable = false)
    var depositAmount: BigDecimal? = null

    @Column(name = "withdrawn_yn", length = 1, nullable = false)
    var withdrawnYn: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @Column(name = "created_by", length = 50, nullable = false, updatable = false)
    var createdBy: String? = null

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null

    @Column(name = "updated_by", length = 50, nullable = false)
    var updatedBy: String? = null

    @Column(name = "withdrawn_at")
    var withdrawnAt: OffsetDateTime? = null

    @Column(name = "deposit_refunded_yn", length = 1, nullable = false)
    var depositRefundedYn: String? = null

    @Column(name = "deposit_refunded_at")
    var depositRefundedAt: OffsetDateTime? = null

    constructor()

    constructor(ids: AuctionParticipationId, depositAmount: BigDecimal) {
        userId = ids.userId
        auctionId = ids.auctionId
        createdBy = userId
        updatedBy = userId
        withdrawnYn = "N"
        depositRefundedYn = "N"
        bidPrice = BigDecimal.ZERO
        this.depositAmount = depositAmount
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

    fun placeBid(bidPrice: BigDecimal) {
        this.bidPrice = bidPrice
    }

    fun markWithdraw() {
        withdrawnYn = "Y"
        withdrawnAt = OffsetDateTime.now()
        updatedBy = userId
    }

    fun markDepositRefunded() {
        depositRefundedYn = "Y"
        depositRefundedAt = OffsetDateTime.now()
        updatedBy = "SYSTEM"
    }
}
