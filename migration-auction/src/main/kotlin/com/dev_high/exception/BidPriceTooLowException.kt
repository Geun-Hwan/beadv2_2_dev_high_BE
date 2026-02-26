package com.dev_high.exception

import org.springframework.http.HttpStatus

/**
 * 입찰가가 현재 경매 가격보다 낮을 경우 발생
 *
 * 임시 마이그레이션 샘플: 기존 Java 클래스와 동일 동작을 Kotlin으로 표현.
 */
class BidPriceTooLowException(
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    val errorCode: String? = null,
    message: String = "현재 최고 입찰가보다 높은 금액으로 다시 입찰해 주세요.",
) : RuntimeException(message) {

    constructor(errorCode: String, message: String) : this(HttpStatus.BAD_REQUEST, errorCode, message)
}
