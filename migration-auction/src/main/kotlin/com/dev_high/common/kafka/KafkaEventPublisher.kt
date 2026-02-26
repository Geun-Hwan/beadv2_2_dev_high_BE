package com.dev_high.common.kafka

import org.springframework.stereotype.Component

@Component
class KafkaEventPublisher {
    fun publish(topic: String, payload: Any) {
        // migration-auction standalone: no-op publisher for compile/runtime safety.
    }
}
