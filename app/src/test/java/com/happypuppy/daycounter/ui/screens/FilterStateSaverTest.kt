package com.happypuppy.daycounter.ui.screens

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FilterStateSaver], which lets MainScreen keep a single
 * `rememberSaveable` [FilterState] instead of mirroring it through two saveable
 * primitives and a `LaunchedEffect` sync.
 */
class FilterStateSaverTest {

    // Bundle-backed saveable state accepts any primitive/String; mirror that here.
    private val scope = SaverScope { true }

    private fun roundTrip(state: FilterState): FilterState? {
        val saved = with(FilterStateSaver) { scope.save(state) }
        assertNotNull("saver must produce a value for $state", saved)
        return FilterStateSaver.restore(saved!!)
    }

    @Test
    fun `default state round-trips`() {
        assertEquals(FilterState(), roundTrip(FilterState()))
    }

    @Test
    fun `every non-category option round-trips`() {
        for (option in FilterOption.entries) {
            val state = FilterState(option = option)
            assertEquals(state, roundTrip(state))
        }
    }

    @Test
    fun `category filter keeps its category`() {
        val state = FilterState(option = FilterOption.CATEGORY, category = "Work")
        assertEquals(state, roundTrip(state))
    }

    @Test
    fun `saved form uses only Bundle-friendly primitives`() {
        val saved = with(FilterStateSaver) {
            scope.save(FilterState(option = FilterOption.CATEGORY, category = "Health"))
        }
        // mapSaver flattens to a [key, value, key, value] list under the hood.
        val flattened = saved as List<*>
        assertTrue(
            "option must be stored by name, not as an enum",
            flattened.all { it == null || it is String }
        )
        assertEquals(FilterOption.CATEGORY.name, flattened[flattened.indexOf("option") + 1])
        assertEquals("Health", flattened[flattened.indexOf("category") + 1])
    }
}
