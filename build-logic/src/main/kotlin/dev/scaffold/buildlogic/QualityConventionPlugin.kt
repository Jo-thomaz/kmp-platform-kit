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
            "Modifier" to Regex("""(?<![A-Za-z0-9_])Modifier(?![A-Za-z0-9_])"""),
            ".dp" to Regex("""(?<![A-Za-z0-9_])\d+\.dp(?![A-Za-z0-9_])"""),
            ".sp" to Regex("""(?<![A-Za-z0-9_])\d+\.sp(?![A-Za-z0-9_])"""),
            "Composable" to Regex("""(?<![A-Za-z0-9_])@?Composable(?![A-Za-z0-9_])"""),
            "MaterialTheme" to Regex("""(?<![A-Za-z0-9_])MaterialTheme(?![A-Za-z0-9_])"""),
            "RoundedCornerShape" to Regex("""(?<![A-Za-z0-9_])RoundedCornerShape(?![A-Za-z0-9_])"""),
            "Arrangement" to Regex("""(?<![A-Za-z0-9_])Arrangement(?![A-Za-z0-9_])"""),
            "Alignment.CenterVertically" to Regex("""(?<![A-Za-z0-9_])Alignment\.CenterVertically(?![A-Za-z0-9_])"""),
            "PaddingValues" to Regex("""(?<![A-Za-z0-9_])PaddingValues(?![A-Za-z0-9_])"""),
            "remember" to Regex("""(?<![A-Za-z0-9_])remember(?![A-Za-z0-9_])"""),
            "Color(0xFF...)" to Regex("""(?<![A-Za-z0-9_])Color\s*\(\s*0x[0-9A-Fa-f]{6,8}\s*\)"""),
        )

        val violations = mutableListOf<String>()
        for (file in swiftFiles.files.sortedBy { it.path }) {
            if (!file.isFile) continue
            var inBlockComment = false
            file.useLines { lines ->
                lines.forEachIndexed { idx, rawLine ->
                    val codeLine = stripComments(rawLine, inBlockComment).also {
                        inBlockComment = it.second
                    }.first
                    if (codeLine.isBlank()) return@forEachIndexed
                    for ((label, regex) in forbiddenRules) {
                        if (regex.containsMatchIn(codeLine)) {
                            violations.add("${file.name}:${idx + 1}: forbidden Compose identifier '$label' in `$rawLine`")
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

    private fun stripComments(line: String, startInBlockComment: Boolean): Pair<String, Boolean> {
        val sb = StringBuilder()
        var inBlock = startInBlockComment
        var i = 0
        while (i < line.length) {
            if (inBlock) {
                val closeIdx = line.indexOf("*/", i)
                if (closeIdx == -1) {
                    return sb.toString() to true
                }
                inBlock = false
                i = closeIdx + 2
            } else {
                if (i + 1 < line.length && line[i] == '/' && line[i + 1] == '/') {
                    break
                } else if (i + 1 < line.length && line[i] == '/' && line[i + 1] == '*') {
                    inBlock = true
                    i += 2
                } else {
                    sb.append(line[i])
                    i += 1
                }
            }
        }
        return sb.toString() to inBlock
    }
}

@CacheableTask
abstract class CheckNoMachinePathsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trackedFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val stampFile: RegularFileProperty

    @TaskAction
    fun check() {
        val forbiddenTokens = listOf(
            "C:\\" + "Users\\",
            "/" + "Users/",
            "/" + "home/",
            "file:" + "///",
        )

        val violations = mutableListOf<String>()
        for (file in trackedFiles.files.sortedBy { it.path }) {
            if (!file.isFile) continue
            file.useLines { lines ->
                lines.forEachIndexed { idx, line ->
                    for (token in forbiddenTokens) {
                        if (line.contains(token)) {
                            violations.add("${file.name}:${idx + 1}: machine-specific path '$token' in `$line`")
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Machine-specific path check failed — tracked files must not contain " +
                    "C:\\Users\\, /Users/, /home/, or file:///:\n" +
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

            val checkNoMachinePathsTask = tasks.register<CheckNoMachinePathsTask>("checkNoMachinePaths") {
                group = "verification"
                description = "Fails if any tracked file contains machine-specific paths (C:\\Users\\, /Users/, /home/, file:///)"
                trackedFiles.from(
                    fileTree(layout.projectDirectory) {
                        exclude(
                            "**/.git/**",
                            "**/.gradle/**",
                            "**/.kotlin/**",
                            "**/.idea/**",
                            "**/build/**",
                            "**/DerivedData/**",
                            "**/*.xcodeproj/**",
                            "**/*.xcworkspace/**",
                            "**/local.properties",
                            "**/.DS_Store",
                            "**/.env",
                            "**/*.iml",
                            "**/*.png",
                            "**/*.jar",
                            "**/*.bin",
                            "build-logic/src/main/kotlin/dev/scaffold/buildlogic/QualityConventionPlugin.kt",
                        )
                    },
                )
                stampFile.set(layout.buildDirectory.file("reports/machine-paths-check/check.stamp"))
            }

            tasks.named("check") {
                dependsOn(checkSwiftTask, checkNoMachinePathsTask)
            }

            tasks.register("verifyAll") {
                group = "verification"
                description = "Runs all non-macOS repository verification checks including checkNoMachinePaths."
                dependsOn(
                    "check",
                    "checkSwiftSources",
                    "checkNoMachinePaths",
                    "detekt",
                )
                tasks.findByName("checkDesignTokens")?.let { dependsOn(it) }
                subprojects.forEach { sub ->
                    dependsOn(
                        sub.tasks.matching {
                            it.name == "jvmTest" ||
                                it.name == "detekt" ||
                                (sub.path == ":tools:architecture-test" && it.name == "test")
                        },
                    )
                }
            }
        }
    }
}
