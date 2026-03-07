package com.dev_high.exception

import org.springframework.http.HttpStatus

class AuctionParticipationNotFoundException :
    MigrationCustomException(HttpStatus.NOT_FOUND, "참여 기록을 찾을 수 없습니다.")
