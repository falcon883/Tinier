package com.durvank883.tinier.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput

object Helper {
    fun Context.getActivity(): ComponentActivity? = when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }

    fun Modifier.gesturesDisabled(disabled: Boolean = true) =
        if (disabled) {
            pointerInput(Unit) {
                awaitPointerEventScope {
                    // we should wait for all new pointer events
                    while (true) {
                        awaitPointerEvent(pass = PointerEventPass.Initial)
                            .changes
                            .forEach(PointerInputChange::consumeAllChanges)
                    }
                }
            }
        } else {
            Modifier
        }
}