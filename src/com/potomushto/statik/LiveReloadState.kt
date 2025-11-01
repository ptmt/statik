package com.potomushto.statik

import kotlinx.serialization.Serializable

/**
 * Manages the state for live reload functionality in watch mode.
 * Thread-safe tracking of when the site was last rebuilt.
 */
class LiveReloadState {
    @Volatile
    private var lastBuildTimestamp = System.currentTimeMillis()

    /**
     * Update the timestamp to the current time.
     * Called after each successful site rebuild.
     */
    fun markRebuilt() {
        lastBuildTimestamp = System.currentTimeMillis()
    }

    /**
     * Get the current timestamp for client polling.
     */
    fun getTimestamp(): Long = lastBuildTimestamp

    /**
     * Get a JSON response for the reload endpoint.
     */
    fun getReloadResponse(): ReloadResponse {
        return ReloadResponse(lastBuildTimestamp)
    }
}

/**
 * Response model for the live reload endpoint
 */
@Serializable
data class ReloadResponse(
    val timestamp: Long
)
