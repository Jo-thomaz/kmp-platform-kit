import dev.scaffold.buildlogic.scaffoldConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("scaffold.config")
    alias(libs.plugins.kotlin.jvm)
    id("scaffold.quality")
}

kotlin {
    jvmToolchain(scaffoldConfig.javaVersion)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.konsist)
}

tasks.test {
    inputs.files(
        rootProject.files(
            "shared/core/src",
            "shared/domain/src",
            "shared/data/src",
            "shared/feature/reference/src",
            "shared/umbrella/src",
        ),
    )
}
