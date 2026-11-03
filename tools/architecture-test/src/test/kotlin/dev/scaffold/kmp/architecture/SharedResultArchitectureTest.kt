package dev.scaffold.kmp.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SharedResultArchitectureTest {
    private val sharedScope = Konsist.scopeFromDirectories(
        listOf(
            "shared/core/src",
            "shared/domain/src",
            "shared/data/src",
            "shared/feature/reference/src",
            "shared/umbrella/src",
        ),
    )

    private val domainScope = Konsist.scopeFromDirectories(
        listOf("shared/domain/src"),
    )

    private val objcInteropForbiddenAnnotations = setOf(
        "ObjCName",
        "HiddenFromObjC",
        "ShouldRefineInSwift",
        "ObjCObject",
        "ExportObjCClass",
        "BetaInteropApi",
    )

    private val objcInteropForbiddenDeclarationNames = setOf(
        "idValue",
        "itemId",
        "IosInterop",
        "SwiftInterop",
        "ObjCInterop",
    )

    private val nsObjectForbiddenNames = setOf(
        "description",
        "hash",
        "isEqual",
        "debugDescription",
    )

    private fun isKotlinResultTypeName(text: String): Boolean {
        val cleaned = text.trim()
        return cleaned == "Result" ||
            cleaned.startsWith("Result<") ||
            cleaned.startsWith("kotlin.Result")
    }

    @Test
    fun `shared functions must not use kotlin Result in return types or parameters or call runCatching`() {
        val msg = "Shared functions must use dev.scaffold.kmp.core.Outcome " +
            "instead of kotlin.Result or runCatching."
        sharedScope
            .functions()
            .assertFalse(additionalMessage = msg) { function ->
                val returnTypeViolation = function.returnType?.let { type ->
                    isKotlinResultTypeName(type.text) || type.name == "Result"
                } ?: false

                val paramViolation = function.parameters.any { param ->
                    isKotlinResultTypeName(param.type.text) || param.type.name == "Result"
                }

                val bodyViolation = Regex("""\brunCatching\s*[({<]|\bkotlin\.Result\b""")
                    .containsMatchIn(function.text)

                returnTypeViolation || paramViolation || bodyViolation
            }
    }

    @Test
    fun `shared properties must not use kotlin Result`() {
        val msg = "Shared properties must use dev.scaffold.kmp.core.Outcome " +
            "instead of kotlin.Result."
        sharedScope
            .properties()
            .assertFalse(additionalMessage = msg) { property ->
                property.type?.let { type ->
                    isKotlinResultTypeName(type.text) || type.name == "Result"
                } ?: false
            }
    }

    @Test
    fun `shared public properties must not collide with NSObject property names`() {
        val msg = "Public properties in shared modules must not be named " +
            "description, hash, isEqual, or debugDescription (they collide with NSObject on iOS)."
        sharedScope
            .properties()
            .filter { it.hasPublicOrDefaultModifier }
            .assertFalse(additionalMessage = msg) { property ->
                property.name in nsObjectForbiddenNames
            }
    }

    @Test
    fun `shared public functions must not collide with NSObject method names`() {
        val msg = "Public functions in shared modules must not be named " +
            "description, hash, isEqual, or debugDescription (they collide with NSObject on iOS)."
        sharedScope
            .functions()
            .filter { it.hasPublicOrDefaultModifier }
            .assertFalse(additionalMessage = msg) { function ->
                function.name in nsObjectForbiddenNames
            }
    }

    @Test
    fun `domain module must remain platform agnostic and free of ObjC interop helpers or annotations`() {
        val msg = "shared/domain must remain platform-agnostic; place Objective-C/Swift interop " +
            "extensions and factories in :shared:umbrella."
        domainScope
            .files
            .assertFalse(additionalMessage = msg) { file ->
                val hasObjcImport = file.imports.any { imp ->
                    imp.name.startsWith("kotlinx.cinterop") ||
                        imp.name.startsWith("kotlin.native") ||
                        imp.name.startsWith("platform.")
                }
                val hasForbiddenAnnotation = (file.classes() + file.functions() + file.properties())
                    .flatMap { it.annotations }
                    .any { it.name in objcInteropForbiddenAnnotations }
                val hasForbiddenMemberName = (file.functions().map { it.name } + file.properties().map { it.name })
                    .any { it in objcInteropForbiddenDeclarationNames }
                hasObjcImport || hasForbiddenAnnotation || hasForbiddenMemberName
            }
    }

    @Test
    fun `source files must not exceed 350 lines`() {
        val maxLines = 350
        val repoRoot = generateSequence(File(".").canonicalFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        val checkedDirs = listOf("shared", "androidApp", "iosApp", "build-logic", "tools")
        val checkedExtensions = setOf("kt", "kts", "swift")

        val violations = checkedDirs
            .map { File(repoRoot, it) }
            .filter { it.exists() }
            .flatMap { dir ->
                dir.walkTopDown()
                    .onEnter { it.name != "build" && it.name != ".gradle" }
                    .filter { it.isFile && it.extension in checkedExtensions }
                    .mapNotNull { file ->
                        val count = file.useLines { it.count() }
                        if (count > maxLines) {
                            "${file.relativeTo(repoRoot).invariantSeparatorsPath} ($count lines > $maxLines max)"
                        } else {
                            null
                        }
                    }
                    .toList()
            }

        assertTrue(
            violations.isEmpty(),
            "Source files must not exceed $maxLines lines for readability and maintainability:\n" +
                violations.joinToString("\n") { "  - $it" },
        )
    }
}
