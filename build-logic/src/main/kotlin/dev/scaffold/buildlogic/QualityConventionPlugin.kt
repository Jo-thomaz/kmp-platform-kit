package dev.scaffold.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

@CacheableTask
abstract class CheckSwiftSourcesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val stampFile: RegularFileProperty

    @TaskAction
    fun check() {
        val forbiddenRules = listOf(
            "Modifier" to Regex("""\bModifier\b"""),
            ".dp" to Regex("""\b\d+\.dp\b"""),
            ".sp" to Regex("""\b\d+\.sp\b"""),
            "Composable" to Regex("""@?Composable\b"""),
            "MaterialTheme" to Regex("""\bMaterialTheme\b"""),
            "RoundedCornerShape" to Regex("""\bRoundedCornerShape\b"""),
            "Arrangement" to Regex("""\bArrangement\b"""),
            "Alignment.CenterVertically" to Regex("""\bAlignment\.CenterVertically\b"""),
            "PaddingValues" to Regex("""\bPaddingValues\b"""),
            "remember" to Regex("""\bremember\s*[({<]"""),
            "Color(0xFF...)" to Regex("""\bColor\s*\(\s*0x[0-9A-Fa-f]{6,8}\s*\)"""),
        )

        val violations = mutableListOf<String>()
        for (file in swiftFiles.files.sortedBy { it.path }) {
            if (!file.isFile) continue
            file.useLines { lines ->
                lines.forEachIndexed { idx, line ->
                    for ((label, regex) in forbiddenRules) {
                        if (regex.containsMatchIn(line)) {
                            violations.add("${file.name}:${idx + 1}: forbidden Compose identifier '$label' in `$line`")
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Swift source check failed — Compose/Android identifiers leaked into iosApp/:\n" +
                    violations.joinToString("\n") { "  - $it" },
            )
        }

        val out = stampFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText("OK")
    }
}

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("base")
        pluginManager.apply("dev.detekt")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            parallel.set(true)
            source.setFrom(
                files(
                    "src/commonMain/kotlin",
                    "src/commonTest/kotlin",
                    "src/androidMain/kotlin",
                    "src/iosMain/kotlin",
                    "src/jvmMain/kotlin",
                    "src/main/kotlin",
                    "src/test/kotlin",
                ),
            )
            val configFile = rootProject.layout.projectDirectory.file("config/detekt/detekt.yml")
            if (configFile.asFile.exists()) {
                config.setFrom(configFile)
            }
        }

        tasks.withType<Detekt>().configureEach {
            exclude("**/build/**", "**/GeneratedTokens.kt")
        }

        if (this == rootProject) {
            val checkSwiftTask = tasks.register<CheckSwiftSourcesTask>("checkSwiftSources") {
                group = "verification"
                description = "Checks that Swift files under iosApp/ do not contain leaked Compose/Android identifiers."
                swiftFiles.from(
                    fileTree("iosApp") {
                        include("**/*.swift")
                    },
                )
                stampFile.set(layout.buildDirectory.file("reports/swift-check/check.stamp"))
            }

            tasks.named("check") {
                dependsOn(checkSwiftTask)
            }
        }
    }
}
