package com.dev_high.common.context

object UserContext {
    data class UserInfo(
        val userId: String,
        val token: String,
    )

    private val context = ThreadLocal<UserInfo>()

    fun set(userInfo: UserInfo) {
        context.set(userInfo)
    }

    fun get(): UserInfo = context.get() ?: UserInfo("SYSTEM", "")

    fun clear() {
        context.remove()
    }
}
