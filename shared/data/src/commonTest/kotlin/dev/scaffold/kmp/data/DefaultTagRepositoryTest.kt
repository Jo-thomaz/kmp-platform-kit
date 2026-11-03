package dev.scaffold.kmp.data

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.data.local.InMemoryTagLocalDataSource
import dev.scaffold.kmp.data.remote.KtorTagRemoteDataSource
import dev.scaffold.kmp.data.remote.TagDto
import dev.scaffold.kmp.data.repository.DefaultTagRepository
import dev.scaffold.kmp.domain.model.Tag
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultTagRepositoryTest {
    @Test
    fun tagDtoSerializesColorTokenPropertyAsColorTokenSnakeCaseJsonField() {
        val id = UuidV7.generate(timestampMillis = 1_762_000_010_050L)
        val domainTag = Tag(
            id = id,
            name = "Urgent",
            colorToken = "statusError",
        )
        val dto = TagDto.fromDomain(domainTag)
        val encodedJson = Json.encodeToString(dto)

        assertTrue(
            encodedJson.contains(""""color_token":"statusError""""),
            "Expected wire JSON to use 'color_token' field, but was: $encodedJson",
        )
        assertFalse(
            encodedJson.contains(""""colorToken""""),
            "Wire JSON must not expose camelCase Kotlin property name 'colorToken': $encodedJson",
        )

        val decoded = Json.decodeFromString<TagDto>(encodedJson).toDomain()
        assertEquals(domainTag, decoded.getOrNull())
    }

    @Test
    fun tagDtoWithInvalidUuidReturnsValidationFailure() {
        val dto = TagDto(
            id = "not-a-uuid-v7",
            name = "Invalid",
            colorToken = "actionPrimary",
        )
        val outcome = dto.toDomain()
        assertTrue(outcome.isFailure)
        assertIs<DomainError.Validation>(outcome.errorOrNull())
    }

    @Test
    fun refreshTagsUpdatesObservedFlowFromRemote() = runTest {
        val sampleId = UuidV7.generate(timestampMillis = 1_762_000_010_100L)
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls += 1
            respond(
                content = """[{"id":"${sampleId.value}","name":"Architecture","color_token":"actionPrimary"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val local = InMemoryTagLocalDataSource()
        val repo = DefaultTagRepository(
            remoteDataSource = KtorTagRemoteDataSource(client),
            localDataSource = local,
        )

        assertTrue(repo.observeTags().first().isEmpty())

        val refreshOutcome = repo.refreshTags()
        assertTrue(refreshOutcome.isSuccess)
        assertEquals(1, networkCalls)
        val observed = repo.observeTags().first()
        assertEquals(1, observed.size)
        assertEquals("Architecture", observed.single().name)
        assertEquals("actionPrimary", observed.single().colorToken)
    }

    @Test
    fun saveTagSendsIdempotencyHeaderAndEmitsToObservedFlow() = runTest {
        val id = UuidV7.generate(timestampMillis = 1_762_000_010_200L)
        val key = IdempotencyKey.generate(timestampMillis = 1_762_000_010_201L)
        var capturedHeader: String? = null

        val mockEngine = MockEngine { request ->
            capturedHeader = request.headers[KtorTagRemoteDataSource.HEADER_IDEMPOTENCY_KEY]
            respond(
                content = """{"id":"${id.value}","name":"DesignSystem","color_token":"surfaceElevated"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val local = InMemoryTagLocalDataSource()
        val repo = DefaultTagRepository(
            remoteDataSource = KtorTagRemoteDataSource(client),
            localDataSource = local,
        )

        val outcome = repo.saveTag(
            tag = Tag(id = id, name = "DesignSystem", colorToken = "surfaceElevated"),
            idempotencyKey = key,
        )

        assertTrue(outcome.isSuccess)
        assertEquals(key.value, capturedHeader)
        assertEquals(1, repo.observeTags().first().size)
    }

    @Test
    fun conflictStatusMapsToDomainErrorConflict() = runTest {
        val id = UuidV7.generate(timestampMillis = 1_762_000_010_300L)
        val key = IdempotencyKey.generate(timestampMillis = 1_762_000_010_301L)
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
        val repo = DefaultTagRepository(
            remoteDataSource = KtorTagRemoteDataSource(client),
            localDataSource = InMemoryTagLocalDataSource(),
        )

        val outcome = repo.saveTag(
            tag = Tag(id = id, name = "Duplicate", colorToken = "statusError"),
            idempotencyKey = key,
        )

        assertTrue(outcome.isFailure)
        assertIs<DomainError.Conflict>(outcome.errorOrNull())
    }
}
