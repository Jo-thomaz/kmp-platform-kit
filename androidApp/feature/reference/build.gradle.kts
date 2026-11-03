plugins {
    id("scaffold.android.compose")
    alias(libs.plugins.roborazzi)
}

roborazzi {
    outputDir.set(rootProject.layout.projectDirectory.dir("docs/parity/reference/android"))
}

dependencies {
    api(projects.shared.core)
    api(projects.shared.domain)
    api(projects.shared.feature.reference)
    implementation(projects.shared.umbrella)
    implementation(projects.androidApp.designsystem)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
