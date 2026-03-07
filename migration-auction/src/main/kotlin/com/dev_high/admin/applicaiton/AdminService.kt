package com.dev_high.admin.applicaiton

import com.dev_high.admin.applicaiton.dto.DashboardAuctionStatusRatioItem
import com.dev_high.auction.application.AuctionLifecycleService
import com.dev_high.auction.application.dto.AuctionResponse
import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.auction.domain.AuctionStatus
import com.dev_high.common.context.UserContext
import com.dev_high.exception.AuctionNotFoundException
import com.dev_high.exception.AuctionStatusInvalidException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

@Service
class AdminService(
    private val auctionRepository: AuctionRepository,
    private val lifecycleService: AuctionLifecycleService,
) {
    @Transactional
    fun startAuctionNow(auctionId: String): AuctionResponse {
        val userId = resolveAdminUserId()
        val auction = auctionRepository.findById(auctionId).orElseThrow { AuctionNotFoundException() }
        if (auction.status != AuctionStatus.READY) {
            throw AuctionStatusInvalidException()
        }
        return AuctionResponse.fromEntity(lifecycleService.startNow(auctionId, userId)!!)
    }

    @Transactional
    fun endAuctionNow(auctionId: String): AuctionResponse {
        val userId = resolveAdminUserId()
        val auction = auctionRepository.findById(auctionId).orElseThrow { AuctionNotFoundException() }
        if (!listOf(AuctionStatus.READY, AuctionStatus.IN_PROGRESS).contains(auction.status)) {
            throw AuctionStatusInvalidException()
        }
        return AuctionResponse.fromEntity(lifecycleService.endNow(auctionId, userId)!!)
    }

    fun getAuctionCount(status: AuctionStatus): Long? = auctionRepository.getAuctionCount(status, null)

    fun getAuctionsByProductId(productId: String): List<AuctionResponse> {
        return auctionRepository.findByProductId(productId).map(AuctionResponse::fromEntity)
    }

    fun getEndingSoonAuctionCount(status: AuctionStatus, withinHours: Int): Long? {
        return auctionRepository.getEndingSoonAuctionCount(status, withinHours)
    }

    fun getAuctionStatusRatio(asOf: String?, timezone: String?): List<DashboardAuctionStatusRatioItem> {
        val target = resolveAsOf(asOf, timezone)
        return AuctionStatus.entries.map { status ->
            DashboardAuctionStatusRatioItem(status, safeCount(auctionRepository.getAuctionCount(status, target)))
        }
    }

    private fun resolveAdminUserId(): String {
        val user = UserContext.get()
        return user.userId.ifBlank { "SYSTEM" }
    }

    private fun safeCount(value: Long?): Long = value ?: 0L

    private fun resolveAsOf(asOf: String?, timezone: String?): OffsetDateTime {
        val zone = resolveZone(timezone)
        if (asOf.isNullOrBlank()) {
            return OffsetDateTime.now(zone)
        }

        try {
            return OffsetDateTime.parse(asOf)
        } catch (_: DateTimeParseException) {
        }

        try {
            return LocalDateTime.parse(asOf).atZone(zone).toOffsetDateTime()
        } catch (_: DateTimeParseException) {
        }

        try {
            return LocalDate.parse(asOf).atStartOfDay(zone).toOffsetDateTime()
        } catch (_: DateTimeParseException) {
        }

        return OffsetDateTime.now(zone)
    }

    private fun resolveZone(timezone: String?): ZoneId {
        if (timezone.isNullOrBlank()) return ZoneId.of("Asia/Seoul")
        return try {
            ZoneId.of(timezone)
        } catch (_: DateTimeException) {
            ZoneId.of("Asia/Seoul")
        }
    }
}
