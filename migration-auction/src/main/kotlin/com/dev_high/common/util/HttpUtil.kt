package com.dev_high.common.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object HttpUtil {
    fun <T> createGatewayEntity(body: T): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val token = getAuthorizationToken()
        if (token != null) {
            headers.setBearerAuth(token)
        }

        return HttpEntity(body, headers)
    }

    @Deprecated("서비스 간 직접 호출")
    fun <T> createDirectEntity(body: T): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("X-Role", "ADMIN")
        return HttpEntity(body, headers)
    }

    private fun getAuthorizationToken(): String? {
        val attrs = RequestContextHolder.getRequestAttributes()
        if (attrs is ServletRequestAttributes) {
            val request: HttpServletRequest = attrs.request
            return request.getHeader("Authorization")
        }
        return null
    }
}
