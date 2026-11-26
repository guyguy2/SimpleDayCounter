package com.happypuppy.daycounter.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.data.database.DEFAULT_CATEGORY_ID
import com.happypuppy.daycounter.data.database.FINANCE_CATEGORY_ID
import com.happypuppy.daycounter.data.database.HEALTH_CATEGORY_ID
import com.happypuppy.daycounter.data.database.WORK_CATEGORY_ID
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.ui.screens.EventUi
import com.happypuppy.daycounter.ui.theme.CategoryColors
import com.happypuppy.daycounter.ui.theme.LocalDateFormat
import com.happypuppy.daycounter.ui.theme.LocalEventCardColors
import com.happypuppy.daycounter.ui.theme.LocalIsDarkTheme
import com.happypuppy.daycounter.util.DayLabel
import com.happypuppy.daycounter.util.isColorBright

@Composable
fun EventCard(
    event: EventUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val daysLeft = event.daysLeft

    // Theme + date format are resolved once at SimpleDayCounterTheme; consume them here instead
    // of re-collecting the DataStore flows per card (IMPROVEMENTS.md P1-5 / P1-1).
    val isDarkTheme = LocalIsDarkTheme.current
    val dateFormat = LocalDateFormat.current

    // Container and text colors for this card's state come from the theme (brand tints, or dynamic
    // color roles when that is on).
    val stateColors = LocalEventCardColors.current.forDaysLeft(daysLeft)
    val backgroundColor = stateColors.container
    val primaryTextColor = stateColors.content
    val secondaryTextColor = stateColors.contentVariant

    // Get category color
    val categoryColor = CategoryColors.getCategoryColor(
        categoryId = event.categoryId,
        categoryName = event.categoryName,
        isDarkTheme = isDarkTheme
    )

    // Determine icon based on stable category ID
    val categoryIcon = when (event.categoryId) {
        DEFAULT_CATEGORY_ID -> Icons.Default.Person
        WORK_CATEGORY_ID -> Icons.Default.Work
        HEALTH_CATEGORY_ID -> Icons.Default.HealthAndSafety
        FINANCE_CATEGORY_ID -> Icons.Default.Payments
        else -> Icons.Default.Event // Default icon for custom categories
    }

    // Swipe is not available to every user, so expose Delete as an accessibility action too.
    // Edit is the card's labelled click ("Edit event"), so it is not repeated as a custom action.
    val deleteLabel = stringResource(R.string.common_delete)
    val editLabel = stringResource(R.string.event_card_edit)
    val accessibilityActions = buildList {
        if (onDelete != null) add(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val formattedDate = formatDate(event.targetDate, dateFormat)
    val yearsText = if (event.years > 0) {
        pluralStringResource(R.plurals.event_card_years, event.years, event.years)
    } else {
        null
    }
    val categoryDisplay = if (event.categoryName.isNotBlank()) {
        categoryDisplayName(event.categoryId, event.categoryName)
    } else {
        ""
    }
    val dayCountText = kotlin.math.abs(daysLeft).toString()
    val dayLabelText = when (val label = event.dayLabel) {
        DayLabel.Today -> stringResource(R.string.event_card_today)
        is DayLabel.Left -> pluralStringResource(R.plurals.event_card_days_left, label.days)
        is DayLabel.Past -> pluralStringResource(R.plurals.event_card_days_past, label.days)
    }
    val repeatsYearlyLabel = stringResource(R.string.event_card_repeats_yearly)
    val cardContentDescription = if (event.repeatsYearly) {
        listOfNotNull(
            event.name,
            formattedDate,
            yearsText,
            repeatsYearlyLabel,
            categoryDisplay.takeIf { it.isNotBlank() },
            dayCountText,
            dayLabelText
        ).joinToString(", ")
    } else {
        null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onKeyEvent { keyEvent ->
                if (onDelete != null &&
                    keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.nativeKeyEvent.repeatCount == 0 &&
                    (keyEvent.key == Key.Delete ||
                        keyEvent.key == Key.Backspace ||
                        keyEvent.key == Key.NumPadDelete)
                ) {
                    onDelete()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = editLabel,
                onClick = onClick
            )
            .semantics {
                customActions = accessibilityActions
                if (cardContentDescription != null) {
                    contentDescription = cardContentDescription
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = primaryTextColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left icon without circle - just show the emoji or icon
            if (event.emoji.isNotBlank()) {
                // Show custom emoji if set by user
                Text(
                    text = event.emoji,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(end = 4.dp)
                )
            } else {
                // Show default category icon (decorative; category name is shown in text)
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Center content with more spacing
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Event name at the top with larger spacing
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                Spacer(modifier = Modifier.height(8.dp)) // Additional spacing

                // Date with the selected format
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )
                    if (event.repeatsYearly) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = secondaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Years line when repeating from a past date
                if (yearsText != null) {
                    Text(
                        text = yearsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }

                // Category
                if (categoryDisplay.isNotBlank()) {
                    Text(
                        text = categoryDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
            }

            // Right circle with day count (same as original)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 80.dp, minHeight = 80.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Determine text color based on category color brightness
                    val textColor = if (isColorBright(categoryColor)) Color.Black else Color.White

                    Text(
                        text = dayCountText,
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dayLabelText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}