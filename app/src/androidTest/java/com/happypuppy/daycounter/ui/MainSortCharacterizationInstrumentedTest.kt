package com.happypuppy.daycounter.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.daycounter.MainActivity
import com.happypuppy.daycounter.data.database.AppDatabase
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.database.ItemRepository
import com.happypuppy.daycounter.data.preferences.AppTheme
import com.happypuppy.daycounter.data.preferences.DataStoreManager
import com.happypuppy.daycounter.ui.screens.FilterState
import com.happypuppy.daycounter.ui.screens.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Characterization tests for all 8 [com.happypuppy.daycounter.ui.screens.SortOption] entries
 * rendered through the real [MainActivity] / `MainScreen` UI (Koin + Room + DataStore).
 * Verifies the vertical `positionInRoot.y` order of rendered event cards before extracting
 * sort logic and menus out of `MainScreen`.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainSortCharacterizationInstrumentedTest {

    @get:Rule
    val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    private val koin get() = GlobalContext.get()

    @Before
    fun setUp() {
        resetState()
        rule.waitUntil(timeoutMillis = 15_000) {
            rule.onAllNodes(hasText("Add event"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        seedEvents()
    }

    @After
    fun tearDown() {
        resetState()
    }

    private fun resetState() = runBlocking(Dispatchers.IO) {
        koin.get<AppDatabase>().clearAllTables()
        val store = koin.get<DataStoreManager>()
        store.clearCustomCategories()
        store.setTheme(AppTheme.SYSTEM_DEFAULT)
        store.setDynamicColor(false)
        store.setSortOption(SortOption.DATE_ADDED_DESC)
        store.setFilterState(FilterState())
    }

    private fun noonIn(days: Long): Date =
        Date.from(LocalDate.now().plusDays(days).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())

    private fun seedEvents() {
        runBlocking(Dispatchers.IO) {
            val repository = koin.get<ItemRepository>()
            repository.insertItem(
                Item(
                    name = "Alpha",
                    itemCategory = "Work",
                    createdAt = Date(2_000L),
                    targetDate = noonIn(10)
                )
            )
            repository.insertItem(
                Item(
                    name = "Bravo",
                    itemCategory = "Finance",
                    createdAt = Date(1_000L),
                    targetDate = noonIn(30)
                )
            )
            repository.insertItem(
                Item(
                    name = "Charlie",
                    itemCategory = "Personal",
                    createdAt = Date(3_000L),
                    targetDate = noonIn(20)
                )
            )
        }
        rule.waitUntilExactlyOneExists(hasText("Alpha"), timeoutMillis = 5_000)
        rule.waitUntilExactlyOneExists(hasText("Bravo"), timeoutMillis = 5_000)
        rule.waitUntilExactlyOneExists(hasText("Charlie"), timeoutMillis = 5_000)
    }

    private fun selectSortAndVerifyOrder(menuLabel: String, expectedOrder: List<String>) {
        rule.onNodeWithContentDescription("Sort").performClick()
        rule.waitUntilExactlyOneExists(hasText(menuLabel), timeoutMillis = 5_000)
        rule.onNodeWithText(menuLabel).performClick()

        rule.waitUntil(timeoutMillis = 5_000) {
            val ys = expectedOrder.map { name ->
                rule.onNodeWithText(name).fetchSemanticsNode().positionInRoot.y
            }
            ys == ys.sorted() && ys.toSet().size == ys.size
        }

        val positions = expectedOrder.map { name ->
            name to rule.onNodeWithText(name).fetchSemanticsNode().positionInRoot.y
        }
        val ys = positions.map { it.second }
        assertEquals("Expected order $expectedOrder for $menuLabel, got $positions", ys.sorted(), ys)
        assertEquals("Expected distinct y positions for $menuLabel, got $positions", ys.size, ys.toSet().size)
    }

    @Test
    fun sortByNameAscOrdersCardsAlphabetically() {
        selectSortAndVerifyOrder("Name (A-Z)", listOf("Alpha", "Bravo", "Charlie"))
    }

    @Test
    fun sortByNameDescOrdersCardsReverseAlphabetically() {
        selectSortAndVerifyOrder("Name (Z-A)", listOf("Charlie", "Bravo", "Alpha"))
    }

    @Test
    fun sortByCategoryAscOrdersCardsByCategoryAlphabetically() {
        selectSortAndVerifyOrder("Category (A-Z)", listOf("Bravo", "Charlie", "Alpha"))
    }

    @Test
    fun sortByCategoryDescOrdersCardsByCategoryReverseAlphabetically() {
        selectSortAndVerifyOrder("Category (Z-A)", listOf("Alpha", "Charlie", "Bravo"))
    }

    @Test
    fun sortByTargetDateAscOrdersEarliestFirst() {
        selectSortAndVerifyOrder("Target Date (Earliest first)", listOf("Alpha", "Charlie", "Bravo"))
    }

    @Test
    fun sortByTargetDateDescOrdersLatestFirst() {
        selectSortAndVerifyOrder("Target Date (Latest first)", listOf("Bravo", "Charlie", "Alpha"))
    }

    @Test
    fun sortByDateAddedAscOrdersOldestFirst() {
        selectSortAndVerifyOrder("Date Added (Oldest first)", listOf("Bravo", "Alpha", "Charlie"))
    }

    @Test
    fun sortByDateAddedDescOrdersNewestFirst() {
        selectSortAndVerifyOrder("Date Added (Newest first)", listOf("Charlie", "Alpha", "Bravo"))
    }
}
