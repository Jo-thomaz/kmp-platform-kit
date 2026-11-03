package dev.scaffold.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("scaffold.config")
        // AGP 9 provides built-in Kotlin; do not apply org.jetbrains.kotlin.android.
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("scaffold.quality")

        val cfg = scaffoldConfig

        extensions.configure<ApplicationExtension> {
            namespace = "${cfg.packageId}.app"
            compileSdk = cfg.compileSdk

            defaultConfig {
                applicationId = cfg.packageId
                minSdk = cfg.minSdk
                targetSdk = cfg.targetSdk
                versionCode = cfg.versionCode
                versionName = cfg.versionName
                manifestPlaceholders["appName"] = cfg.appName
            }

            buildFeatures {
                compose = true
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(cfg.javaVersion)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        dependencies {
            val bom = platform(libs.findLibrary("androidx-compose-bom").get())
            add("implementation", bom)
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-material3").get())
            add("implementation", libs.findLibrary("androidx-activity-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
        }
    }
}
