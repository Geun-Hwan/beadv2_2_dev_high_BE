package com.dev_high.exception

import org.springframework.http.HttpStatus

class AlreadyWithdrawnException(
    errorCode: String? = null,
    message: String = "이미 경매를 포기한 사용자입니다.",
) : MigrationCustomException(HttpStatus.BAD_REQUEST, message, errorCode)
