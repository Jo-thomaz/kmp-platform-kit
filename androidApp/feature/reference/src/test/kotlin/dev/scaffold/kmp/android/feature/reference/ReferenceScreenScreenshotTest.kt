package dev.scaffold.kmp.android.feature.reference

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.scaffold.kmp.core.DomainError
import dev.scaffold.kmp.core.UuidV7
import dev.scaffold.kmp.designsystem.ScaffoldTheme
import dev.scaffold.kmp.domain.model.ReferenceItem
import dev.scaffold.kmp.feature.reference.ReferenceUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "w400dp-h800dp-normal-long-notround-any-420dpi-keyshidden-nonav",
)
class ReferenceScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sampleItems = listOf(
        ReferenceItem(
            id = UuidV7.generate(timestampMillis = 1_762_000_000_001L),
            title = "Offline-first architecture",
            summary = "Shared presenter state rendered natively in Compose and SwiftUI",
        ),
        ReferenceItem(
            id = UuidV7.generate(timestampMillis = 1_762_000_000_002L),
            title = "Deterministic design tokens",
            summary = "Single W3C DTCG source generating Compose and SwiftUI themes",
        ),
    )

    private fun captureState(
        name: String,
        darkTheme: Boolean,
        state: ReferenceUiState,
    ) {
        composeRule.setContent {
            ScaffoldTheme(darkTheme = darkTheme) {
                ReferenceScreen(
                    state = state,
                    onAction = {},
                )
            }
        }
        val modeSuffix = if (darkTheme) "dark" else "light"
        composeRule.onRoot().captureRoboImage("${name}_${modeSuffix}.png")
    }

    @Test
    fun captureLoadingLight() = captureState(
        name = "Loading",
        darkTheme = false,
        state = ReferenceUiState.Loading,
    )

    @Test
    fun captureLoadingDark() = captureState(
        name = "Loading",
        darkTheme = true,
        state = ReferenceUiState.Loading,
    )

    @Test
    fun captureContentLight() = captureState(
        name = "Content",
        darkTheme = false,
        state = ReferenceUiState.Content(items = sampleItems),
    )

    @Test
    fun captureContentDark() = captureState(
        name = "Content",
        darkTheme = true,
        state = ReferenceUiState.Content(items = sampleItems),
    )

    @Test
    fun captureEmptyLight() = captureState(
        name = "Empty",
        darkTheme = false,
        state = ReferenceUiState.Empty,
    )

    @Test
    fun captureEmptyDark() = captureState(
        name = "Empty",
        darkTheme = true,
        state = ReferenceUiState.Empty,
    )

    @Test
    fun captureErrorLight() = captureState(
        name = "Error",
        darkTheme = false,
        state = ReferenceUiState.Error(
            error = DomainError.Network(message = "Network connection unavailable", statusCode = 503),
            retryable = true,
        ),
    )

    @Test
    fun captureErrorDark() = captureState(
        name = "Error",
        darkTheme = true,
        state = ReferenceUiState.Error(
            error = DomainError.Network(message = "Network connection unavailable", statusCode = 503),
            retryable = true,
        ),
    )
}
