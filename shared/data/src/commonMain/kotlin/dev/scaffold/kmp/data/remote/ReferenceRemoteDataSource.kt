package dev.scaffold.kmp.data.remote

import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.IdempotencyKey
import dev.scaffold.kmp.core.Outcome
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.domain.model.ReferenceItem
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
import kotlinx.serialization.Serializable

@Serializable
data class ReferenceItemDto(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
) {
    fun toDomain(): Outcome<ReferenceItem> =
        UuidV7.parse(id).map { uuid ->
            ReferenceItem(
                id = uuid,
                title = title,
                description = description,
                isCompleted = isCompleted,
            )
        }

    companion object {
        fun fromDomain(item: ReferenceItem): ReferenceItemDto =
            ReferenceItemDto(
                id = item.id.value,
                title = item.title,
                description = item.description,
                isCompleted = item.isCompleted,
            )
    }
}

interface ReferenceRemoteDataSource {
    suspend fun fetchItems(): Outcome<List<ReferenceItem>>

    suspend fun postItem(
        item: ReferenceItem,
        idempotencyKey: IdempotencyKey,
    ): Outcome<ReferenceItem>
}

class KtorReferenceRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://api.example.com",
) : ReferenceRemoteDataSource {
    override suspend fun fetchItems(): Outcome<List<ReferenceItem>> =
        executeRequest {
            val response = httpClient.get("$baseUrl/api/v1/items")
            if (!response.status.isSuccess()) {
                return@executeRequest Outcome.failure(mapHttpStatus(response))
            }
            val dtos = response.body<List<ReferenceItemDto>>()
            val items = mutableListOf<ReferenceItem>()
            for (dto in dtos) {
                when (val mapped = dto.toDomain()) {
                    is Outcome.Success -> items.add(mapped.value)
                    is Outcome.Failure -> return@executeRequest mapped
                }
            }
            Outcome.success(items)
        }

    override suspend fun postItem(
        item: ReferenceItem,
        idempotencyKey: IdempotencyKey,
    ): Outcome<ReferenceItem> =
        executeRequest {
            val response = httpClient.post("$baseUrl/api/v1/items") {
                contentType(ContentType.Application.Json)
                header(HEADER_IDEMPOTENCY_KEY, idempotencyKey.value)
                setBody(ReferenceItemDto.fromDomain(item))
            }
            if (!response.status.isSuccess()) {
                return@executeRequest Outcome.failure(mapHttpStatus(response))
            }
            response.body<ReferenceItemDto>().toDomain()
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
