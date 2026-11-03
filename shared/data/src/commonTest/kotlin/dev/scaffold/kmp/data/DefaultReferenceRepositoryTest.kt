package dev.scaffold.kmp.data

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.data.local.InMemoryReferenceLocalDataSource
import dev.scaffold.kmp.data.remote.KtorReferenceRemoteDataSource
import dev.scaffold.kmp.data.repository.DefaultReferenceRepository
import dev.scaffold.kmp.domain.model.ReferenceItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultReferenceRepositoryTest {
    @Test
    fun refreshItemsUpdatesObservedFlowFromRemote() = runTest {
        val sampleId = UuidV7.generate(timestampMillis = 1_762_000_000_100L)
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls += 1
            respond(
                content = """[{"id":"${sampleId.value}","title":"First","description":"Desc","isCompleted":false}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val local = InMemoryReferenceLocalDataSource()
        val repo = DefaultReferenceRepository(
            remoteDataSource = KtorReferenceRemoteDataSource(client),
            localDataSource = local,
        )

        assertTrue(repo.observeItems().first().isEmpty())

        val refreshOutcome = repo.refreshItems()
        assertTrue(refreshOutcome.isSuccess)
        assertEquals(1, networkCalls)
        assertEquals(1, repo.observeItems().first().size)
        assertEquals("First", repo.observeItems().first().single().title)
    }

    @Test
    fun saveItemSendsIdempotencyHeaderAndEmitsToObservedFlow() = runTest {
        val id = UuidV7.generate(timestampMillis = 1_762_000_000_200L)
        val key = IdempotencyKey.generate(timestampMillis = 1_762_000_000_201L)
        var capturedHeader: String? = null

        val mockEngine = MockEngine { request ->
            capturedHeader = request.headers[KtorReferenceRemoteDataSource.HEADER_IDEMPOTENCY_KEY]
            respond(
                content = """{"id":"${id.value}","title":"Saved","description":"Body","isCompleted":false}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val local = InMemoryReferenceLocalDataSource()
        val repo = DefaultReferenceRepository(
            remoteDataSource = KtorReferenceRemoteDataSource(client),
            localDataSource = local,
        )

        val outcome = repo.saveItem(
            item = ReferenceItem(id = id, title = "Saved", description = "Body"),
            idempotencyKey = key,
        )

        assertTrue(outcome.isSuccess)
        assertEquals(key.value, capturedHeader)
        assertEquals(1, repo.observeItems().first().size)
    }

    @Test
    fun conflictStatusMapsToDomainErrorConflict() = runTest {
        val id = UuidV7.generate(timestampMillis = 1_762_000_000_300L)
        val key = IdempotencyKey.generate(timestampMillis = 1_762_000_000_301L)
        val mockEngine = MockEngine {
            respond(
                content = """{"error":"conflict"}""",
                status = HttpStatusCode.Conflict,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val repo = DefaultReferenceRepository(
            remoteDataSource = KtorReferenceRemoteDataSource(client),
            localDataSource = InMemoryReferenceLocalDataSource(),
        )

        val outcome = repo.saveItem(
            item = ReferenceItem(id = id, title = "Conflict", description = "Body"),
            idempotencyKey = key,
        )

        assertTrue(outcome.isFailure)
        assertIs<DomainError.Conflict>(outcome.errorOrNull())
    }
}
