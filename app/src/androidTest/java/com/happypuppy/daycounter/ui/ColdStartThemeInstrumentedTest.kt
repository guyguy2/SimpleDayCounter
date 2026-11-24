package com.happypuppy.daycounter.ui

import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.happypuppy.daycounter.MainActivity
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import kotlin.math.abs

/**
 * UX-3: the app's first screen uses the stored theme, whatever the system mode is. Each test
 * launches a fresh MainActivity for one cell of the system (light/dark) x app (system/light/dark)
 * matrix and checks the pixels on screen. The theme is stored before launch, so the splash
 * keep condition has to hand over the resolved value, not the placeholder.
 *
 * Changes the emulator's night mode (`cmd uimode night`) and resets it to "no" afterwards.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ColdStartThemeInstrumentedTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store get() = GlobalContext.get().get<DataStoreManager>()

    @After
    fun resetGlobalState() {
        shell("cmd uimode night no")
        runBlocking(Dispatchers.IO) { store.setTheme(AppTheme.SYSTEM_DEFAULT) }
    }

    private fun shell(command: String) {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private fun launchAndCheck(systemDark: Boolean, theme: AppTheme, expectedBackground: Int) {
        shell("cmd uimode night ${if (systemDark) "yes" else "no"}")
        runBlocking(Dispatchers.IO) { store.setTheme(theme) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { }
            rule.waitUntil(timeoutMillis = 15_000) {
                var night = false
                scenario.onActivity {
                    night = it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES
                }
                night == systemDark &&
                    rule.onAllNodes(hasText("Add event"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            rule.waitForIdle()

            // The system splash can still be fading out when the content exists, and in the mixed
            // cells it has the system's color, so wait (bounded) for the screen to settle.
            var pixel = screenPixel()
            val deadline = System.currentTimeMillis() + 5_000
            while (!near(pixel, expectedBackground) && System.currentTimeMillis() < deadline) {
                Thread.sleep(100)
                pixel = screenPixel()
            }
            assertTrue(
                "system dark=$systemDark app=$theme: expected ${hex(expectedBackground)} but was ${hex(pixel)}",
                near(pixel, expectedBackground)
            )
        }
    }

    /** Left edge, mid height: plain screen background, clear of cards and the empty-state text. */
    private fun screenPixel(): Int {
        val shot = instrumentation.uiAutomation.takeScreenshot()
        return shot.getPixel(2, (shot.height * 0.45).toInt())
    }

    private fun hex(c: Int) = "#%06X".format(c and 0xFFFFFF)

    private fun near(a: Int, b: Int): Boolean =
        listOf(16, 8, 0).all { shift -> abs(((a shr shift) and 0xFF) - ((b shr shift) and 0xFF)) <= 3 }

    private companion object {
        const val LIGHT_BACKGROUND = 0xFFFEF7FF.toInt()
        const val DARK_BACKGROUND = 0xFF121212.toInt()
    }

    @Test
    fun systemLightAppSystemDefaultIsLight() =
        launchAndCheck(systemDark = false, theme = AppTheme.SYSTEM_DEFAULT, expectedBackground = LIGHT_BACKGROUND)

    @Test
    fun systemDarkAppSystemDefaultIsDark() =
        launchAndCheck(systemDark = true, theme = AppTheme.SYSTEM_DEFAULT, expectedBackground = DARK_BACKGROUND)

    @Test
    fun systemLightAppLightIsLight() =
        launchAndCheck(systemDark = false, theme = AppTheme.LIGHT, expectedBackground = LIGHT_BACKGROUND)

    @Test
    fun systemDarkAppLightIsLight() =
        launchAndCheck(systemDark = true, theme = AppTheme.LIGHT, expectedBackground = LIGHT_BACKGROUND)

    @Test
    fun systemLightAppDarkIsDark() =
        launchAndCheck(systemDark = false, theme = AppTheme.DARK, expectedBackground = DARK_BACKGROUND)

    @Test
    fun systemDarkAppDarkIsDark() =
        launchAndCheck(systemDark = true, theme = AppTheme.DARK, expectedBackground = DARK_BACKGROUND)
}
