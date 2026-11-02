@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.scaffold.kmp.core

import kotlin.jvm.JvmInline
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
value class UuidV7 private constructor(val value: String) : Comparable<UuidV7> {
    val timestampMillis: Long
        get() {
            val uuid = Uuid.parse(value)
            return uuid.toLongs { mostSignificantBits, _ ->
                mostSignificantBits ushr TIMESTAMP_SHIFT_BITS
            }
        }

    fun toKotlinUuid(): Uuid = Uuid.parse(value)

    override fun compareTo(other: UuidV7): Int = value.compareTo(other.value)

    override fun toString(): String = value

    companion object {
        private const val TIMESTAMP_SHIFT_BITS = 16
        private const val VERSION_SHIFT_BITS = 12
        private const val NIBBLE_MASK = 0x0FL
        private const val UUID_VERSION_7 = 7L
        private const val VARIANT_SHIFT_BITS = 62
        private const val VARIANT_RFC9562 = 2L // binary 10xx

        /**
         * Generates a strictly monotonic RFC 9562 UUIDv7 using Kotlin stdlib [Uuid.generateV7],
         * or a deterministic timestamp-based UUIDv7 via [Uuid.generateV7NonMonotonicAt] when
         * [timestampMillis] is explicitly supplied.
         */
        fun generate(timestampMillis: Long? = null): UuidV7 {
            val uuid = if (timestampMillis != null) {
                require(timestampMillis >= 0L) { "timestampMillis must be non-negative" }
                Uuid.generateV7NonMonotonicAt(Instant.fromEpochMilliseconds(timestampMillis))
            } else {
                Uuid.generateV7()
            }
            return UuidV7(uuid.toString())
        }

        fun parse(raw: String): Outcome<UuidV7> {
            val normalized = raw.trim().lowercase()
            val parsed = try {
                Uuid.parse(normalized)
            } catch (_: IllegalArgumentException) {
                return invalidUuidOutcome(raw)
            }
            val isV7 = parsed.toLongs { msb, lsb ->
                val version = (msb ushr VERSION_SHIFT_BITS) and NIBBLE_MASK
                val variant = lsb ushr VARIANT_SHIFT_BITS
                version == UUID_VERSION_7 && variant == VARIANT_RFC9562
            }
            return if (isV7) {
                Outcome.success(UuidV7(parsed.toString()))
            } else {
                invalidUuidOutcome(raw)
            }
        }

        private fun invalidUuidOutcome(raw: String): Outcome<Nothing> =
            Outcome.failure(
                DomainError.Validation(
                    field = "uuid",
                    message = "Invalid UUIDv7 format: '$raw'",
                ),
            )
    }
}
