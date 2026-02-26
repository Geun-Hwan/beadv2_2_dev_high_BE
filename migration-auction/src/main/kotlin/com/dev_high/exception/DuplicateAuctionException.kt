package com.dev_high.exception

import org.springframework.http.HttpStatus

class DuplicateAuctionException(
    errorCode: String? = null,
    message: String = "이미 진행 중이거나 종료된 경매가 존재합니다. 새 경매를 등록할 수 없습니다.",
) : MigrationCustomException(HttpStatus.BAD_REQUEST, message, errorCode)
