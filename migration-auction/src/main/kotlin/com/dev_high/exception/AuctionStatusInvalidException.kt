package com.dev_high.exception

import org.springframework.http.HttpStatus

class AuctionStatusInvalidException :
    MigrationCustomException(HttpStatus.BAD_REQUEST, "진행중인 경매는 수정할 수 없습니다. 새 경매를 생성하세요.")
