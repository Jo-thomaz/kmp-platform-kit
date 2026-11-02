package dev.scaffold.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.of
import java.io.Serializable

data class ScaffoldConfig(
    val appName: String,
    val packageId: String,
    val bundleId: String,
    val versionName: String,
    val versionCode: Int,
    val minSdk: Int,
    val compileSdk: Int,
    val targetSdk: Int,
    val iosDeploymentTarget: String,
    val javaVersion: Int,
    val features: List<String>,
) : Serializable

abstract class ScaffoldConfigValueSource : ValueSource<ScaffoldConfig, ScaffoldConfigValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val configFile: RegularFileProperty
    }

    override fun obtain(): ScaffoldConfig {
        val file = parameters.configFile.get().asFile
        if (!file.exists()) {
            error("Missing scaffold.yaml at ${file.absolutePath}")
        }
        val scalars = mutableMapOf<String, String>()
        val features = mutableListOf<String>()
        var inFeaturesList = false

        file.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.substringBefore("#").trimEnd()
                if (line.isBlank()) continue
                val trimmed = line.trim()
                if (inFeaturesList && trimmed.startsWith("-")) {
                    features.add(trimmed.removePrefix("-").trim().removeSurrounding("\""))
                    continue
                } else if (!rawLine.startsWith(" ") && !rawLine.startsWith("\t")) {
                    inFeaturesList = false
                }
                val colonIndex = trimmed.indexOf(':')
                if (colonIndex > 0) {
                    val key = trimmed.substring(0, colonIndex).trim()
                    val value = trimmed.substring(colonIndex + 1).trim().removeSurrounding("\"")
                    if (key == "features" && value.isEmpty()) {
                        inFeaturesList = true
                    } else {
                        scalars[key] = value
                    }
                }
            }
        }

        return ScaffoldConfig(
            appName = scalars["appName"] ?: error("scaffold.yaml missing appName"),
            packageId = scalars["packageId"] ?: error("scaffold.yaml missing packageId"),
            bundleId = scalars["bundleId"] ?: error("scaffold.yaml missing bundleId"),
            versionName = scalars["versionName"] ?: "0.1.0",
            versionCode = scalars["versionCode"]?.toIntOrNull() ?: 1,
            minSdk = scalars["minSdk"]?.toIntOrNull() ?: 26,
            compileSdk = scalars["compileSdk"]?.toIntOrNull() ?: 36,
            targetSdk = scalars["targetSdk"]?.toIntOrNull() ?: 36,
            iosDeploymentTarget = scalars["iosDeploymentTarget"] ?: "18.0",
            javaVersion = scalars["javaVersion"]?.toIntOrNull() ?: 17,
            features = features.toList(),
        )
    }
}

class ScaffoldConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.scaffoldConfigProvider()
    }
}

fun Project.scaffoldConfigProvider(): Provider<ScaffoldConfig> =
    providers.of(ScaffoldConfigValueSource::class) {
        parameters.configFile.set(rootProject.layout.projectDirectory.file("scaffold.yaml"))
    }

val Project.scaffoldConfig: ScaffoldConfig
    get() = scaffoldConfigProvider().get()

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.moduleNamespace(suffix: String? = null): String {
    val base = scaffoldConfig.packageId
    val pathSuffix = suffix ?: path
        .removePrefix(":")
        .replace(':', '.')
        .replace('-', '_')
    return if (pathSuffix.isEmpty()) base else "$base.$pathSuffix"
}
