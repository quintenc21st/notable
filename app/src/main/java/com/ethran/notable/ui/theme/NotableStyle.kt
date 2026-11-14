package com.ethran.notable.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared look of this fork's UI: flat, rounded, high-contrast surfaces with one accent color.
 *
 * Tuned for color e-ink (Kaleido): no shadows, gradients or light greys, which ghost or
 * dither badly. The accent is a deep, saturated blue so it still reads as "selected" where
 * the panel shows color only faintly, and keeps white text legible on monochrome screens.
 */
object NotableStyle {
    val Accent = Color(0xFF1565C0)
    val OnAccent = Color.White

    /** Menus, popups and dialogs. */
    val PanelShape = RoundedCornerShape(14.dp)

    /** Buttons and other tappable controls. */
    val ControlShape = RoundedCornerShape(10.dp)

    /** Small things inside controls: toolbar buttons, thumbnails. */
    val SmallShape = RoundedCornerShape(8.dp)
}

/** White rounded surface with a bold outline — the standard floating panel. */
fun Modifier.notablePanel(shape: Shape = NotableStyle.PanelShape): Modifier =
    this
        .clip(shape)
        .background(Color.White)
        .border(2.dp, Color.Black, shape)
