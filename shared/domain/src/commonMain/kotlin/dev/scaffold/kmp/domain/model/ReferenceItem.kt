package dev.scaffold.kmp.domain.model

import dev.scaffold.kmp.core.UuidV7

data class ReferenceItem(
    val id: UuidV7,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
)
