package com.happypuppy.daycounter.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowSizeClass

val ADAPTIVE_CONTENT_MAX_WIDTH = 840.dp
val ADAPTIVE_GRID_MIN_CELL_WIDTH = 320.dp
val ADAPTIVE_EVENT_FORM_DIALOG_MAX_WIDTH = 560.dp

const val ADAPTIVE_EVENT_FORM_DIALOG_TAG = "adaptive_event_form_dialog"
const val ADAPTIVE_EVENT_FORM_SHEET_TAG = "adaptive_event_form_sheet"

/**
 * Window-size-driven layout decisions for Simple Day Counter.
 *
 * - Compact width (< 600.dp): single-column list with horizontal swipe-to-delete enabled.
 * - Medium / Expanded width (>= 600.dp): centered multi-column grid capped at
 *   [ADAPTIVE_CONTENT_MAX_WIDTH] with horizontal swipe disabled on grid cells (edit sheet Delete,
 *   hardware Delete key, and TalkBack custom Delete action remain available).
 * - Event form container: centered [Dialog] capped at [ADAPTIVE_EVENT_FORM_DIALOG_MAX_WIDTH] when
 *   width >= 600.dp or height < 480.dp; [ModalBottomSheet] on compact portrait windows.
 */
@Immutable
data class AdaptiveLayoutSpec(
    val useGrid: Boolean,
    val enableSwipeToDelete: Boolean,
    val useDialogForEventForm: Boolean
)

/** Pure mapping from Material 3 [WindowSizeClass] to [AdaptiveLayoutSpec] for JVM unit testing. */
fun adaptiveLayoutSpecFor(windowSizeClass: WindowSizeClass): AdaptiveLayoutSpec {
    val isMediumOrWider = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val isCompactHeight = !windowSizeClass.isHeightAtLeastBreakpoint(
        WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
    )
    return AdaptiveLayoutSpec(
        useGrid = isMediumOrWider,
        enableSwipeToDelete = !isMediumOrWider,
        useDialogForEventForm = isMediumOrWider || isCompactHeight
    )
}

@Composable
fun rememberAdaptiveLayoutSpec(): AdaptiveLayoutSpec {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return adaptiveLayoutSpecFor(windowSizeClass)
}

/**
 * Presents the Add / Edit Event form in either a centered [Dialog] (wide or short-height windows)
 * or a [ModalBottomSheet] (compact portrait windows), dismissing cleanly on hardware Escape
 * without intercepting text-editing keys inside form fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveEventFormContainer(
    useDialog: Boolean,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val escapeDismissModifier = Modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            onDismissRequest()
            true
        } else {
            false
        }
    }

    if (useDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .widthIn(max = ADAPTIVE_EVENT_FORM_DIALOG_MAX_WIDTH)
                    .fillMaxWidth()
                    .testTag(ADAPTIVE_EVENT_FORM_DIALOG_TAG)
                    .then(escapeDismissModifier),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                content()
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ADAPTIVE_EVENT_FORM_SHEET_TAG)
                    .then(escapeDismissModifier)
            ) {
                content()
            }
        }
    }
}
