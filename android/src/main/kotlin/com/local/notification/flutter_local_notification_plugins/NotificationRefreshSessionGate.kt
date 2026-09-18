package com.local.notification.flutter_local_notification_plugins

internal class NotificationRefreshSessionGate(
    private val refreshCount: Int,
) {
    private var token: String? = null
    private var nextRefreshIndex: Int = 1

    @Synchronized
    fun start(newToken: String) {
        token = newToken
        nextRefreshIndex = 1
    }

    @Synchronized
    fun isCurrent(candidateToken: String): Boolean = token == candidateToken

    @Synchronized
    fun claim(
        candidateToken: String,
        refreshIndex: Int,
    ): Boolean {
        if (token != candidateToken || refreshIndex != nextRefreshIndex) {
            return false
        }
        if (refreshIndex !in 1..refreshCount) {
            return false
        }
        nextRefreshIndex++
        return true
    }

    @Synchronized
    fun finish(candidateToken: String): Boolean {
        if (token != candidateToken) return false
        token = null
        nextRefreshIndex = 1
        return true
    }

    @Synchronized
    fun cancel(): Boolean {
        val hadSession = token != null
        token = null
        nextRefreshIndex = 1
        return hadSession
    }
}
