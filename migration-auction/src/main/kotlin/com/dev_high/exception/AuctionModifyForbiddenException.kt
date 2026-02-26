package com.dev_high.exception

import org.springframework.http.HttpStatus

class AuctionModifyForbiddenException(
    message: String = "수정 권한이 없습니다.",
) : MigrationCustomException(HttpStatus.FORBIDDEN, message)
