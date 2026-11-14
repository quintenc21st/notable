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

    /**
     * Spine colors for notebook covers. Deep, saturated hues: they stay distinguishable on a
     * color e-ink panel and all read as "dark" on a monochrome one.
     */
    private val NotebookColors = listOf(
        Color(0xFF1565C0), // blue
        Color(0xFFC62828), // red
        Color(0xFF2E7D32), // green
        Color(0xFFEF6C00), // orange
        Color(0xFF6A1B9A), // purple
        Color(0xFF00838F), // teal
    )

    /** A stable color for the notebook with this id, so each one keeps its own spine. */
    fun notebookColor(notebookId: String): Color =
        NotebookColors[(notebookId.hashCode() and 0x7fffffff) % NotebookColors.size]
}

/** White rounded surface with a bold outline — the standard floating panel. */
fun Modifier.notablePanel(shape: Shape = NotableStyle.PanelShape): Modifier =
    this
        .clip(shape)
        .background(Color.White)
        .border(2.dp, Color.Black, shape)
