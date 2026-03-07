package com.dev_high.auction.application

import com.dev_high.auction.application.dto.AuctionBidMessage
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@Service
class AuctionWebSocketService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val auctionRankingService: AuctionRankingService,
) {
    private val auctionRooms: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()

    fun broadcastBidSuccess(message: AuctionBidMessage) {
        val updated = message.withCurrentUsers(getCurrentUserCount(message.auctionId ?: ""))
        messagingTemplate.convertAndSend("/topic/auction.${message.auctionId}", updated)
    }

    fun joinAuction(auctionId: String, sessionId: String, viewDedupKey: String) {
        val users = auctionRooms.computeIfAbsent(auctionId) { ConcurrentHashMap.newKeySet() }
        users.add(sessionId)
        auctionRankingService.incrementViewCount(auctionId, viewDedupKey)

        if (users.isNotEmpty()) {
            log.info("current user count: {}", users.size)
            val payload = mapOf("type" to "USER_JOIN", "currentUsers" to users.size)
            messagingTemplate.convertAndSend("/topic/auction.$auctionId", payload)
        }
    }

    fun leaveAuction(auctionId: String, sessionId: String) {
        val users = auctionRooms[auctionId]
        if (users != null) {
            users.remove(sessionId)
            log.info("current users count: {}", users.size)
            if (users.isEmpty()) {
                auctionRooms.remove(auctionId)
            }
        }

        if (users != null && users.isNotEmpty()) {
            val payload = mapOf("type" to "USER_LEAVE", "currentUsers" to users.size)
            messagingTemplate.convertAndSend("/topic/auction.$auctionId", payload)
        }
    }

    @EventListener
    fun handleDisconnect(event: SessionDisconnectEvent) {
        val sessionId = event.sessionId
        log.info("disconnect sessionId: {}", sessionId)

        auctionRooms.forEach { (auctionId, users) ->
            if (users.remove(sessionId)) {
                log.info("cur user count: {}", users.size)

                if (users.isEmpty()) {
                    auctionRooms.remove(auctionId)
                }

                if (users.isNotEmpty()) {
                    val payload = mapOf("type" to "USER_LEAVE", "currentUsers" to users.size)
                    messagingTemplate.convertAndSend("/topic/auction.$auctionId", payload)
                }
            }
        }
    }

    fun getCurrentUserCount(auctionId: String): Int {
        return auctionRooms.getOrDefault(auctionId, Collections.emptySet()).size
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuctionWebSocketService::class.java)
    }
}
