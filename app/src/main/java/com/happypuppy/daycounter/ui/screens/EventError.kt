package com.happypuppy.daycounter.ui.screens

import com.happypuppy.daycounter.R
import com.happypuppy.daycounter.util.UiText

/** Why an [EventViewModel] operation failed. Carries no raw exception text. */
sealed interface EventError {
    val message: UiText

    data object LoadFailed : EventError {
        override val message = UiText.StringResource(R.string.main_error_load_failed)
    }

    data object SaveFailed : EventError {
        override val message = UiText.StringResource(R.string.main_error_save_failed)
    }

    data object UpdateFailed : EventError {
        override val message = UiText.StringResource(R.string.main_error_update_failed)
    }

    data object DeleteFailed : EventError {
        override val message = UiText.StringResource(R.string.main_error_delete_failed)
    }

    data object RestoreFailed : EventError {
        override val message = UiText.StringResource(R.string.main_error_restore_failed)
    }
}
