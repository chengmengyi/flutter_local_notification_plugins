package com.local.notification.flutter_local_notification_plugins

internal class NotificationRefreshSessionGate {
    private var token: String? = null
    private var nextRefreshIndex: Int = 1
    private var refreshCount: Int = 0

    @Synchronized
    fun start(
        newToken: String,
        newRefreshCount: Int,
    ) {
        require(newRefreshCount > 0)
        token = newToken
        nextRefreshIndex = 1
        refreshCount = newRefreshCount
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
        refreshCount = 0
        return true
    }

    @Synchronized
    fun cancel(): Boolean {
        val hadSession = token != null
        token = null
        nextRefreshIndex = 1
        refreshCount = 0
        return hadSession
    }
}
