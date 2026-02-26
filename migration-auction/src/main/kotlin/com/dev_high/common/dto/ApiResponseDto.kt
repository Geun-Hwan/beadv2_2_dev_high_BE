package com.dev_high.common.dto

data class ApiResponseDto<T>(
    var code: String = "SUCCESS",
    var message: String = "정상적으로 처리되었습니다.",
    var data: T? = null,
) {
    companion object {
        fun <T> success(data: T): ApiResponseDto<T> = ApiResponseDto(data = data)
        fun <T> success(message: String, data: T): ApiResponseDto<T> = ApiResponseDto(message = message, data = data)
        fun <T> of(code: String, message: String, data: T): ApiResponseDto<T> = ApiResponseDto(code, message, data)
        fun <T> fail(message: String): ApiResponseDto<T> = ApiResponseDto("FAIL", message, null)
        fun <T> fail(message: String, errorCode: String): ApiResponseDto<T> = ApiResponseDto(errorCode, message, null)
        fun <T> error(message: String): ApiResponseDto<T> = ApiResponseDto("ERR001", message, null)
    }
}
