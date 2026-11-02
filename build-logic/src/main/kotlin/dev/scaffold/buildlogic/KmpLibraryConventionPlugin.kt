package dev.scaffold.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("scaffold.config")
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("scaffold.quality")

        val cfg = scaffoldConfig

        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(cfg.javaVersion)

            extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("androidLibrary") {
                namespace = moduleNamespace()
                compileSdk = cfg.compileSdk
                minSdk = cfg.minSdk
                withHostTestBuilder {}
            }

            jvm()
            iosArm64()
            iosSimulatorArm64()

            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }

            sourceSets.named("commonTest") {
                dependencies {
                    implementation(libs.findLibrary("kotlin-test").get())
                    implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                }
            }
        }

        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
            options.release.set(17)
        }
    }
}
