package com.dev_high.auction.presentation

import com.dev_high.auction.application.AuctionRankingService
import com.dev_high.auction.application.AuctionRecommendationService
import com.dev_high.auction.application.AuctionService
import com.dev_high.auction.application.dto.AuctionRankingResponse
import com.dev_high.auction.application.dto.AuctionRecommendationResponse
import com.dev_high.auction.application.dto.AuctionResponse
import com.dev_high.auction.presentation.dto.AuctionRequest
import com.dev_high.auction.presentation.dto.UserAuctionListRequest
import com.dev_high.common.dto.ApiResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auctions")
@Tag(name = "Auction", description = "경매 관리 API")
class AuctionController(
    private val auctionService: AuctionService,
    private val auctionRankingService: AuctionRankingService,
    private val auctionRecommendationService: AuctionRecommendationService,
) {
    @Operation(summary = "경매 목록 조회", description = "페이지네이션과 필터를 통해 경매 목록을 조회합니다.")
    @GetMapping
    fun getAuctionList(@ModelAttribute request: UserAuctionListRequest, pageable: Pageable): ApiResponseDto<Page<AuctionResponse>> {
        val res = auctionService.getUserAuctionList(request, pageable)
        return ApiResponseDto.success(res)
    }

    @Operation(summary = "상품과 연관된 경매목록 조회", description = "상품과 연관된 경매 목록을 조회합니다.")
    @GetMapping("by-product")
    fun getAuctionListByProductId(@RequestParam productId: String): ApiResponseDto<Any> {
        return ApiResponseDto.success(auctionService.getAuctionListByProductId(productId))
    }

    @Operation(summary = "경매 단일 조회", description = "경매 ID로 정보를 조회합니다.")
    @GetMapping("{auctionId}")
    fun getAuction(@PathVariable auctionId: String): ApiResponseDto<AuctionResponse> {
        return ApiResponseDto.success(auctionService.getAuction(auctionId))
    }

    @Operation(summary = "경매 리스트 조회", description = "경매 ID list로 정보를 조회합니다.")
    @GetMapping("by-ids")
    fun getAuctionForIds(@RequestParam auctionIds: List<String>): ApiResponseDto<List<AuctionResponse>> {
        return ApiResponseDto.success(auctionService.getAuctions(auctionIds))
    }

    @Operation(summary = "경매 생성", description = "새로운 경매를 생성합니다.")
    @PostMapping
    fun createAuction(@RequestBody request: AuctionRequest): ApiResponseDto<AuctionResponse> {
        val res = auctionService.createAuction(request, false)
        return ApiResponseDto.of("CREATED", "성공적으로 저장하였습니다.", res)
    }

    @Operation(summary = "경매 수정", description = "기존 경매 정보를 수정합니다.")
    @PutMapping("{auctionId}")
    fun modifyAuction(@PathVariable auctionId: String, @RequestBody request: AuctionRequest): ApiResponseDto<AuctionResponse> {
        val res = auctionService.modifyAuction(auctionId, request, false)
        return ApiResponseDto.success(res)
    }

    @Operation(summary = "경매 삭제", description = "경매 ID로 경매를 삭제합니다.")
    @DeleteMapping("{auctionId}")
    fun removeAuction(@PathVariable auctionId: String): ApiResponseDto<AuctionResponse> {
        return ApiResponseDto.success(auctionService.removeAuction(auctionId, false))
    }

    @Operation(summary = "오늘의 인기 경매 TOP 조회", description = "입찰 횟수와 입장 조회수를 기반으로 TOP 경매를 조회합니다.")
    @GetMapping("top/today")
    fun getTodayTopAuctions(@RequestParam(defaultValue = "10") limit: Int): ApiResponseDto<List<AuctionRankingResponse>> {
        return ApiResponseDto.success(auctionRankingService.getTodayTop(limit))
    }

    @Operation(summary = "경매 시작가 추천", description = "유사 상품 낙찰 데이터를 기반으로 추천값을 제공합니다.")
    @GetMapping("recommendation")
    fun getAuctionRecommendation(@RequestParam productId: String): ApiResponseDto<AuctionRecommendationResponse> {
        return ApiResponseDto.success(auctionRecommendationService.recommend(productId))
    }
}
