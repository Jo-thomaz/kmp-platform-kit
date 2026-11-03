package dev.scaffold.kmp.data.remote

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.domain.model.Tag
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("color_token")
    val colorToken: String,
) {
    fun toDomain(): Outcome<Tag> =
        UuidV7.parse(id).map { uuid ->
            Tag(
                id = uuid,
                name = name,
                colorToken = colorToken,
            )
        }

    companion object {
        fun fromDomain(tag: Tag): TagDto =
            TagDto(
                id = tag.id.value,
                name = tag.name,
                colorToken = tag.colorToken,
            )
    }
}

interface TagRemoteDataSource {
    suspend fun fetchTags(): Outcome<List<Tag>>

    suspend fun postTag(
        tag: Tag,
        idempotencyKey: IdempotencyKey,
    ): Outcome<Tag>
}

class KtorTagRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://api.example.com",
) : TagRemoteDataSource {
    override suspend fun fetchTags(): Outcome<List<Tag>> =
        executeRequest {
            val response = httpClient.get("$baseUrl/api/v1/tags")
            if (!response.status.isSuccess()) {
                return@executeRequest Outcome.failure(mapHttpStatus(response))
            }
            val dtos = response.body<List<TagDto>>()
            val tags = mutableListOf<Tag>()
            for (dto in dtos) {
                when (val mapped = dto.toDomain()) {
                    is Outcome.Success -> tags.add(mapped.value)
                    is Outcome.Failure -> return@executeRequest mapped
                }
            }
            Outcome.success(tags)
        }

    override suspend fun postTag(
        tag: Tag,
        idempotencyKey: IdempotencyKey,
    ): Outcome<Tag> =
        executeRequest {
            val response = httpClient.post("$baseUrl/api/v1/tags") {
                contentType(ContentType.Application.Json)
                header(HEADER_IDEMPOTENCY_KEY, idempotencyKey.value)
                setBody(TagDto.fromDomain(tag))
            }
            if (!response.status.isSuccess()) {
                return@executeRequest Outcome.failure(mapHttpStatus(response))
            }
            response.body<TagDto>().toDomain()
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <T> executeRequest(block: () -> Outcome<T>): Outcome<T> =
        try {
            block()
        } catch (e: Throwable) {
            Outcome.failure(
                DomainError.Network(
                    message = e.message ?: "Network request failed",
                ),
            )
        }

    private fun mapHttpStatus(response: HttpResponse): DomainError =
        when (response.status) {
            HttpStatusCode.Unauthorized -> DomainError.Unauthorized()
            HttpStatusCode.Conflict -> DomainError.Conflict("Conflict: duplicate or stale mutation")
            else -> DomainError.Network(
                message = "HTTP ${response.status.value}",
                statusCode = response.status.value,
            )
        }

    companion object {
        const val HEADER_IDEMPOTENCY_KEY = "Idempotency-Key"
    }
}
