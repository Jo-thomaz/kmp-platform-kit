plugins {
    id("scaffold.android.compose")
}

tasks.configureEach {
    mustRunAfter(":generateDesignTokens")
}
