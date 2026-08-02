package com.haoshield.domain.model

data class BlockedAppGroup(
    val id: String,
    val displayName: String,
    val packageNames: List<String>,
    val isPreset: Boolean = true,
)