package com.dev_high.auction.domain

import com.dev_high.auction.application.dto.AuctionFilterCondition
import com.dev_high.auction.application.dto.AuctionProductProjection
import org.springframework.data.domain.Page
import java.time.OffsetDateTime

interface AuctionRepository {
    fun findById(id: String): java.util.Optional<Auction>
    fun findByIdIn(ids: List<String>): List<Auction>
    fun findByProductIdAndDeletedYn(productId: String): List<Auction>
    fun findByProductId(productId: String): List<Auction>
    fun findByProductIdIn(productIds: List<String>): List<Auction>
    fun save(auction: Auction): Auction
    fun bulkUpdateStartStatus(): List<AuctionProductProjection>
    fun bulkUpdateEndStatus(): List<AuctionProductProjection>
    fun bulkUpdateStatus(auctionIds: List<String>, status: AuctionStatus): List<String>
    fun existsByProductIdAndStatusInAndDeletedYn(productId: String, statuses: List<AuctionStatus>, deletedYn: String): Boolean
    fun filterAuctions(condition: AuctionFilterCondition): Page<Auction>
    fun getEndingSoonAuctionCount(status: AuctionStatus, withinHours: Int): Long?
    fun getAuctionCount(status: AuctionStatus, asOf: OffsetDateTime?): Long?
}
