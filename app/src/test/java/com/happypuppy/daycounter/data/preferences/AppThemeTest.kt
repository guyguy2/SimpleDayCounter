package com.happypuppy.daycounter.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which theme the app renders for each app setting and system mode (UX-3 cold-start matrix). */
class AppThemeTest {

    @Test
    fun `system default follows the device`() {
        assertEquals(false, AppTheme.SYSTEM_DEFAULT.isDark(systemDark = false))
        assertEquals(true, AppTheme.SYSTEM_DEFAULT.isDark(systemDark = true))
    }

    @Test
    fun `light is light in both system modes`() {
        assertEquals(false, AppTheme.LIGHT.isDark(systemDark = false))
        assertEquals(false, AppTheme.LIGHT.isDark(systemDark = true))
    }

    @Test
    fun `dark is dark in both system modes`() {
        assertEquals(true, AppTheme.DARK.isDark(systemDark = false))
        assertEquals(true, AppTheme.DARK.isDark(systemDark = true))
    }
}
