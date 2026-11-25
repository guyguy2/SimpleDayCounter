package com.happypuppy.daycounter.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies [FilterStateSaver] handles stale or corrupt saved state safely, e.g. after a release
 * renames or removes a [FilterOption]. Restoring is driven by the system bundle, so it must fall
 * back cleanly to [FilterState] defaults instead of throwing on process-death restore.
 */
class FilterStateSaverEdgeCasesTest {
    // mapSaver stores a flat [key, value, key, value, ...] list; see FilterStateSaverTest.

    @Test
    fun `restore with a missing category key yields a null category`() {
        val restored = FilterStateSaver.restore(listOf("option", FilterOption.PAST.name))
        assertEquals(FilterState(FilterOption.PAST, null), restored)
        assertNull(restored?.category)
    }

    @Test
    fun `restore with an unknown option name falls back safely to ALL`() {
        val restored = FilterStateSaver.restore(listOf("option", "RENAMED_OPTION", "category", "Work"))
        assertEquals(FilterState(FilterOption.ALL, null), restored)
    }

    @Test
    fun `restore with non-string or missing option falls back safely to ALL`() {
        assertEquals(
            FilterState(FilterOption.ALL, null),
            FilterStateSaver.restore(listOf("option", 42, "category", "Work"))
        )
        assertEquals(
            FilterState(FilterOption.ALL, null),
            FilterStateSaver.restore(emptyList<Any>())
        )
    }
}
