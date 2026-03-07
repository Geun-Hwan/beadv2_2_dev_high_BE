package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionFilterCondition
import com.dev_high.auction.application.dto.AuctionResponse
import com.dev_high.auction.domain.Auction
import com.dev_high.auction.domain.AuctionLiveState
import com.dev_high.auction.domain.AuctionRepository
import com.dev_high.auction.domain.AuctionStatus
import com.dev_high.auction.infrastructure.bid.AuctionLiveStateJpaRepository
import com.dev_high.auction.presentation.dto.AdminAuctionListRequest
import com.dev_high.auction.presentation.dto.AuctionRequest
import com.dev_high.auction.presentation.dto.UserAuctionListRequest
import com.dev_high.common.context.UserContext
import com.dev_high.common.exception.CustomException
import com.dev_high.common.util.DateUtil
import com.dev_high.exception.AuctionModifyForbiddenException
import com.dev_high.exception.AuctionNotFoundException
import com.dev_high.exception.AuctionStatusInvalidException
import com.dev_high.exception.DuplicateAuctionException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.time.OffsetDateTime

@Service
class AuctionService(
    private val auctionRepository: AuctionRepository,
    private val auctionLiveStateRepository: AuctionLiveStateJpaRepository,
    private val auctionSummaryCacheService: AuctionSummaryCacheService,
    private val auctionEventDispatcher: AuctionEventDispatcher,
) {
    fun getUserAuctionList(request: UserAuctionListRequest, pageable: Pageable): Page<AuctionResponse> {
        val filter = AuctionFilterCondition.fromUserRequest(request, pageable)
        return auctionRepository.filterAuctions(filter).map(AuctionResponse::fromEntity)
    }

    fun getAdminAuctionList(request: AdminAuctionListRequest, pageable: Pageable): Page<AuctionResponse> {
        val filter = AuctionFilterCondition.fromAdminRequest(request, pageable)
        return auctionRepository.filterAuctions(filter).map(AuctionResponse::fromEntity)
    }

    fun getAuction(auctionId: String): AuctionResponse {
        val auction = auctionRepository.findById(auctionId).orElseThrow { AuctionNotFoundException() }
        return AuctionResponse.fromEntity(auction)
    }

    fun getAuctions(auctionIds: List<String>): List<AuctionResponse> {
        return auctionRepository.findByIdIn(auctionIds).map(AuctionResponse::fromEntity)
    }

    @Transactional
    fun createAuction(request: AuctionRequest, isAdmin: Boolean): AuctionResponse {
        val userId = isMine(null, isAdmin)
        validateAuction(request)

        val start = requireNotNull(request.auctionStartAt).withSecond(0).withNano(0)
        val end = requireNotNull(request.auctionEndAt).withSecond(0).withNano(0)
        val productId = requireNotNull(request.productId)
        val productName = requireNotNull(request.productName)
        val sellerId = requireNotNull(request.sellerId)
        val startBid = requireNotNull(request.startBid)

        validateAuctionTime(start, end)

        if (auctionRepository.existsByProductIdAndStatusInAndDeletedYn(
                productId,
                listOf(AuctionStatus.READY, AuctionStatus.IN_PROGRESS, AuctionStatus.COMPLETED),
                "N",
            )
        ) {
            throw DuplicateAuctionException()
        }

        val auction = auctionRepository.save(
            Auction(
                startBid,
                start,
                end,
                userId,
                productId,
                productName,
                sellerId,
            ),
        )

        val liveState = AuctionLiveState(auction)
        auctionLiveStateRepository.save(liveState)
        auctionEventDispatcher.publishSearchUpdate(auction)
        return AuctionResponse.fromEntity(auction)
    }

    @Transactional
    fun modifyAuction(auctionId: String, request: AuctionRequest, isAdmin: Boolean): AuctionResponse {
        val auction = auctionRepository.findById(auctionId).orElseThrow { AuctionNotFoundException() }

        if (auction.status != AuctionStatus.READY) {
            throw AuctionStatusInvalidException()
        }
        val userId = isMine(auction.sellerId, isAdmin)

        validateAuction(request)
        val start = requireNotNull(request.auctionStartAt).withSecond(0).withNano(0)
        val end = requireNotNull(request.auctionEndAt).withSecond(0).withNano(0)
        val startBid = requireNotNull(request.startBid)
        val productName = requireNotNull(request.productName)
        validateAuctionTime(start, end)

        auction.modify(startBid, start, end, userId, productName)
        auctionEventDispatcher.publishSearchUpdate(auction)
        return AuctionResponse.fromEntity(auction)
    }

    @Transactional
    fun removeAuction(auctionId: String, isAdmin: Boolean): AuctionResponse {
        var auction = auctionRepository.findById(auctionId).orElseThrow { AuctionNotFoundException() }
        val userId = isMine(auction.sellerId, isAdmin)

        if (!listOf(AuctionStatus.READY, AuctionStatus.CANCELLED, AuctionStatus.FAILED).contains(auction.status)) {
            throw AuctionStatusInvalidException()
        }

        auction.remove(userId)
        auctionSummaryCacheService.delete(auctionId)
        auction = auctionRepository.save(auction)

        return AuctionResponse.fromEntity(auction)
    }

    private fun validateAuction(request: AuctionRequest) {
        if (request.auctionStartAt == null || request.auctionEndAt == null) {
            throw CustomException("경매 시작/종료 시간은 반드시 입력해야 합니다.")
        }
        if (request.productId.isNullOrBlank() || request.productName.isNullOrBlank() || request.sellerId.isNullOrBlank()) {
            throw CustomException("상품/판매자 정보는 반드시 입력해야 합니다.")
        }
        if (request.auctionStartAt.isAfter(request.auctionEndAt)) {
            throw CustomException("경매 시작 시간은 종료 시간보다 이전이어야 합니다.")
        }
        if (request.startBid == null || request.startBid.toLong() <= 0) {
            throw CustomException("시작 입찰가는 0보다 큰 정수여야 합니다.")
        }
    }

    private fun validateAuctionTime(start: OffsetDateTime, end: OffsetDateTime) {
        val now = OffsetDateTime.now()

        if (!start.isAfter(now)) {
            throw CustomException("경매 시작 시간은 현재 시간 이후여야 합니다.")
        }
        if (!end.isAfter(start)) {
            throw CustomException("경매 종료 시간은 시작 시간 이후여야 합니다.")
        }

        val currentSecond = now.second
        if (currentSecond > 55) {
            val earliest = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
            if (start.isBefore(earliest)) {
                throw CustomException("${DateUtil.format(earliest, "HH:mm")} 이후에 다시 시도해주세요.")
            }
        }
    }

    fun getAuctionListByProductId(productId: String): List<AuctionResponse> {
        return auctionRepository.findByProductIdAndDeletedYn(productId).map(AuctionResponse::fromEntity)
    }

    private fun isMine(sellerId: String?, isAdmin: Boolean): String {
        val userId = UserContext.get().userId

        if (sellerId == null || isAdmin) {
            return userId
        }

        if (!StringUtils.hasText(userId) || userId != sellerId) {
            throw AuctionModifyForbiddenException()
        }

        return userId
    }
}
