package com.ethran.notable.editor.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.data.datastore.BUTTON_SIZE
import com.ethran.notable.data.datastore.GlobalAppSettings
import com.ethran.notable.editor.utils.autoEInkAnimationOnScroll
import com.ethran.notable.ui.components.PagePreview
import com.ethran.notable.ui.components.ScaledPopup
import com.ethran.notable.ui.viewmodels.PagesViewModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Check
import compose.icons.feathericons.Copy
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.Grid
import compose.icons.feathericons.Image
import compose.icons.feathericons.PlusCircle
import compose.icons.feathericons.Trash
import compose.icons.feathericons.X
import kotlinx.coroutines.launch

/**
 * Compact page picker shown over the editor when the page number in the toolbar is tapped.
 *
 * - Scroll the strip and tap a page to switch to it.
 * - Long-press a page for the Edit Mode actions of the full pages view (delete, duplicate,
 *   add page after, move earlier/later) plus "Generate previews".
 * - Tapping outside the popup (or pressing back) dismisses it.
 *
 * It reuses [PagesViewModel], so page edits go through the same code paths as the
 * full-screen pages view.
 */
@Composable
fun PagesPopup(
    bookId: String,
    currentPageId: String?,
    onSwitchToPage: (String) -> Unit,
    onPagesChanged: (List<String>) -> Unit,
    onOpenAllPages: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Separate key so this instance never collides with the full-screen view's view model.
    val viewModel: PagesViewModel = hiltViewModel(key = "editor-pages-popup")
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(bookId) { viewModel.loadBook(bookId) }

    // Keep the toolbar's "n/total" correct after pages are added, removed or moved.
    LaunchedEffect(state.pageIds) {
        if (!state.isLoading) onPagesChanged(state.pageIds)
    }

    LaunchedEffect(state.bookMissing) {
        if (state.bookMissing) onDismiss()
    }

    val toolbarAtTop = GlobalAppSettings.current.toolbarPosition == AppSettings.Position.Top
    val density = LocalDensity.current
    val gap = with(density) { (BUTTON_SIZE + 10).dp.roundToPx() }

    ScaledPopup(
        alignment = if (toolbarAtTop) Alignment.TopCenter else Alignment.BottomCenter,
        offset = IntOffset(0, if (toolbarAtTop) gap else -gap),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        PagesPopupContent(
            isLoading = state.isLoading,
            pageIds = state.pageIds,
            currentPageId = currentPageId,
            onSwitchToPage = onSwitchToPage,
            onOpenAllPages = onOpenAllPages,
            onClose = onDismiss,
            onDelete = viewModel::deletePage,
            onDuplicate = viewModel::duplicatePage,
            onAddAfter = { index -> viewModel.newPageInBook(bookId, index + 1) },
            onMove = { pageId, toIndex -> viewModel.reorderPage(bookId, pageId, toIndex) },
            onGeneratePreviews = viewModel::generateThumbnailsForCurrentBook,
        )
    }
}

// Sized for a 10.3" Boox in landscape (about 1300dp wide): roughly seven pages are visible
// at once, and every long-press action fits on one row without scrolling.
private val THUMB_WIDTH = 150.dp
private val THUMB_SPACING = 14.dp
private val PANEL_SHAPE = RoundedCornerShape(14.dp)
private val THUMB_SHAPE = RoundedCornerShape(8.dp)
private val BUTTON_SHAPE = RoundedCornerShape(10.dp)

@Composable
private fun PagesPopupContent(
    isLoading: Boolean,
    pageIds: List<String>,
    currentPageId: String?,
    onSwitchToPage: (String) -> Unit,
    onOpenAllPages: () -> Unit,
    onClose: () -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onAddAfter: (Int) -> Unit,
    onMove: (String, Int) -> Unit,
    onGeneratePreviews: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Page chosen with a long-press; while set, the header turns into its action bar.
    var selectedPageId by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var previewsRequested by remember { mutableStateOf(false) }

    val currentIndex = pageIds.indexOf(currentPageId)
    val thumbWidthPx = with(density) { THUMB_WIDTH.roundToPx() }

    /** Scrolls so that page [index] sits in the middle of the strip. */
    suspend fun centerOn(index: Int) {
        val viewport = listState.layoutInfo.viewportSize.width
        if (viewport > 0) {
            listState.scrollToItem(index, -((viewport - thumbWidthPx) / 2).coerceAtLeast(0))
        } else {
            // Not laid out yet: leave a few pages of context before it instead.
            listState.scrollToItem((index - 3).coerceAtLeast(0))
        }
    }

    // Open on the current page.
    var didInitialScroll by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading, currentIndex) {
        if (!isLoading && !didInitialScroll && currentIndex >= 0) {
            centerOn(currentIndex)
            didInitialScroll = true
        }
    }

    // The selected page may have been deleted.
    LaunchedEffect(pageIds) {
        val selected = selectedPageId
        if (selected != null && !pageIds.contains(selected)) {
            selectedPageId = null
            confirmDelete = false
        }
    }

    val selectedId = selectedPageId
    val selectedIndex = selectedId?.let { pageIds.indexOf(it) } ?: -1
    val isSelecting = selectedId != null && selectedIndex >= 0

    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 1180.dp)
            .fillMaxWidth()
            .clip(PANEL_SHAPE)
            .background(Color.White)
            .border(2.dp, Color.Black, PANEL_SHAPE)
    ) {
        // --- Header / action bar ---
        if (isSelecting && selectedId != null) {
            // Scrolls sideways as a fallback, should the actions not fit (portrait, large UI scale).
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .height(60.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (confirmDelete) "Delete page ${selectedIndex + 1}?"
                    else "Page ${selectedIndex + 1}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(end = 12.dp)
                )
                if (confirmDelete) {
                    BarButton(FeatherIcons.Trash, "Delete", onDark = true) {
                        onDelete(selectedId)
                        selectedPageId = null
                        confirmDelete = false
                    }
                    BarButton(FeatherIcons.X, "Cancel", onDark = true) { confirmDelete = false }
                } else {
                    val isOpenPage = selectedId == currentPageId
                    BarButton(
                        FeatherIcons.ArrowLeft, "Move left", onDark = true,
                        enabled = selectedIndex > 0
                    ) { onMove(selectedId, selectedIndex - 1) }
                    BarButton(
                        FeatherIcons.ArrowRight, "Move right", onDark = true,
                        enabled = selectedIndex < pageIds.size - 1
                    ) { onMove(selectedId, selectedIndex + 1) }
                    BarButton(FeatherIcons.PlusCircle, "Add after", onDark = true) {
                        onAddAfter(selectedIndex)
                    }
                    BarButton(FeatherIcons.Copy, "Duplicate", onDark = true) {
                        onDuplicate(selectedId)
                    }
                    // The page shown in the editor can't be deleted out from under it.
                    BarButton(
                        FeatherIcons.Trash, if (isOpenPage) "Delete (open)" else "Delete",
                        onDark = true, enabled = !isOpenPage
                    ) { confirmDelete = true }
                    BarButton(
                        FeatherIcons.Image,
                        if (previewsRequested) "Generating…" else "Previews",
                        onDark = true
                    ) {
                        onGeneratePreviews()
                        previewsRequested = true
                    }
                    Spacer(Modifier.width(12.dp))
                    BarButton(FeatherIcons.Check, "Done", onDark = true, emphasized = true) {
                        selectedPageId = null
                    }
                }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
                if (pageIds.isNotEmpty()) {
                    Text(
                        text = (if (currentIndex >= 0) "${currentIndex + 1} of ${pageIds.size}"
                        else "${pageIds.size}") + "  ·  hold a page for options",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                BarButton(
                    FeatherIcons.Crosshair, "Current page", emphasized = true,
                    enabled = currentIndex >= 0
                ) { scope.launch { centerOn(currentIndex) } }
                BarButton(FeatherIcons.Grid, "All pages") { onOpenAllPages() }
                BarButton(FeatherIcons.X, "Close") { onClose() }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )

        // --- Thumbnails ---
        if (isLoading) {
            Text("Loading…", color = Color.Black, modifier = Modifier.padding(20.dp))
        } else {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(THUMB_SPACING),
                modifier = Modifier
                    .fillMaxWidth()
                    .autoEInkAnimationOnScroll()
            ) {
                itemsIndexed(pageIds, key = { _, id -> id }) { index, pageId ->
                    PopupPageThumb(
                        pageId = pageId,
                        index = index,
                        isCurrent = pageId == currentPageId,
                        isSelected = pageId == selectedPageId,
                        onTap = {
                            if (selectedPageId != null) {
                                // While picking actions, a tap moves the selection.
                                selectedPageId = pageId
                                confirmDelete = false
                            } else {
                                onSwitchToPage(pageId)
                            }
                        },
                        onLongPress = {
                            selectedPageId = pageId
                            confirmDelete = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PopupPageThumb(
    pageId: String,
    index: Int,
    isCurrent: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val borderWidth = if (isSelected || isCurrent) 3.dp else 1.dp
    Column(
        Modifier
            .width(THUMB_WIDTH)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(THUMB_SHAPE)
                .background(Color.White)
                .border(borderWidth, Color.Black, THUMB_SHAPE)
        ) {
            PagePreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .padding(borderWidth),
                pageId = pageId
            )
            if (isSelected) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FeatherIcons.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isCurrent) "${index + 1}  ·  Current" else "${index + 1}",
            fontSize = 14.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) Color.White else Color.Black,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isCurrent) Color.Black else Color.White)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/**
 * Icon + label button for the popup's header. Flat and high-contrast for e-ink: no shadows,
 * ripples or greys that would ghost. [onDark] is for the black selection bar; [emphasized]
 * fills the button with the bar's contrasting color.
 */
@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    onDark: Boolean = false,
    emphasized: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fg = if (onDark) Color.White else Color.Black
    val bg = if (onDark) Color.Black else Color.White
    val filled = emphasized && enabled
    val content = when {
        !enabled -> Color.Gray
        filled -> bg
        else -> fg
    }
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(BUTTON_SHAPE)
            .background(if (filled) fg else bg)
            .border(1.dp, if (enabled) fg else Color.Gray, BUTTON_SHAPE)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = content, fontSize = 15.sp, maxLines = 1)
    }
}
