package com.dev_high.common.util

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtil {
    private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private const val DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss"
    val DEFAULT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_PATTERN)

    fun now(): OffsetDateTime = OffsetDateTime.now(KST)

    fun parse(dateStr: String): OffsetDateTime {
        val ldt = LocalDateTime.parse(dateStr, DEFAULT_FORMATTER)
        return ldt.atZone(KST).toOffsetDateTime()
    }

    fun parse(dateStr: String, pattern: String): OffsetDateTime {
        val ldt = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern))
        return ldt.atZone(KST).toOffsetDateTime()
    }

    fun format(dateTime: OffsetDateTime): String = dateTime.format(DEFAULT_FORMATTER)

    fun format(dateTime: OffsetDateTime, pattern: String): String {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun nowStr(): String = format(now())

    fun nowStr(pattern: String): String = format(now(), pattern)
}
