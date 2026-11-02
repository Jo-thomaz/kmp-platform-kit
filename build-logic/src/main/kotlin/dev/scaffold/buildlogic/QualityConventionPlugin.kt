package dev.scaffold.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
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
    }
}
