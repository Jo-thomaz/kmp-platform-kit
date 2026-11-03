package dev.scaffold.kmp.domain.model

import dev.scaffold.kmp.core.UuidV7

data class Tag(
    val id: UuidV7,
    val name: String,
    val colorToken: String,
)
