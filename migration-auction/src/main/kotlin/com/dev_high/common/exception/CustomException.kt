package com.dev_high.common.exception

import org.springframework.http.HttpStatus

open class CustomException : RuntimeException {
    val status: HttpStatus
    val errorCode: String?

    constructor(message: String) : super(message) {
        status = HttpStatus.BAD_REQUEST
        errorCode = "null"
    }

    constructor(errorCode: String, message: String) : super(message) {
        status = HttpStatus.BAD_REQUEST
        this.errorCode = errorCode
    }

    constructor(status: HttpStatus, message: String) : super(message) {
        this.status = status
        errorCode = null
    }

    constructor(status: HttpStatus, message: String, errorCode: String) : super(message) {
        this.status = status
        this.errorCode = errorCode
    }
}
