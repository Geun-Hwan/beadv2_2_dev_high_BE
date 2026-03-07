package com.dev_high.auction.presentation

import com.dev_high.auction.application.AuctionWebSocketService
import com.dev_high.common.context.UserContext
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Controller

@Controller
class AuctionWebSocketController(
    private val auctionWebSocketService: AuctionWebSocketService,
) {
    @MessageMapping("/join/{auctionId}")
    fun joinAuction(@DestinationVariable auctionId: String, accessor: StompHeaderAccessor) {
        val sessionId = accessor.sessionId ?: return
        val dedupKey = buildViewDedupKey(accessor, sessionId)
        auctionWebSocketService.joinAuction(auctionId, sessionId, dedupKey)
    }

    @MessageMapping("/leave/{auctionId}")
    fun leaveAuction(@DestinationVariable auctionId: String, accessor: StompHeaderAccessor) {
        val sessionId = accessor.sessionId ?: return
        auctionWebSocketService.leaveAuction(auctionId, sessionId)
    }

    private fun buildViewDedupKey(accessor: StompHeaderAccessor, sessionId: String): String {
        val userId = UserContext.get().userId
        if (userId.isNotBlank()) return userId

        val ip = extractSessionAttribute(accessor, "clientIp")
        val userAgent = extractSessionAttribute(accessor, "userAgent")

        if (ip == null && userAgent == null) return sessionId
        return "${ip ?: ""}|${userAgent ?: ""}"
    }

    private fun extractSessionAttribute(accessor: StompHeaderAccessor, name: String): String? {
        val attrs = accessor.sessionAttributes ?: return null
        val value = attrs[name] ?: return null
        val text = value.toString().trim()
        return text.ifBlank { null }
    }
}
