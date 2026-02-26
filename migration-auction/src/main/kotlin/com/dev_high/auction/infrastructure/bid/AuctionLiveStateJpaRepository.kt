package com.dev_high.auction.infrastructure.bid

import com.dev_high.auction.domain.AuctionLiveState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuctionLiveStateJpaRepository : JpaRepository<AuctionLiveState, String>
