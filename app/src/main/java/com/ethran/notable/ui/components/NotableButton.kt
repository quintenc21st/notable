package com.ethran.notable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethran.notable.ui.theme.NotableStyle

/**
 * The app's standard button: rounded, flat, with an optional leading icon. [primary] fills it
 * with the accent color — use it for the one main action on a screen.
 */
@Composable
fun NotableButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = NotableStyle.ControlShape
    val background = if (primary && enabled) NotableStyle.Accent else Color.White
    val outline = when {
        !enabled -> Color.Gray
        primary -> NotableStyle.Accent
        else -> Color.Black
    }
    val content = when {
        !enabled -> Color.Gray
        primary -> NotableStyle.OnAccent
        else -> Color.Black
    }
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(background)
            .border(1.5.dp, outline, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = content,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
