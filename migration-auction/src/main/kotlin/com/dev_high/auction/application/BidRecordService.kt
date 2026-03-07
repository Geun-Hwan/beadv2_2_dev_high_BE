package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionBidMessage
import com.dev_high.auction.application.dto.AuctionParticipationResponse
import com.dev_high.auction.domain.AuctionBidHistory
import com.dev_high.auction.domain.AuctionLiveState
import com.dev_high.auction.domain.AuctionParticipation
import com.dev_high.auction.domain.BidType
import com.dev_high.auction.domain.idclass.AuctionParticipationId
import com.dev_high.auction.infrastructure.bid.AuctionBidHistoryJpaRepository
import com.dev_high.auction.infrastructure.bid.AuctionParticipationJpaRepository
import com.dev_high.common.context.UserContext
import com.dev_high.common.dto.ApiResponseDto
import com.dev_high.common.exception.CustomException
import com.dev_high.common.util.HttpUtil
import com.dev_high.exception.AlreadyWithdrawnException
import com.dev_high.exception.AuctionParticipationNotFoundException
import com.dev_high.exception.CannotWithdrawHighestBidderException
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal

@Service
class BidRecordService(
    private val auctionBidHistoryJpaRepository: AuctionBidHistoryJpaRepository,
    private val auctionParticipationJpaRepository: AuctionParticipationJpaRepository,
    private val restTemplate: RestTemplate,
    private val entityManager: EntityManager,
    private val eventDispatcher: AuctionEventDispatcher,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordHistory(history: AuctionBidHistory): AuctionBidHistory {
        return auctionBidHistoryJpaRepository.save(history)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveParticipation(participation: AuctionParticipation) {
        auctionParticipationJpaRepository.save(participation)
    }

    fun hasParticipated(userId: String?, auctionId: String?): Boolean {
        if (userId == null || auctionId == null) return false
        val participationId = AuctionParticipationId(userId, auctionId)
        return auctionParticipationJpaRepository.existsById(participationId)
    }

    fun findParticipation(auctionId: String): AuctionParticipationResponse {
        val userId = UserContext.get().userId
        val isExist = hasParticipated(userId, auctionId)
        if (!isExist) {
            return AuctionParticipationResponse.isNotParticipated(auctionId)
        }

        val participationId = AuctionParticipationId(userId, auctionId)
        val participation = auctionParticipationJpaRepository.findById(participationId)
            .orElseThrow { AuctionParticipationNotFoundException() }

        return AuctionParticipationResponse.isParticipated(participation)
    }

    @Transactional
    fun withdrawAuctionBid(auctionId: String, userId: String): AuctionParticipationResponse {
        val participation = auctionParticipationJpaRepository.findById(AuctionParticipationId(userId, auctionId))
            .orElseThrow { AuctionParticipationNotFoundException() }

        if (participation.withdrawnYn == "Y") {
            throw AlreadyWithdrawnException()
        }

        val liveState: AuctionLiveState = participation.auction?.liveState
            ?: throw AuctionParticipationNotFoundException()

        if (userId == liveState.highestUserId) {
            throw CannotWithdrawHighestBidderException()
        }

        participation.markWithdraw()
        recordHistory(AuctionBidHistory(auctionId, BigDecimal.ZERO, userId, BidType.BID_WITHDRAW))

        if (depositUpdate(userId, auctionId, participation.depositAmount ?: BigDecimal.ZERO, "REFUND") != null) {
            processRefundComplete(participation)
        }

        auctionParticipationJpaRepository.save(participation)
        return AuctionParticipationResponse.isParticipated(participation)
    }

    private fun depositUpdate(userId: String, auctionId: String, amount: BigDecimal, type: String): ApiResponseDto<*>? {
        return try {
            val map = hashMapOf<String, Any>(
                "userId" to userId,
                "type" to type,
                "depositOrderId" to auctionId,
                "amount" to amount,
            )

            val entity: HttpEntity<Map<String, Any>> = HttpUtil.createGatewayEntity(map)
            val url = "http://USER-SERVICE/api/v1/deposit/usages"

            val response: ResponseEntity<ApiResponseDto<*>> = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<ApiResponseDto<*>>() {},
            )

            response.body?.also {
                log.info("deposit response >>>{}", it.data.toString())
            }
        } catch (e: Exception) {
            log.error("예치금 업데이트 실패: {}", e.message, e)
            null
        }
    }

    fun markDepositRefunded(auctionId: String, userIds: List<String>): Long {
        val participation = auctionParticipationJpaRepository.findByAuctionIdAndUserIdIn(auctionId, userIds)
        var count = 0L
        for (part in participation) {
            if (part.depositRefundedYn == "N") {
                processRefundComplete(part)
                count++
            }
        }
        return count
    }

    private fun processRefundComplete(participation: AuctionParticipation) {
        participation.markDepositRefunded()

        recordHistory(
            AuctionBidHistory(
                participation.auctionId ?: "",
                participation.depositAmount ?: BigDecimal.ZERO,
                participation.userId ?: "",
                BidType.REFUND_COMPLETE,
            ),
        )
    }

    fun getAllMyParticipation(pageable: Pageable): Page<AuctionParticipationResponse> {
        val userId = UserContext.get().userId
        return auctionParticipationJpaRepository.findByUserId(userId, pageable)
            .map(AuctionParticipationResponse::isParticipated)
    }

    fun createParticipation(auctionId: String, decimal: BigDecimal, userId: String): AuctionParticipationResponse? {
        if (hasParticipated(userId, auctionId)) {
            log.info("이미 처리되었습니다. : {}", auctionId)
            return null
        }

        val id = AuctionParticipationId(userId, auctionId)

        if (depositUpdate(userId, auctionId, decimal, "DEPOSIT") == null) {
            throw CustomException("보증금 납부가 실패하였습니다.")
        }

        return try {
            val participation = auctionParticipationJpaRepository.save(AuctionParticipation(id, decimal))
            AuctionParticipationResponse.isParticipated(participation)
        } catch (e: Exception) {
            eventDispatcher.publishDepositRefundRequest(listOf(userId), auctionId, decimal)
            throw e
        }
    }

    fun getBidHistory(auctionId: String, pageable: Pageable): Page<AuctionBidMessage> {
        val page = auctionBidHistoryJpaRepository.findByAuctionIdAndType(auctionId, BidType.BID_SUCCESS, pageable)
        val dtoList = page.content.map(AuctionBidMessage::fromEntity)
        return PageImpl(dtoList, pageable, page.totalElements)
    }

    companion object {
        private val log = LoggerFactory.getLogger(BidRecordService::class.java)
    }
}
