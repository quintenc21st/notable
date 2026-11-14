package com.ethran.notable.editor.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethran.notable.ui.noRippleClickable
import com.ethran.notable.ui.theme.NotableStyle

/**
 * A toolbar button.
 *
 * - Plain tools are a bare icon; the selected one sits on a rounded accent tile.
 * - Pens ([penColor] set) are a dot of their ink color; the selected pen becomes a rounded
 *   tile of that color with a black outline, so it stands out even when the ink is black.
 */
@Composable
fun ToolbarButton(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    iconId: Int? = null,
    vectorIcon: ImageVector? = null,
    imageVector: ImageVector? = null,
    text: String? = null,
    penColor: Color? = null,
    contentDescription: String = ""
) {
    val bgColor = when {
        penColor != null -> penColor
        isSelected -> NotableStyle.Accent
        else -> Color.Transparent
    }
    // Icon color that stays readable on whatever is behind it.
    val tint = when {
        penColor != null -> if (penColor.luminance() < 0.45f) Color.White else Color.Black
        isSelected -> NotableStyle.OnAccent
        else -> Color.Black
    }
    val shape = if (isSelected) NotableStyle.SmallShape else CircleShape

    Box(
        Modifier
            .then(modifier)
            .noRippleClickable { onSelect() }
            .padding(1.dp)
            .background(color = bgColor, shape = shape)
            .then(
                if (isSelected && penColor != null) Modifier.border(2.dp, Color.Black, shape)
                else Modifier
            )
            .padding(6.dp)
    ) {
        when {
            iconId != null -> {
                Icon(painterResource(iconId), contentDescription, tint = tint)
            }

            vectorIcon != null -> {
                Icon(vectorIcon, contentDescription, tint = tint)
            }

            imageVector != null -> {
                Icon(imageVector, contentDescription, tint = tint)
            }

            text != null -> {
                Text(text, fontSize = 20.sp, color = tint)
            }
        }
    }
}
