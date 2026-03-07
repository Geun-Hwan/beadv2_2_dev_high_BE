package com.dev_high.auction.infrastructure.auction

import com.dev_high.auction.application.dto.AuctionProductProjection
import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AuctionJpaRepository : JpaRepository<Auction, String> {
    fun existsByProductIdAndStatusInAndDeletedYn(productId: String, statuses: List<AuctionStatus>, deletedYn: String): Boolean
    fun findByProductIdAndDeletedYnOrderByCreatedAtDesc(productId: String, deletedYn: String): List<Auction>
    fun findByProductIdOrderByCreatedAtDesc(productId: String): List<Auction>

    @Modifying
    @Query(
        value = """
            UPDATE auction.auction
            SET status = 'IN_PROGRESS',
                updated_at = NOW(),
                updated_by = 'SYSTEM'
            WHERE status = 'READY'
              AND deleted_yn = 'N'
              AND auction_start_at <= NOW()
            RETURNING
                id,
                product_id AS productId
        """,
        nativeQuery = true,
    )
    fun bulkUpdateStart(): List<AuctionProductProjection>

    @Modifying
    @Query(
        value = """
              UPDATE auction.auction
              SET status = 'COMPLETED',
                  updated_at = NOW(),
                  updated_by = 'SYSTEM'
              WHERE status = 'IN_PROGRESS'
                AND deleted_yn = 'N'
                AND auction_end_at <= NOW()
              RETURNING
                id,
                product_id AS productId
        """,
        nativeQuery = true,
    )
    fun bulkUpdateEnd(): List<AuctionProductProjection>

    @Modifying
    @Query(
        value = """
              UPDATE auction.auction
            SET status = :status,
                updated_at = NOW(),
                updated_by = 'SYSTEM'
            WHERE id IN (:auctionIds) AND deleted_yn = 'N'
            RETURNING product_id
        """,
        nativeQuery = true,
    )
    fun bulkUpdateStatus(
        @Param("auctionIds") auctionIds: List<String>,
        @Param("status") status: String,
    ): List<String>
}
