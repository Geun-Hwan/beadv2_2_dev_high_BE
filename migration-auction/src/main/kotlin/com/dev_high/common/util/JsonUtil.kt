package com.dev_high.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

object JsonUtil {
    private val mapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun toJson(obj: Any): String {
        return try {
            mapper.writeValueAsString(obj)
        } catch (e: Exception) {
            throw RuntimeException("JSON 변환 실패", e)
        }
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return try {
            mapper.readValue(json, clazz)
        } catch (e: Exception) {
            throw RuntimeException("JSON 파싱 실패", e)
        }
    }

    fun <T> fromPayload(payload: Any?, clazz: Class<T>): T {
        return try {
            val json = mapper.writeValueAsString(payload)
            mapper.readValue(json, clazz)
        } catch (e: Exception) {
            throw RuntimeException("Payload 변환 실패: ${clazz.simpleName}", e)
        }
    }
}
