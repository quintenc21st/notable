package com.ethran.notable.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ethran.notable.ui.theme.NotableStyle

/**
 * Flat, rounded button — no theme grays, e-ink friendly. [inverted] fills it with the accent
 * color, for the main action or the selected option.
 */
@Composable
fun EInkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = NotableStyle.ControlShape,
        border = BorderStroke(1.5.dp, if (inverted) NotableStyle.Accent else Color.Black),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (inverted) NotableStyle.Accent else Color.White,
            contentColor = if (inverted) NotableStyle.OnAccent else Color.Black,
        ),
        content = content,
    )
}
