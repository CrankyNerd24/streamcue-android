package com.streamcueapp.streamcue.model

import kotlinx.serialization.Serializable

/** A service the user has picked on the Services tab — mirrors iOS's
 * Subscriptions.Service, stored as JSON instead of "id|name|logo" lines. */
@Serializable
data class StreamingService(
    val id: Int,
    val name: String,
    val logoPath: String? = null,
)
