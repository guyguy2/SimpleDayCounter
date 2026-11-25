package com.happypuppy.daycounter.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.Category
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORIES
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.ui.theme.LocalDateFormat
import com.happypuppy.daycounter.ui.theme.SimpleDayCounterTheme
import com.happypuppy.daycounter.util.isSingleGrapheme
import com.happypuppy.daycounter.util.localDateToUtcPickerMillis
import com.happypuppy.daycounter.util.utcPickerMillisToLocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val MAX_EVENT_NAME_LENGTH = 100

/**
 * Curated emoji quick-pick entry paired with its accessibility label resource.
 */
data class EmojiSuggestion(
    val emoji: String,
    @StringRes val labelRes: Int
)

val defaultEmojiSuggestions = listOf(
    EmojiSuggestion("\uD83C\uDF82", R.string.form_emoji_birthday),
    EmojiSuggestion("\u2708\uFE0F", R.string.form_emoji_travel),
    EmojiSuggestion("\uD83C\uDF89", R.string.form_emoji_celebration),
    EmojiSuggestion("\uD83D\uDCBC", R.string.form_emoji_work),
    EmojiSuggestion("\uD83C\uDFC3", R.string.form_emoji_fitness),
    EmojiSuggestion("\u2764\uFE0F", R.string.form_emoji_love),
    EmojiSuggestion("\uD83C\uDF93", R.string.form_emoji_graduation),
    EmojiSuggestion("\uD83C\uDFE0", R.string.form_emoji_home)
)

/**
 * Quick relative target-date shortcut rendered below the date field.
 */
enum class QuickDatePreset(@StringRes val labelRes: Int) {
    TODAY(R.string.form_preset_today),
    TOMORROW(R.string.form_preset_tomorrow),
    NEXT_WEEK(R.string.form_preset_one_week),
    NEXT_MONTH(R.string.form_preset_one_month);

    /** Resolves this preset relative to [today]. */
    fun resolveDate(today: LocalDate): LocalDate = when (this) {
        TODAY -> today
        TOMORROW -> today.plusDays(1)
        NEXT_WEEK -> today.plusWeeks(1)
        NEXT_MONTH -> today.plusMonths(1)
    }
}

/**
 * Shared draft state and validation logic for both Add and Edit event forms.
 */
data class EventFormState(
    val name: String = "",
    val emoji: String = "",
    val targetDate: LocalDate = LocalDate.now(),
    val selectedCategoryId: String = DEFAULT_CATEGORY_ID
) {
    val trimmedName: String get() = name.trim()
    val isNameTooLong: Boolean get() = name.length > MAX_EVENT_NAME_LENGTH
    val isEmojiValid: Boolean get() = isSingleGrapheme(emoji)
    val canSave: Boolean get() = trimmedName.isNotEmpty() && !isNameTooLong && isEmojiValid

    fun withName(raw: String): EventFormState = copy(name = raw)

    fun withEmoji(raw: String): EventFormState =
        if (isSingleGrapheme(raw)) copy(emoji = raw) else this

    fun toggleEmojiSuggestion(emoji: String): EventFormState =
        if (this.emoji == emoji) copy(emoji = "") else withEmoji(emoji)

    fun withTargetDate(date: LocalDate): EventFormState = copy(targetDate = date)

    fun withCategoryId(categoryId: String): EventFormState = copy(selectedCategoryId = categoryId)

    companion object {
        fun fromItem(item: Item): EventFormState = EventFormState(
            name = item.name,
            emoji = item.emoji,
            targetDate = item.targetDate,
            selectedCategoryId = item.categoryId
        )

        fun defaultNew(clock: Clock = Clock.systemDefaultZone()): EventFormState = EventFormState(
            targetDate = LocalDate.now(clock)
        )
    }
}

/**
 * Bundle-friendly [Saver] for [EventFormState] with tolerant restore on incomplete or malformed
 * saved lists so process-death restoration falls back cleanly to defaults.
 */
val EventFormStateSaver: Saver<EventFormState, Any> = listSaver<EventFormState, Any>(
    save = { state ->
        listOf(
            state.name,
            state.emoji,
            state.targetDate.toString(),
            state.selectedCategoryId
        )
    },
    restore = { saved ->
        val defaults = EventFormState.defaultNew()
        val name = saved.getOrNull(0) as? String ?: defaults.name
        val emoji = (saved.getOrNull(1) as? String)
            ?.takeIf { isSingleGrapheme(it) }
            ?: defaults.emoji
        val targetDate = when (val rawDate = saved.getOrNull(2)) {
            is String -> runCatching { LocalDate.parse(rawDate) }.getOrNull()
            is Number -> Instant.ofEpochMilli(rawDate.toLong())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            else -> null
        } ?: defaults.targetDate
        val selectedCategoryId = (saved.getOrNull(3) as? String)
            ?.takeIf { it.isNotBlank() }
            ?: defaults.selectedCategoryId
        EventFormState(
            name = name,
            emoji = emoji,
            targetDate = targetDate,
            selectedCategoryId = selectedCategoryId
        )
    }
)

/**
 * Shared stateless form used by both Add and Edit event sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventForm(
    title: String,
    state: EventFormState,
    categories: List<Category>,
    onStateChange: (EventFormState) -> Unit,
    onAddCustomCategory: suspend (String) -> Category?,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    today: LocalDate = LocalDate.now()
) {
    val dateFormat = LocalDateFormat.current
    val scope = rememberCoroutineScope()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }

    // Opens the picker on a touch tap of the read-only date field.
    val dateInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(dateInteractionSource) {
        dateInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDatePicker = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { onStateChange(state.withName(it)) },
            label = { Text(stringResource(R.string.form_event_name)) },
            isError = state.isNameTooLong,
            supportingText = if (state.isNameTooLong) {
                { Text(stringResource(R.string.form_event_name_too_long)) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.emoji,
            onValueChange = { onStateChange(state.withEmoji(it)) },
            label = { Text(stringResource(R.string.form_optional_emoji)) },
            placeholder = { Text(stringResource(R.string.form_emoji_placeholder)) },
            trailingIcon = if (state.emoji.isNotEmpty()) {
                {
                    IconButton(onClick = { onStateChange(state.withEmoji("")) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.form_clear_emoji)
                        )
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            defaultEmojiSuggestions.forEach { suggestion ->
                val suggestionLabel = stringResource(suggestion.labelRes)
                MaterialFilterChip(
                    selected = state.emoji == suggestion.emoji,
                    onClick = { onStateChange(state.toggleEmojiSuggestion(suggestion.emoji)) },
                    label = { Text(suggestion.emoji) },
                    modifier = Modifier.semantics {
                        contentDescription = suggestionLabel
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val selectDateAction = stringResource(R.string.form_select_date_action)
        // Target date field: the read-only field itself is the single focus target and acts as a
        // button that opens the date picker (no overlay).
        OutlinedTextField(
            value = formatDate(state.targetDate, dateFormat),
            onValueChange = { /* readonly */ },
            label = { Text(stringResource(R.string.form_target_date)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    onClick(label = selectDateAction) {
                        showDatePicker = true
                        true
                    }
                },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.form_select_date)
                )
            },
            readOnly = true,
            interactionSource = dateInteractionSource
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickDatePreset.entries.forEach { preset ->
                val presetLabel = stringResource(preset.labelRes)
                SuggestionChip(
                    onClick = { onStateChange(state.withTargetDate(preset.resolveDate(today))) },
                    label = { Text(presetLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CategorySelector(
            selectedCategoryId = state.selectedCategoryId,
            categories = categories,
            onCategorySelected = { categoryId -> onStateChange(state.withCategoryId(categoryId)) },
            onAddCustomClick = { showAddCategoryDialog = true },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_delete))
                }

                Spacer(modifier = Modifier.weight(0.5f))

                Button(
                    onClick = onPrimaryAction,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(primaryActionLabel)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (onCancel != null) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onPrimaryAction,
                    enabled = state.canSave
                ) {
                    Text(primaryActionLabel)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToUtcPickerMillis(state.targetDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        onStateChange(state.withTargetDate(utcPickerMillisToLocalDate(selectedMillis)))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddCategoryDialog) {
        val customCategoryNames = remember(categories) {
            categories.filterNot { it.isDefault }.map { it.name }.toSet()
        }
        InlineAddCategoryDialog(
            customCategories = customCategoryNames,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { normalized ->
                scope.launch {
                    try {
                        val created = onAddCustomCategory(normalized)
                        if (created != null) {
                            onStateChange(state.withCategoryId(created.id))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                    showAddCategoryDialog = false
                }
            }
        )
    }
}

@Composable
private fun InlineAddCategoryDialog(
    customCategories: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var rawName by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val normalizedName = normalizeCategoryName(rawName)
    val isDuplicate = isDuplicateCategory(rawName, customCategories)

    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) {
                rawName = ""
                onDismiss()
            }
        },
        title = { Text(stringResource(R.string.category_inline_dialog_title)) },
        text = {
            OutlinedTextField(
                value = rawName,
                onValueChange = { rawName = it },
                label = { Text(stringResource(R.string.category_new_label)) },
                placeholder = { Text(stringResource(R.string.category_name_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                isError = isDuplicate,
                supportingText = if (isDuplicate) {
                    { Text(stringResource(R.string.category_exists)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = normalizeCategoryName(rawName)
                    if (normalized.isNotEmpty() && !isDuplicate && !isSubmitting) {
                        isSubmitting = true
                        rawName = ""
                        onConfirm(normalized)
                    }
                },
                enabled = normalizedName.isNotEmpty() && !isDuplicate && !isSubmitting
            ) {
                Text(stringResource(R.string.common_add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    rawName = ""
                    onDismiss()
                },
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun EventFormAddLightPreview() {
    val previewDate = LocalDate.of(2026, 6, 15)
    SimpleDayCounterTheme(darkTheme = false) {
        EventForm(
            title = stringResource(R.string.add_event_title),
            state = EventFormState(targetDate = previewDate),
            categories = DEFAULT_CATEGORIES,
            onStateChange = {},
            onAddCustomCategory = { null },
            primaryActionLabel = stringResource(R.string.add_event_save),
            onPrimaryAction = {},
            onCancel = {},
            today = previewDate
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventFormAddDarkPreview() {
    val previewDate = LocalDate.of(2026, 6, 15)
    SimpleDayCounterTheme(darkTheme = true) {
        EventForm(
            title = stringResource(R.string.add_event_title),
            state = EventFormState(targetDate = previewDate),
            categories = DEFAULT_CATEGORIES,
            onStateChange = {},
            onAddCustomCategory = { null },
            primaryActionLabel = stringResource(R.string.add_event_save),
            onPrimaryAction = {},
            onCancel = {},
            today = previewDate
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventFormEditLightPreview() {
    val previewDate = LocalDate.of(2026, 6, 15)
    SimpleDayCounterTheme(darkTheme = false) {
        EventForm(
            title = stringResource(R.string.edit_event_title),
            state = EventFormState(
                name = "Summer Vacation",
                emoji = "\u2708\uFE0F",
                targetDate = previewDate.plusWeeks(1),
                selectedCategoryId = DEFAULT_CATEGORY_ID
            ),
            categories = DEFAULT_CATEGORIES + Category(id = "travel", name = "Travel"),
            onStateChange = {},
            onAddCustomCategory = { null },
            primaryActionLabel = stringResource(R.string.edit_event_save),
            onPrimaryAction = {},
            onDelete = {},
            today = previewDate
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventFormEditDarkPreview() {
    val previewDate = LocalDate.of(2026, 6, 15)
    SimpleDayCounterTheme(darkTheme = true) {
        EventForm(
            title = stringResource(R.string.edit_event_title),
            state = EventFormState(
                name = "Summer Vacation",
                emoji = "\u2708\uFE0F",
                targetDate = previewDate.plusWeeks(1),
                selectedCategoryId = DEFAULT_CATEGORY_ID
            ),
            categories = DEFAULT_CATEGORIES + Category(id = "travel", name = "Travel"),
            onStateChange = {},
            onAddCustomCategory = { null },
            primaryActionLabel = stringResource(R.string.edit_event_save),
            onPrimaryAction = {},
            onDelete = {},
            today = previewDate
        )
    }
}
