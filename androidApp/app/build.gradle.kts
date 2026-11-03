plugins {
    id("scaffold.android.app")
}

dependencies {
    implementation(projects.androidApp.designsystem)
    implementation(projects.androidApp.feature.reference)
}
