package com.happypuppy.daycounter.ui.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.happypuppy.daycounter.BuildConfig

/**
 * A custom saver for bottom sheet state that preserves sheet visibility across configuration changes
 */
class BottomSheetStateSaver : Saver<MutableState<Boolean>, Boolean> {
    override fun restore(value: Boolean): MutableState<Boolean> = mutableStateOf(value)

    override fun SaverScope.save(value: MutableState<Boolean>): Boolean = value.value
}

/**
 * Helper function to update a sheet's state in a way that preserves it
 */
@Composable
fun rememberSaveableSheetState(key: String, initialValue: Boolean = false): Pair<Boolean, (Boolean) -> Unit> {
    val sheetVisible = rememberSaveable(saver = BottomSheetStateSaver()) { mutableStateOf(initialValue) }

    LaunchedEffect(key) {
        if (BuildConfig.DEBUG) Log.d("BottomSheet", "Sheet '$key' initialized with state: ${sheetVisible.value}")
    }

    val setSheetVisible: (Boolean) -> Unit = { newValue ->
        if (BuildConfig.DEBUG) Log.d("BottomSheet", "Sheet '$key' state changing from ${sheetVisible.value} to $newValue")
        sheetVisible.value = newValue
    }

    return Pair(sheetVisible.value, setSheetVisible)
}