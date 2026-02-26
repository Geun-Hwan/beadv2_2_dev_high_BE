package com.dev_high.auction.presentation

import com.dev_high.auction.application.AuctionBidBanService
import com.dev_high.auction.application.BidRecordService
import com.dev_high.auction.application.BidService
import com.dev_high.auction.presentation.dto.AuctionBidRequest
import com.dev_high.auction.presentation.dto.RefundCompleteRequest
import com.dev_high.common.context.UserContext
import com.dev_high.common.dto.ApiResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auctions")
@Tag(name = "AuctionBid", description = "경매 입찰 관련 API")
class AuctionBidController(
    private val bidService: BidService,
    private val bidRecordService: BidRecordService,
    private val auctionBidBanService: AuctionBidBanService,
) {
    @Operation(summary = "경매 참여 현황 조회", description = "본인의 경매 참여 현황을 전체 조회합니다.")
    @GetMapping("participation/me")
    fun getParticipationList(pageable: Pageable): ApiResponseDto<Any> {
        return ApiResponseDto.success(bidRecordService.getAllMyParticipation(pageable))
    }

    @Operation(summary = "특정 경매 참여 현황 조회", description = "auctionId에 해당하는 경매에서 본인의 참여 현황을 조회합니다.")
    @GetMapping("{auctionId}/participation")
    fun getParticipationForAuction(@Parameter(description = "조회할 경매 ID", required = true) @PathVariable auctionId: String): ApiResponseDto<Any> {
        return ApiResponseDto.success(bidRecordService.findParticipation(auctionId))
    }

    @Operation(summary = "보증금 납부 확인", description = "보증금 결제 후 참여이력을 최초 등록합니다.")
    @PostMapping("{auctionId}/participation")
    fun createParticipation(
        @Parameter(description = "경매 ID", required = true) @PathVariable auctionId: String,
        @RequestBody request: AuctionBidRequest,
    ): ApiResponseDto<Any?> {
        val result = bidRecordService.createParticipation(auctionId, request.toDepositCommand(), UserContext.get().userId)
        return ApiResponseDto(
            "CREATED",
            "성공적으로 저장하였습니다.",
            result,
        )
    }

    @Operation(summary = "경매 입찰 등록/수정", description = "경매에 입찰을 등록하거나 기존 입찰 정보를 수정합니다.")
    @PostMapping("{auctionId}/bids")
    fun upsertAuctionBid(
        @Parameter(description = "경매 ID", required = true) @PathVariable auctionId: String,
        @RequestBody request: AuctionBidRequest,
    ): ApiResponseDto<Any> {
        return ApiResponseDto.of(
            "CREATED",
            "성공적으로 저장하였습니다.",
            bidService.createOrUpdateAuctionBid(auctionId, request.toBidCommand()),
        )
    }

    @Operation(summary = "경매 입찰 제한 상태 조회", description = "부정 입찰 의심으로 인한 제한 상태를 조회합니다.")
    @GetMapping("{auctionId}/bid-ban")
    fun getBidBanStatus(@Parameter(description = "경매 ID", required = true) @PathVariable auctionId: String): ApiResponseDto<Any> {
        val userId = UserContext.get().userId
        return ApiResponseDto.success(auctionBidBanService.getStatus(auctionId, userId))
    }

    @Operation(summary = "경매 입찰 포기", description = "auctionId에 해당하는 경매에서 본인의 입찰을 포기합니다. 보증금은 즉시 환급")
    @PutMapping("{auctionId}/withdraw")
    fun withdrawAuctionBid(@Parameter(description = "포기할 경매 ID", required = true) @PathVariable auctionId: String): ApiResponseDto<Any> {
        return ApiResponseDto.success(
            "입찰을 포기하고 환불 요청이 완료되었습니다.",
            bidRecordService.withdrawAuctionBid(auctionId, UserContext.get().userId),
        )
    }

    @Deprecated("legacy")
    @Operation(summary = "보증금 환불완료 처리(여러명)", description = "예치금 서비스에서 보증금 환급 완료후 호출하여 상태를 업데이트합니다.")
    @PutMapping("{auctionId}/refund-complete")
    fun markRefundComplete(
        @Parameter(description = "환불 완료 처리할 경매 ID", required = true) @PathVariable auctionId: String,
        @RequestBody request: RefundCompleteRequest,
    ): ApiResponseDto<Any> {
        return ApiResponseDto.success("환불 완료 처리되었습니다.", bidRecordService.markDepositRefunded(auctionId, request.userIds))
    }

    @GetMapping("{auctionId}/bids/history")
    fun getBidHistory(
        @PathVariable auctionId: String,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): ApiResponseDto<Any> {
        return ApiResponseDto.success(bidRecordService.getBidHistory(auctionId, pageable))
    }
}
