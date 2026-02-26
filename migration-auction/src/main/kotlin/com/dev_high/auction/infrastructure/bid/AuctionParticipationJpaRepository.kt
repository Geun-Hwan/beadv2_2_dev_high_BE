package com.dev_high.auction.infrastructure.bid

import com.dev_high.auction.domain.AuctionParticipation
import com.dev_high.auction.domain.idclass.AuctionParticipationId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AuctionParticipationJpaRepository : JpaRepository<AuctionParticipation, AuctionParticipationId> {
    fun findByUserId(userId: String, pageable: Pageable): Page<AuctionParticipation>

    @Query("SELECT p FROM AuctionParticipation p WHERE p.auctionId = :auctionId AND p.withdrawnYn ='N' AND p.depositRefundedYn ='N'")
    fun findByAuctionId(auctionId: String): List<AuctionParticipation>

    fun findByAuctionIdAndUserIdIn(auctionId: String, userIds: List<String>): List<AuctionParticipation>
}
