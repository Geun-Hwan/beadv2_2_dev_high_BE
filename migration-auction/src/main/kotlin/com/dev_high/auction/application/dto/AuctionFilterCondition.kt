package com.dev_high.auction.application.dto

import com.dev_high.auction.domain.AuctionStatus
import com.dev_high.auction.presentation.dto.AdminAuctionListRequest
import com.dev_high.auction.presentation.dto.UserAuctionListRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.util.StringUtils
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AuctionFilterCondition(
    val status: List<AuctionStatus>?,
    val minBid: BigDecimal?,
    val maxBid: BigDecimal?,
    val startFrom: OffsetDateTime?,
    val startTo: OffsetDateTime?,
    val endFrom: OffsetDateTime?,
    val endTo: OffsetDateTime?,
    val productId: String?,
    val sellerId: String?,
    val deletedYn: String?,
    val pageNumber: Int,
    val pageSize: Int,
    val sort: Sort,
) {
    companion object {
        fun fromUserRequest(request: UserAuctionListRequest, pageable: Pageable?): AuctionFilterCondition {
            return build(
                status = request.status,
                minBid = null,
                maxBid = null,
                startFrom = null,
                startTo = null,
                endFrom = null,
                endTo = null,
                productId = null,
                sellerId = null,
                deletedYn = "N",
                pageable = pageable,
            )
        }

        fun fromAdminRequest(request: AdminAuctionListRequest, pageable: Pageable?): AuctionFilterCondition {
            val status = request.status?.let { listOf(it) }
            return build(
                status = status,
                minBid = request.minBid,
                maxBid = request.maxBid,
                startFrom = request.startFrom,
                startTo = request.startTo,
                endFrom = request.endFrom,
                endTo = request.endTo,
                productId = request.productId,
                sellerId = request.sellerId,
                deletedYn = request.deletedYn,
                pageable = pageable,
            )
        }

        private fun build(
            status: List<AuctionStatus>?,
            minBid: BigDecimal?,
            maxBid: BigDecimal?,
            startFrom: OffsetDateTime?,
            startTo: OffsetDateTime?,
            endFrom: OffsetDateTime?,
            endTo: OffsetDateTime?,
            productId: String?,
            sellerId: String?,
            deletedYn: String?,
            pageable: Pageable?,
        ): AuctionFilterCondition {
            if (startFrom != null && startTo != null && startFrom.isAfter(startTo)) {
                throw IllegalArgumentException("시작일 From은 To 이전이어야 합니다.")
            }
            if (endFrom != null && endTo != null && endFrom.isAfter(endTo)) {
                throw IllegalArgumentException("종료일 From은 To 이전이어야 합니다.")
            }

            val pageNumber = pageable?.pageNumber ?: 0
            val pageSize = pageable?.pageSize ?: 20
            val sort = pageable?.sort ?: Sort.by("auctionStartAt").descending()

            return AuctionFilterCondition(
                status = status,
                minBid = minBid,
                maxBid = maxBid,
                startFrom = startFrom,
                startTo = startTo,
                endFrom = endFrom,
                endTo = endTo,
                productId = if (StringUtils.hasText(productId)) productId else null,
                sellerId = if (StringUtils.hasText(sellerId)) sellerId else null,
                deletedYn = if (StringUtils.hasText(deletedYn)) deletedYn else null,
                pageNumber = pageNumber,
                pageSize = pageSize,
                sort = sort,
            )
        }
    }
}
