package com.happypuppy.daycounter.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.ui.screens.EventUi
import com.happypuppy.daycounter.util.vibrate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(
    event: EventUi,
    isVibrationEnabled: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var deleteDispatched by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.5f }
    )

    // Shared by the swipe gesture and the card's "Delete" accessibility action.
    val deleteOnce = {
        if (!deleteDispatched) {
            deleteDispatched = true
            if (isVibrationEnabled) {
                vibrate(context)
            }
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                deleteOnce()
            }
        },
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                },
                label = "SwipeDeleteBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    ) {
        EventCard(event = event, onClick = onClick, onDelete = deleteOnce)
    }
}
