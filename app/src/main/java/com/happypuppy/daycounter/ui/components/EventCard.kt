package com.happypuppy.daycounter.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.daycounter.data.database.Item
import com.happypuppy.daycounter.data.preferences.formatDate
import com.happypuppy.daycounter.ui.theme.CategoryColors
import com.happypuppy.daycounter.ui.theme.DarkCardPink
import com.happypuppy.daycounter.ui.theme.DarkCardPurple
import com.happypuppy.daycounter.ui.theme.DarkCardYellow
import com.happypuppy.daycounter.ui.theme.DarkOnSurface
import com.happypuppy.daycounter.ui.theme.DarkOnSurfaceVariant
import com.happypuppy.daycounter.ui.theme.LocalDateFormat
import com.happypuppy.daycounter.ui.theme.LocalIsDarkTheme
import com.happypuppy.daycounter.util.DayLabel
import com.happypuppy.daycounter.util.calculateDaysLeft
import com.happypuppy.daycounter.util.dayLabelFor
import com.happypuppy.daycounter.util.isColorBright

@Composable
fun EventCard(
    item: Item,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val daysLeft = calculateDaysLeft(item.targetDate)

    // Theme + date format are resolved once at SimpleDayCounterTheme; consume them here instead
    // of re-collecting the DataStore flows per card (IMPROVEMENTS.md P1-5 / P1-1).
    val isDarkTheme = LocalIsDarkTheme.current
    val dateFormat = LocalDateFormat.current

    // Calculate the appropriate background color based on days left and theme
    val backgroundColor = if (isDarkTheme) {
        when {
            daysLeft < 0 -> DarkCardPink    // Past date
            daysLeft == 0 -> DarkCardPurple // Today
            else -> DarkCardYellow  // Future date
        }
    } else {
        when {
            daysLeft < 0 -> Color(0xFFFFF0F6) // Light Pink
            daysLeft == 0 -> Color(0xFFF9F0FF) // Light Purple
            else -> Color(0xFFFFFBE6) // Light Yellow
        }
    }

    // Calculate text colors based on theme
    val primaryTextColor = if (isDarkTheme) DarkOnSurface else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) DarkOnSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant

    // Get category color
    val categoryColor = CategoryColors.getCategoryColor(item.itemCategory, isDarkTheme)

    // Determine icon based on category
    val categoryIcon = when (item.itemCategory) {
        "Personal" -> Icons.Default.Person
        "Work" -> Icons.Default.Work
        "Health" -> Icons.Default.HealthAndSafety
        "Finance" -> Icons.Default.Payments
        else -> Icons.Default.Event // Default icon for custom categories
    }

    // Swipe is not available to every user, so expose Delete as an accessibility action too.
    // Edit is the card's labelled click ("Edit event"), so it is not repeated as a custom action.
    val accessibilityActions = buildList {
        if (onDelete != null) add(CustomAccessibilityAction("Delete") { onDelete(); true })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Edit event") { onClick() }
            .semantics { customActions = accessibilityActions },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = primaryTextColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(
            1.dp, if (isDarkTheme)
                Color.DarkGray.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left icon without circle - just show the emoji or icon
            if (item.emoji.isNotBlank()) {
                // Show custom emoji if set by user
                Text(
                    text = item.emoji,
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
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                Spacer(modifier = Modifier.height(8.dp)) // Additional spacing

                // Date with the selected format
                val formattedDate = formatDate(item.targetDate, dateFormat)

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )

                // Category
                if (item.itemCategory.isNotBlank()) {
                    Text(
                        text = item.itemCategory,
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
                        text = kotlin.math.abs(daysLeft).toString(),
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (val label = dayLabelFor(daysLeft)) {
                        DayLabel.Today -> "Today!"
                        is DayLabel.Left -> if (label.days == 1) "day left" else "days left"
                        is DayLabel.Past -> if (label.days == 1) "day past" else "days past"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}