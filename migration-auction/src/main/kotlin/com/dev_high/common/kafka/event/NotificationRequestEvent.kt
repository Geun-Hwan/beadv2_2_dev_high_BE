package com.dev_high.common.kafka.event

import com.dev_high.common.type.NotificationCategory

data class NotificationRequestEvent(
    val userIds: List<String>,
    val content: String,
    val targetUrl: String,
    val type: NotificationCategory.Type,
)
