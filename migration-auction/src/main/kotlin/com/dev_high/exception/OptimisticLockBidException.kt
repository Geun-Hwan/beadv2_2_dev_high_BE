package com.dev_high.exception

import org.springframework.http.HttpStatus

class OptimisticLockBidException(
    errorCode: String? = null,
    message: String = "동시 입찰로 인해 처리되지 않았습니다. 다시 시도해주세요.",
) : MigrationCustomException(HttpStatus.CONFLICT, message, errorCode)
