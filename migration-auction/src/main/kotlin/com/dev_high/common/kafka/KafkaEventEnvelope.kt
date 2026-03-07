package com.dev_high.common.kafka

import java.util.UUID

data class KafkaEventEnvelope<T>(
    val eventId: UUID,
    val module: String,
    val eventType: String,
    val timestamp: Long,
    val payload: T,
) {
    companion object {
        fun <T : Any> wrap(module: String, payload: T): KafkaEventEnvelope<T> {
            return KafkaEventEnvelope(
                eventId = UUID.randomUUID(),
                module = module,
                eventType = payload::class.java.simpleName,
                timestamp = System.currentTimeMillis(),
                payload = payload,
            )
        }
    }
}
