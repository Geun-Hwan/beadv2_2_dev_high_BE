package com.dev_high.exception

import org.springframework.http.HttpStatus

open class MigrationCustomException(
    val httpStatus: HttpStatus,
    override val message: String,
    val errorCode: String? = null,
) : RuntimeException(message)
