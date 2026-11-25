package com.happypuppy.daycounter.ui.components

import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryNamesTest {

    @Test
    fun `every default category has its own display name resource`() {
        val ids = DEFAULT_CATEGORIES.map { defaultCategoryNameRes(it.id) }
        assertEquals(DEFAULT_CATEGORIES.size, ids.filterNotNull().toSet().size)
    }

    @Test
    fun `default categories map by their stable id`() {
        assertEquals(R.string.category_personal, defaultCategoryNameRes(DEFAULT_CATEGORY_ID))
        assertEquals(R.string.category_work, defaultCategoryNameRes(WORK_CATEGORY_ID))
        assertEquals(R.string.category_health, defaultCategoryNameRes(HEALTH_CATEGORY_ID))
        assertEquals(R.string.category_finance, defaultCategoryNameRes(FINANCE_CATEGORY_ID))
    }

    @Test
    fun `custom and unknown category ids have no resource`() {
        assertNull(defaultCategoryNameRes("custom-uuid-123"))
        assertNull(defaultCategoryNameRes("Personal"))
        assertNull(defaultCategoryNameRes(""))
    }
}
