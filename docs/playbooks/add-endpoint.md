# Playbook: Add an API Endpoint to a Remote Data Source & Repository

## Purpose
Add a new HTTP endpoint (query or mutation) to an existing Ktor remote data source (`{Model}RemoteDataSource` / `Ktor{Model}RemoteDataSource`), wire it through the repository port (`{Model}Repository` / `Default{Model}Repository`) and domain use case, and verify wire serialization, headers, and HTTP error mapping with Ktor `MockEngine` tests.

**Prerequisites:** Requires a configured Android SDK and JDK; see `CONTRIBUTING.md`.

---

## Inputs I Provide (Spec Template)

```yaml
modelName: ReferenceItem          # Existing model ({Model})
method: POST                      # GET, POST, PUT, PATCH, DELETE
path: /api/v1/items/{id}/complete # Endpoint path relative to baseUrl
requiresIdempotencyKey: true      # true for POST/PATCH mutations that must be retry-safe
requestDto: CompleteItemRequestDto # Optional request body DTO (or none)
responseDto: ReferenceItemDto     # Response DTO (or Unit for 204 No Content)
repositoryMethod: completeItem(id: UuidV7, idempotencyKey: IdempotencyKey): Outcome<ReferenceItem>
```

---

## Placeholders & Exact File Paths

Read `packageId` from `scaffold.yaml` (e.g., `dev.scaffold.kmp`) and derive `{packagePath}` (`dev/scaffold/kmp`).

| Layer | Exact File Path |
|---|---|
| Domain Repository Port | `shared/domain/src/commonMain/kotlin/{packagePath}/domain/repository/{Model}Repository.kt` |
| Domain Use Case | `shared/domain/src/commonMain/kotlin/{packagePath}/domain/usecase/{Model}UseCases.kt` |
| Domain Use Case Test | `shared/domain/src/commonTest/kotlin/{packagePath}/domain/{Model}UseCasesTest.kt` |
| Local Data Source | `shared/data/src/commonMain/kotlin/{packagePath}/data/local/{Model}LocalDataSource.kt` |
| Remote Data Source & DTOs | `shared/data/src/commonMain/kotlin/{packagePath}/data/remote/{Model}RemoteDataSource.kt` |
| Repository Implementation | `shared/data/src/commonMain/kotlin/{packagePath}/data/repository/Default{Model}Repository.kt` |
| Ktor `MockEngine` & Wire Tests | `shared/data/src/commonTest/kotlin/{packagePath}/data/Default{Model}RepositoryTest.kt` |

---

## Reference Files to Copy Patterns From

- **Remote Data Source, DTOs & Error Mapping:** `shared/data/src/commonMain/kotlin/{packagePath}/data/remote/ReferenceRemoteDataSource.kt`
- **Repository Port & Implementation:** `shared/domain/src/commonMain/kotlin/{packagePath}/domain/repository/ReferenceRepository.kt`, `shared/data/src/commonMain/kotlin/{packagePath}/data/repository/DefaultReferenceRepository.kt`
- **MockEngine Tests:** `shared/data/src/commonTest/kotlin/{packagePath}/data/DefaultReferenceRepositoryTest.kt`

---

## Rules That Apply

1. **`Outcome` not `kotlin.Result`:** Remote data source methods, DTO mappers, repository methods, and use cases must return `Outcome<T>`. Never use `kotlin.Result` or `runCatching`.
2. **`NSObject` Collision Ban:** Never name public properties or public functions `description`, `hash`, `isEqual`, or `debugDescription` in `shared/` modules.
3. **DTO `@SerialName` Wire Contract:** Request and response DTOs in `:shared:data` must use `@Serializable` and `@SerialName("<wire_name>")` whenever the JSON key differs from the Kotlin property name. Never leak DTO types into `:shared:domain`.
4. **Idempotency Header on Mutations:** Every non-idempotent write (`POST` / mutation) must accept `idempotencyKey: IdempotencyKey` and attach `header("Idempotency-Key", idempotencyKey.value)`.
5. **Typed Error Mapping:** Map HTTP statuses deterministically:
   - `401 Unauthorized` -> `DomainError.Unauthorized()`
   - `404 Not Found` -> `DomainError.NotFound(resource = ..., id = ...)`
   - `409 Conflict` -> `DomainError.Conflict(...)`
   - `400` / `422` -> `DomainError.Validation(...)`
   - Other non-2xx statuses -> `DomainError.Network(message = "HTTP ${response.status.value}", statusCode = response.status.value)`
   - Thrown exceptions / IO failures -> `DomainError.Network(message = e.message ?: "Network request failed")`
6. **Offline-First Local Cache Sync:** After a successful remote mutation or fetch, update `{Model}LocalDataSource` (`upsert` or `replaceAll`) so active `observe...(): Flow<List<{Model}>>` collectors receive the updated state automatically.
7. **No Logic in UI / Tokens Only / Lifecycle / File Size Limit:** Keep endpoint orchestration in use cases/presenters (never in UI), use semantic tokens only in UI, preserve presenter `close()` lifecycle on Android and iOS, and keep every file **<= 350 lines**.

---

## Step-by-Step Procedure

1. **Add Request/Response DTOs (if needed) in `{Model}RemoteDataSource.kt`:**
   - Annotate with `@Serializable` and `@SerialName(...)` for wire keys.
   - Add `toDomain(): Outcome<...>` and/or `fromDomain(...)` mapper functions.
2. **Extend `{Model}RemoteDataSource` & `Ktor{Model}RemoteDataSource`:**
   - Add the `suspend` method returning `Outcome<...>`.
   - Wrap the Ktor call in `executeRequest { ... }`, attach `ContentType.Application.Json` and `Idempotency-Key` (if applicable), check `response.status.isSuccess()`, and map the response DTO via `.toDomain()`.
3. **Extend `{Model}Repository` & `Default{Model}Repository`:**
   - Add the method to the domain repository interface in `:shared:domain`.
   - Implement it in `Default{Model}Repository`, updating `{Model}LocalDataSource` on `Outcome.Success`.
4. **Add Ktor `MockEngine` Tests in `Default{Model}RepositoryTest.kt`:**
   - Test request method, URL path, `Idempotency-Key` header, serialized JSON field names, local `Flow` emission on `200`/`201`, and `DomainError` mapping on `401`/`409`/`503`.

---

## Gate Commands to Run

```powershell
./gradlew.bat :shared:domain:allTests :shared:data:allTests :tools:architecture-test:test detekt
```

---

## Definition of Done

- [ ] Endpoint is declared on `{Model}RemoteDataSource` and implemented on `Ktor{Model}RemoteDataSource`.
- [ ] Repository port and `Default{Model}Repository` expose the operation and synchronize `{Model}LocalDataSource`.
- [ ] `Default{Model}RepositoryTest.kt` verifies request path/method/headers, `@SerialName` wire JSON, `Flow` state update, and error mapping.
- [ ] All modified files are <= 350 lines.
- [ ] `:shared:domain:allTests`, `:shared:data:allTests`, `:tools:architecture-test:test`, and `detekt` pass.
