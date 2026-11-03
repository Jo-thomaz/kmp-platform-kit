plugins {
    id("scaffold.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core)
        }
    }
}
