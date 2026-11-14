package com.ethran.notable.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethran.notable.sync.SyncBadge
import com.ethran.notable.ui.theme.NotableStyle

// A cover reads as a bound notebook: square on the spine side, rounded on the open side.
private val CoverShape = RoundedCornerShape(
    topStart = 3.dp, bottomStart = 3.dp, topEnd = 12.dp, bottomEnd = 12.dp
)

/**
 * A notebook in the library: its first page as the cover, a colored spine that stays the same
 * for that notebook, the page count, and the title underneath.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotebookCard(
    modifier: Modifier = Modifier,
    bookId: String,
    title: String,
    pageIds: List<String>,
    openPageId: String?,
    syncBadge: SyncBadge? = null,
    onOpen: (bookId: String, pageId: String) -> Unit,
    onOpenSettings: (bookId: String) -> Unit,
    onPreviewMissing: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onOpen(bookId, openPageId ?: pageIds[0]) },
                onLongClick = { onOpenSettings(bookId) })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(CoverShape)
                .background(Color.White)
                .border(1.5.dp, Color.Black, CoverShape)
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(NotableStyle.notebookColor(bookId))
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.Black)
                )
                PagePreview(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    pageId = pageIds[0],
                    onPreviewMissing = onPreviewMissing
                )
            }
            Text(
                text = pageIds.size.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black, CircleShape)
                    .padding(horizontal = 9.dp, vertical = 2.dp)
            )
            syncBadge?.iconOrNull()?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = "Sync status: ${syncBadge.name}",
                    tint = Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Icon for the corner badge, or null when there is nothing worth showing. */
private fun SyncBadge.iconOrNull(): ImageVector? = when (this) {
    SyncBadge.SYNCED -> Icons.Default.CloudDone
    SyncBadge.NOT_SYNCED -> Icons.Default.CloudOff
    SyncBadge.SCHEDULED -> Icons.Default.Schedule
    SyncBadge.SYNCING -> Icons.Default.Sync
    SyncBadge.REMOTE_AHEAD -> Icons.Default.CloudDownload
    SyncBadge.CONFLICT -> Icons.Default.SyncProblem
    SyncBadge.ERROR -> Icons.Default.ErrorOutline
}