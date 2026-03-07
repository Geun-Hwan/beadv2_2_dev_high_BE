package com.dev_high.exception

import org.springframework.http.HttpStatus

class AuctionTimeOutOfRangeException(
    errorCode: String? = null,
    message: String = "현재 경매 시간 범위를 벗어난 입찰입니다.",
) : MigrationCustomException(HttpStatus.BAD_REQUEST, message, errorCode)
