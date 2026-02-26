package com.dev_high.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class AuctionHandshakeInterceptor : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        if (request is ServletServerHttpRequest) {
            val http = request.servletRequest
            val ip = extractClientIp(http)
            val userAgent = http.getHeader("User-Agent")
            if (ip != null) attributes[ATTR_CLIENT_IP] = ip
            if (userAgent != null) attributes[ATTR_USER_AGENT] = userAgent
        }
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) = Unit

    private fun extractClientIp(request: HttpServletRequest): String? {
        for (header in IP_HEADERS) {
            val value = request.getHeader(header)
            if (!value.isNullOrBlank()) {
                return if (value.contains(",")) value.split(",")[0].trim() else value.trim()
            }
        }
        return request.remoteAddr
    }

    companion object {
        private const val ATTR_CLIENT_IP = "clientIp"
        private const val ATTR_USER_AGENT = "userAgent"
        private val IP_HEADERS = listOf("X-Forwarded-For", "X-Real-IP")
    }
}
