package com.dev_high.auction.infrastructure.bid

import com.dev_high.auction.domain.AuctionBidHistory
import com.dev_high.auction.domain.BidType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuctionBidHistoryJpaRepository : JpaRepository<AuctionBidHistory, Long> {
    fun findByAuctionIdAndType(auctionId: String, type: BidType, pageable: Pageable): Page<AuctionBidHistory>
}
