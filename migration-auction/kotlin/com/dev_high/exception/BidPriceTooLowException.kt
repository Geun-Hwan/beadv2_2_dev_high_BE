package com.dev_high.exception

import com.dev_high.common.exception.CustomException
import org.springframework.http.HttpStatus

/**
 * 입찰가가 현재 경매 가격보다 낮을 경우 발생
 *
 * 임시 마이그레이션 샘플: 기존 Java 클래스와 동일 동작을 Kotlin으로 표현.
 */
class BidPriceTooLowException : CustomException {

    constructor() : super(
        HttpStatus.BAD_REQUEST,
        "현재 최고 입찰가보다 높은 금액으로 다시 입찰해 주세요."
    )

    constructor(errorCode: String, message: String) : super(
        HttpStatus.BAD_REQUEST,
        message,
        errorCode
    )
}
