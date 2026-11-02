package dev.scaffold.kmp.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTypesTest {
    @Test
    fun outcomeSuccessMapsAndFoldsValue() {
        val initial: Outcome<Int> = Outcome.success(21)
        val mapped = initial.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(42, mapped.getOrNull())
        assertNull(mapped.errorOrNull())
        assertEquals("ok:42", mapped.fold(onSuccess = { "ok:$it" }, onFailure = { "err:${it.message}" }))
    }

    @Test
    fun outcomeFailurePreservesDomainErrorAcrossMapAndFlatMap() {
        val domainError = DomainError.NotFound(resource = "Item", id = "123")
        val initial: Outcome<Int> = Outcome.failure(domainError)
        val chained = initial
            .map { it + 1 }
            .flatMap { Outcome.success(it.toString()) }

        assertTrue(chained.isFailure)
        assertNull(chained.getOrNull())
        assertEquals(domainError, chained.errorOrNull())
        assertEquals("Item '123' was not found", chained.errorOrNull()?.message)
    }

    @Test
    fun domainErrorSubtypesExposeStructuredFields() {
        val network = DomainError.Network(message = "Timeout", statusCode = 504)
        val validation = DomainError.Validation(field = "title", message = "Title must not be blank")
        val conflict = DomainError.Conflict(message = "Duplicate idempotency key")
        val unauthorized = DomainError.Unauthorized()
        val unknown = DomainError.Unknown(message = "Unexpected", causeMessage = "IOException")

        assertEquals(504, network.statusCode)
        assertEquals("title", validation.field)
        assertEquals("Duplicate idempotency key", conflict.message)
        assertEquals("Unauthorized", unauthorized.message)
        assertEquals("IOException", unknown.causeMessage)
    }

    @Test
    fun uuidV7GeneratesOrderedValidIdsAndRoundTripsTimestamp() {
        val t1 = 1_762_000_100_000L
        val t2 = 1_762_000_200_000L
        val id1 = UuidV7.generate(timestampMillis = t1)
        val id2 = UuidV7.generate(timestampMillis = t2)

        assertEquals(t1, id1.timestampMillis)
        assertEquals(t2, id2.timestampMillis)
        assertTrue(id1 < id2, "Later UUIDv7 timestamp should sort after earlier timestamp")

        val parsed = UuidV7.parse(id1.value.uppercase())
        assertEquals(id1, parsed.getOrNull())
    }

    @Test
    fun uuidV7MonotonicAndUniqueAcrossConcurrentCoroutines() = runTest {
        val sequential = List(256) { UuidV7.generate() }
        assertEquals(sequential.sorted(), sequential, "Sequential Uuid.generateV7() calls must be strictly monotonic")

        val concurrentCount = 64
        val perCoroutine = 16
        val generated = (0 until concurrentCount).map {
            async(Dispatchers.Default) {
                List(perCoroutine) { UuidV7.generate() }
            }
        }.awaitAll().flatten()

        assertEquals(concurrentCount * perCoroutine, generated.toSet().size)
    }

    @Test
    fun uuidV7RejectsInvalidOrNonV7Strings() {
        val uuidV4 = "550e8400-e29b-41d4-a716-446655440000"
        val outcome = UuidV7.parse(uuidV4)

        assertTrue(outcome.isFailure)
        val err = assertIs<DomainError.Validation>(outcome.errorOrNull())
        assertEquals("uuid", err.field)
    }

    @Test
    fun idempotencyKeyValidatesLengthAndGeneratesUuidV7BackedKey() {
        val generated = IdempotencyKey.generate(timestampMillis = 1_762_000_300_000L)
        assertTrue(UuidV7.parse(generated.value).isSuccess)

        val valid = IdempotencyKey.of("  mutation-key-0001  ")
        assertEquals("mutation-key-0001", valid.getOrNull()?.value)

        val tooShort = IdempotencyKey.of("short")
        assertTrue(tooShort.isFailure)
        assertIs<DomainError.Validation>(tooShort.errorOrNull())

        val tooLong = IdempotencyKey.of("a".repeat(129))
        assertTrue(tooLong.isFailure)
        assertIs<DomainError.Validation>(tooLong.errorOrNull())
    }
}
