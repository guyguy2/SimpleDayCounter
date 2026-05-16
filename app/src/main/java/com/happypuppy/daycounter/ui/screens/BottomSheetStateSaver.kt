package com.happypuppy.daycounter.ui.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A custom saver for bottom sheet state that preserves sheet visibility across configuration changes
 */
class BottomSheetStateSaver : Saver<MutableState<Boolean>, Boolean> {
    override fun restore(value: Boolean): MutableState<Boolean> = mutableStateOf(value)

    override fun SaverScope.save(value: MutableState<Boolean>): Boolean = value.value
}

/**
 * Extension function to remember sheet state that survives configuration changes
 * @param key - Unique identifier for this state
 * @param initialValue - Initial visibility of sheet
 */
@Composable
fun rememberSheetState(key: String, initialValue: Boolean = false): Boolean {
    // Log the state for debugging
    val sheetVisible = rememberSaveable(saver = BottomSheetStateSaver()) { mutableStateOf(initialValue) }

    // Log state changes and lifecycle events
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("BottomSheet", "Sheet '$key' state on resume: ${sheetVisible.value}")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("BottomSheet", "Sheet '$key' state on pause: ${sheetVisible.value}")
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return sheetVisible.value
}

/**
 * Helper function to update a sheet's state in a way that preserves it
 */
@Composable
fun rememberSaveableSheetState(key: String, initialValue: Boolean = false): Pair<Boolean, (Boolean) -> Unit> {
    val sheetVisible = rememberSaveable(saver = BottomSheetStateSaver()) { mutableStateOf(initialValue) }

    LaunchedEffect(key) {
        Log.d("BottomSheet", "Sheet '$key' initialized with state: ${sheetVisible.value}")
    }

    val setSheetVisible: (Boolean) -> Unit = { newValue ->
        Log.d("BottomSheet", "Sheet '$key' state changing from ${sheetVisible.value} to $newValue")
        sheetVisible.value = newValue
    }

    return Pair(sheetVisible.value, setSheetVisible)
}