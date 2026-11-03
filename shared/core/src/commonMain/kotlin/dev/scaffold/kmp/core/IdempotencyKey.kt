package dev.scaffold.kmp.core

import kotlin.jvm.JvmInline

@JvmInline
value class IdempotencyKey private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 128

        fun generate(timestampMillis: Long? = null): IdempotencyKey {
            val uuid = UuidV7.generate(timestampMillis = timestampMillis)
            return IdempotencyKey(uuid.value)
        }

        fun of(raw: String): Outcome<IdempotencyKey> {
            val trimmed = raw.trim()
            return when {
                trimmed.length < MIN_LENGTH -> Outcome.failure(
                    DomainError.Validation(
                        field = "idempotencyKey",
                        message = "IdempotencyKey must be at least $MIN_LENGTH characters",
                    ),
                )
                trimmed.length > MAX_LENGTH -> Outcome.failure(
                    DomainError.Validation(
                        field = "idempotencyKey",
                        message = "IdempotencyKey must be at most $MAX_LENGTH characters",
                    ),
                )
                else -> Outcome.success(IdempotencyKey(trimmed))
            }
        }
    }
}
