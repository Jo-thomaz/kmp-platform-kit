package dev.scaffold.kmp.core

sealed interface DomainError {
    val message: String

    data class Network(
        override val message: String,
        val statusCode: Int? = null,
    ) : DomainError

    data class NotFound(
        val resource: String,
        val id: String,
        override val message: String = "$resource '$id' was not found",
    ) : DomainError

    data class Validation(
        val field: String,
        override val message: String,
    ) : DomainError

    data class Conflict(
        override val message: String,
    ) : DomainError

    data class Unauthorized(
        override val message: String = "Unauthorized",
    ) : DomainError

    data class Unknown(
        override val message: String,
        val causeMessage: String? = null,
    ) : DomainError
}
