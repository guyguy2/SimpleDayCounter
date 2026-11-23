package com.happypuppy.daycounter.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * Characterizes how [FilterStateSaver] reacts to stale or corrupt saved state, e.g. after a release
 * renames or removes a [FilterOption]. Restoring is driven by the system bundle, so a throw here
 * would crash the Activity on process-death restore.
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
    fun `restore with an unknown option name currently throws IllegalArgumentException`() {
        try {
            FilterStateSaver.restore(listOf("option", "RENAMED_OPTION", "category", null))
            fail("expected IllegalArgumentException; if restore became tolerant, update this test")
        } catch (_: IllegalArgumentException) {
            // Known weakness: a renamed enum would crash on restore. See PLAN.md (code quality).
        }
    }
}
