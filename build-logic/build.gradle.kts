import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "dev.scaffold.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("scaffoldConfig") {
            id = "scaffold.config"
            implementationClass = "dev.scaffold.buildlogic.ScaffoldConfigPlugin"
        }
        register("kmpLibrary") {
            id = "scaffold.kmp.library"
            implementationClass = "dev.scaffold.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "scaffold.kmp.feature"
            implementationClass = "dev.scaffold.buildlogic.KmpFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "scaffold.android.compose"
            implementationClass = "dev.scaffold.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidApp") {
            id = "scaffold.android.app"
            implementationClass = "dev.scaffold.buildlogic.AndroidAppConventionPlugin"
        }
        register("quality") {
            id = "scaffold.quality"
            implementationClass = "dev.scaffold.buildlogic.QualityConventionPlugin"
        }
        register("designTokens") {
            id = "scaffold.tokens"
            implementationClass = "dev.scaffold.buildlogic.DesignTokensPlugin"
        }
    }
}
