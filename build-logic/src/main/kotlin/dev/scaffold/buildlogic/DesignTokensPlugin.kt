package dev.scaffold.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

@CacheableTask
abstract class GenerateDesignTokensTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:Input
    abstract val packageId: Property<String>

    @get:OutputFile
    abstract val kotlinOutputFile: RegularFileProperty

    @get:OutputFile
    abstract val swiftOutputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val model = DesignTokensParser.parse(tokensFile.get().asFile.readText())
        val ktFile = kotlinOutputFile.get().asFile
        ktFile.parentFile.mkdirs()
        ktFile.writeText(DesignTokensRenderer.renderKotlin(packageId.get(), model))

        val swiftFile = swiftOutputFile.get().asFile
        swiftFile.parentFile.mkdirs()
        swiftFile.writeText(DesignTokensRenderer.renderSwift(model))
    }
}

@CacheableTask
abstract class CheckDesignTokensTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:Input
    abstract val packageId: Property<String>

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinOutputFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftOutputFile: RegularFileProperty

    @get:OutputFile
    abstract val stampFile: RegularFileProperty

    @TaskAction
    fun check() {
        val model = DesignTokensParser.parse(tokensFile.get().asFile.readText())
        val expectedKt = DesignTokensRenderer.renderKotlin(packageId.get(), model)
        val expectedSwift = DesignTokensRenderer.renderSwift(model)

        val actualKtFile = kotlinOutputFile.get().asFile
        val actualSwiftFile = swiftOutputFile.get().asFile

        val actualKt = if (actualKtFile.exists()) actualKtFile.readText().replace("\r\n", "\n") else ""
        val actualSwift = if (actualSwiftFile.exists()) actualSwiftFile.readText().replace("\r\n", "\n") else ""

        val mismatches = mutableListOf<String>()
        if (actualKt != expectedKt) {
            mismatches.add(actualKtFile.relativeToOrSelf(actualKtFile.parentFile).path)
        }
        if (actualSwift != expectedSwift) {
            mismatches.add(actualSwiftFile.relativeToOrSelf(actualSwiftFile.parentFile).path)
        }
        if (mismatches.isNotEmpty()) {
            throw GradleException(
                "Generated design token files are out of date with design-tokens/tokens.json: " +
                    "${mismatches.joinToString(", ")}. Run './gradlew generateDesignTokens' to update them.",
            )
        }
        val stamp = stampFile.get().asFile
        stamp.parentFile.mkdirs()
        stamp.writeText("OK")
    }
}

class DesignTokensPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("scaffold.config")
        val cfg = scaffoldConfig
        val pkgPath = cfg.packageId.replace('.', '/')

        val tokensJson = layout.projectDirectory.file("design-tokens/tokens.json")
        val ktOut = layout.projectDirectory.file(
            "androidApp/designsystem/src/main/kotlin/$pkgPath/designsystem/tokens/GeneratedTokens.kt",
        )
        val swiftOut = layout.projectDirectory.file(
            "iosApp/DesignSystem/Generated/GeneratedTokens.swift",
        )

        val generateTask = tasks.register<GenerateDesignTokensTask>("generateDesignTokens") {
            group = "design tokens"
            description = "Generates Compose and SwiftUI design tokens from design-tokens/tokens.json."
            tokensFile.set(tokensJson)
            packageId.set(cfg.packageId)
            kotlinOutputFile.set(ktOut)
            swiftOutputFile.set(swiftOut)
        }

        tasks.register<CheckDesignTokensTask>("checkDesignTokens") {
            group = "verification"
            description = "Verifies generated Compose and SwiftUI design tokens match design-tokens/tokens.json."
            mustRunAfter(generateTask)
            tokensFile.set(tokensJson)
            packageId.set(cfg.packageId)
            kotlinOutputFile.set(ktOut)
            swiftOutputFile.set(swiftOut)
            stampFile.set(layout.buildDirectory.file("reports/design-tokens/check.stamp"))
        }
    }
}
