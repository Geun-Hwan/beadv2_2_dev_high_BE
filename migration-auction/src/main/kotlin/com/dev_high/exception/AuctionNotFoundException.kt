package com.dev_high.exception

import org.springframework.http.HttpStatus

class AuctionNotFoundException(
    errorCode: String? = null,
    message: String = "존재하지 않는 경매입니다.",
) : MigrationCustomException(HttpStatus.NOT_FOUND, message, errorCode)
