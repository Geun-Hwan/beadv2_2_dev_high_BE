package com.dev_high.auction.domain

enum class AuctionStatus {
    READY,       // 경매 대기 (시작 전)
    IN_PROGRESS, // 경매 진행
    COMPLETED,   // 경매 종료 (낙찰자 확정)
    FAILED,      // 경매 유찰 (입찰자 없음)
    CANCELLED,   // 경매 포기 (낙찰자 취소/결제 취소)
}
