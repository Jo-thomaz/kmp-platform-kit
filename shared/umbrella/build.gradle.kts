import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("scaffold.kmp.library")
    alias(libs.plugins.skie)
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "SharedUmbrella"
            isStatic = true
            export(projects.shared.core)
            export(projects.shared.domain)
            export(projects.shared.feature.reference)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core)
            api(projects.shared.domain)
            api(projects.shared.data)
            api(projects.shared.feature.reference)
        }
    }
}
