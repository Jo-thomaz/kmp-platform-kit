package dev.scaffold.kmp.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

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
}
