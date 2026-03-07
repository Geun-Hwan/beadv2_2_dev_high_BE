package com.dev_high.exception

import org.springframework.http.HttpStatus

class CannotWithdrawHighestBidderException(
    errorCode: String? = null,
    message: String = "최고 입찰자는 경매를 포기할 수 없습니다.",
) : MigrationCustomException(HttpStatus.BAD_REQUEST, message, errorCode)
