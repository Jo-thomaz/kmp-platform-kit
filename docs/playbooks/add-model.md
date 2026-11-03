# Playbook: Add a Domain Model, DTO, Repository Port & Data Adapter

## Purpose
Add a new shared domain entity (`{Model}`) with its repository port in `:shared:domain`, plus its wire DTO, mapper (`toDomain()` / `fromDomain()`), in-memory local data source, Ktor remote data source, repository implementation, and unit tests in `:shared:data`.

**Prerequisites:** Requires a configured Android SDK and JDK; see `CONTRIBUTING.md`.

---

## Inputs I Provide (Spec Template)

```yaml
modelName: Tag                 # PascalCase model name ({Model}); camelCase = {model}, plural = {Models} / {models}
endpointPath: /api/v1/tags     # REST collection path for GET (list) and POST (create/save)
fields:
  - kotlinName: id
    kotlinType: UuidV7         # Wire JSON type is String (UUIDv7 string)
    jsonName: id
  - kotlinName: name
    kotlinType: String
    jsonName: name
  - kotlinName: colorToken
    kotlinType: String
    jsonName: color_token      # Example where wire name differs from Kotlin camelCase
operations:
  - observeAll: Flow<List<Tag>>
  - refresh: Outcome<Unit>
  - save: Outcome<Tag>         # Requires IdempotencyKey
```

---

## Placeholders & Exact File Paths

Read `packageId` from `scaffold.yaml` (e.g., `dev.scaffold.kmp`), and derive `{packagePath}` by replacing `.` with `/` (e.g., `dev/scaffold/kmp`).

| Layer | Exact File Path |
|---|---|
| Domain Model | `shared/domain/src/commonMain/kotlin/{packagePath}/domain/model/{Model}.kt` |
| Domain Repository Port | `shared/domain/src/commonMain/kotlin/{packagePath}/domain/repository/{Model}Repository.kt` |
| Local Data Source + In-Memory Adapter | `shared/data/src/commonMain/kotlin/{packagePath}/data/local/{Model}LocalDataSource.kt` |
| Wire DTO + Mapper + Remote Data Source | `shared/data/src/commonMain/kotlin/{packagePath}/data/remote/{Model}RemoteDataSource.kt` |
| Repository Adapter | `shared/data/src/commonMain/kotlin/{packagePath}/data/repository/Default{Model}Repository.kt` |
| Repository & DTO Wire Tests | `shared/data/src/commonTest/kotlin/{packagePath}/data/Default{Model}RepositoryTest.kt` |
| (Optional) iOS Interop Helpers | `shared/umbrella/src/commonMain/kotlin/{packagePath}/umbrella/{Model}IosInterop.kt` |

---

## Reference Files to Copy Patterns From

- **Domain Model:** `shared/domain/src/commonMain/kotlin/{packagePath}/domain/model/ReferenceItem.kt`
- **Domain Repository Port:** `shared/domain/src/commonMain/kotlin/{packagePath}/domain/repository/ReferenceRepository.kt`
- **Local Data Source & In-Memory Adapter:** `shared/data/src/commonMain/kotlin/{packagePath}/data/local/ReferenceLocalDataSource.kt`
- **DTO, Mapper & Remote Data Source:** `shared/data/src/commonMain/kotlin/{packagePath}/data/remote/ReferenceRemoteDataSource.kt`
- **Repository Implementation:** `shared/data/src/commonMain/kotlin/{packagePath}/data/repository/DefaultReferenceRepository.kt`
- **Unit & Wire Contract Tests:** `shared/data/src/commonTest/kotlin/{packagePath}/data/DefaultReferenceRepositoryTest.kt`

---

## Rules That Apply

1. **`Outcome` not `kotlin.Result`:** Every fallible function (including `Dto.toDomain()`, remote calls, and repository commands) must return `dev.scaffold.kmp.core.Outcome<T>`. Never use `kotlin.Result` or `runCatching` (enforced by Detekt and Konsist).
2. **`NSObject` Collision Ban:** Public properties and public functions in `shared/` modules must **never** be named `description`, `hash`, `isEqual`, or `debugDescription` (any parameter count). Use `summary` or another domain-specific name instead (enforced by Konsist).
3. **DTO Wire Contract (`@SerialName`):** The DTO represents the external JSON wire contract and must not break when Kotlin properties are renamed or use camelCase. Always annotate **every** DTO property with `@SerialName("<json_field>")` (defaulting to `snake_case` for multi-word properties when the user prompt only gives Kotlin camelCase names, e.g., `@SerialName("color_token") val colorToken: String`, or preserving legacy wire names like `@SerialName("description") val summary: String`), and add a test asserting the serialized JSON field names.
4. **Platform-Agnostic Domain:** `:shared:domain` must contain pure Kotlin domain models and ports only (`UuidV7`, `IdempotencyKey`, `Outcome`, `Flow`). Never add Objective-C/Swift interop helpers (`idValue`, `@ObjCName`, etc.) in `:shared:domain`; if iOS needs unmangled helpers, place them in `:shared:umbrella`.
5. **Reactive Reads + Idempotent Writes:** Repository read streams must expose `Flow<List<{Model}>>` backed by the local data source (`MutableStateFlow` + `Mutex` in the in-memory adapter). Mutation methods must accept `IdempotencyKey` and send it in the `Idempotency-Key` HTTP header.
6. **UI & Token Separation:** Domain and data layers must not reference UI components or raw color hex values; if a model carries a theme reference (e.g., `colorToken`), store the semantic token identifier as a `String`.
7. **Presenter Lifecycle & File Size Limit:** When a model is later wired into a presenter, both platforms must honour presenter `close()` lifecycle (`ViewModelProvider.Factory` + `onCleared()` on Android; `.task { await viewModel.run() }` + `deinit` on iOS). Every source file must stay **<= 350 lines**.

---

## Step-by-Step Procedure

1. **Create the Domain Model (`{Model}.kt`):**
   - Define `data class {Model}(val id: UuidV7, ...)` in `{packageId}.domain.model`.
   - Ensure no property or function name collides with `NSObject` (`description`, `hash`, `isEqual`, `debugDescription`).
2. **Create the Repository Port (`{Model}Repository.kt`):**
   - Define `interface {Model}Repository` in `{packageId}.domain.repository` with:
     - `fun observe{Models}(): Flow<List<{Model}>>`
     - `suspend fun refresh{Models}(): Outcome<Unit>`
     - `suspend fun save{Model}({model}: {Model}, idempotencyKey: IdempotencyKey): Outcome<{Model}>`
3. **Create the Local Data Source (`{Model}LocalDataSource.kt`):**
   - Define `interface {Model}LocalDataSource` (`observeAll()`, `getAll()`, `replaceAll(items: List<{Model}>)`, `upsert(item: {Model})`) and `class InMemory{Model}LocalDataSource` using `Mutex`, `LinkedHashMap<UuidV7, {Model}>`, and `MutableStateFlow<List<{Model}>>`.
4. **Create the DTO, Mapper & Remote Data Source (`{Model}RemoteDataSource.kt`):**
   - Define `@Serializable data class {Model}Dto` with explicit `@SerialName(...)` on every property (using `snake_case` when the spec only provides camelCase Kotlin field names).
   - Add `fun toDomain(): Outcome<{Model}>` (validating `id` with `UuidV7.parse(id).map { ... }`) and `companion object { fun fromDomain({model}: {Model}): {Model}Dto }`.
   - Define `interface {Model}RemoteDataSource` with:
     - `suspend fun fetch{Models}(): Outcome<List<{Model}>>`
     - `suspend fun post{Model}({model}: {Model}, idempotencyKey: IdempotencyKey): Outcome<{Model}>`
   - Define `class Ktor{Model}RemoteDataSource(private val httpClient: HttpClient, private val baseUrl: String = "https://api.example.com")` with `companion object { const val HEADER_IDEMPOTENCY_KEY = "Idempotency-Key" }`, mapping HTTP 401 -> `DomainError.Unauthorized`, HTTP 409 -> `DomainError.Conflict`, other non-2xx -> `DomainError.Network(statusCode = ...)`, and network exceptions -> `DomainError.Network`.
5. **Create the Repository Implementation (`Default{Model}Repository.kt`):**
   - Implement `{Model}Repository` coordinating `{Model}RemoteDataSource` and `{Model}LocalDataSource`.
6. **Write Unit & Wire Contract Tests (`Default{Model}RepositoryTest.kt`):**
   - Test 1: DTO wire serialization/deserialization asserting exact JSON field names (`@SerialName` verification) and round-trip `toDomain()`.
   - Test 2: `toDomain()` on a DTO with an invalid `id` returns `Outcome.Failure(DomainError.Validation)`.
   - Test 3: `refresh{Models}()` fetches from Ktor `MockEngine` and updates `observe{Models}()`.
   - Test 4: `save{Model}()` sends the `Idempotency-Key` header and updates `observe{Models}()`.
   - Test 5: HTTP 409 Conflict maps to `Outcome.Failure(DomainError.Conflict)`.

---

## Gate Commands to Run

```powershell
./gradlew.bat :shared:domain:allTests :shared:data:allTests :tools:architecture-test:test detekt
```

---

## Definition of Done

- [ ] `{Model}.kt` and `{Model}Repository.kt` exist in `:shared:domain` with zero platform/interop leakage.
- [ ] `{Model}Dto`, `{Model}LocalDataSource`, `{Model}RemoteDataSource`, and `Default{Model}Repository` exist in `:shared:data`.
- [ ] `Default{Model}RepositoryTest.kt` covers DTO `@SerialName` wire format, `refresh`, `save` with `Idempotency-Key`, and error mapping.
- [ ] All files are <= 350 lines.
- [ ] `:shared:domain:allTests`, `:shared:data:allTests`, `:tools:architecture-test:test`, and `detekt` pass with 0 warnings/errors.
