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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
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
    val gap = with(density) { (BUTTON_SIZE + 6).dp.roundToPx() }

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
            onDelete = viewModel::deletePage,
            onDuplicate = viewModel::duplicatePage,
            onAddAfter = { index -> viewModel.newPageInBook(bookId, index + 1) },
            onMove = { pageId, toIndex -> viewModel.reorderPage(bookId, pageId, toIndex) },
            onGeneratePreviews = viewModel::generateThumbnailsForCurrentBook,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagesPopupContent(
    isLoading: Boolean,
    pageIds: List<String>,
    currentPageId: String?,
    onSwitchToPage: (String) -> Unit,
    onOpenAllPages: () -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onAddAfter: (Int) -> Unit,
    onMove: (String, Int) -> Unit,
    onGeneratePreviews: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Page chosen with a long-press; while set, the action row is shown for it.
    var selectedPageId by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var previewsRequested by remember { mutableStateOf(false) }

    val currentIndex = pageIds.indexOf(currentPageId)

    fun jumpToCurrent() {
        if (currentIndex >= 0) {
            // Leave a page or two of context to the left of the current one.
            scope.launch { listState.scrollToItem((currentIndex - 2).coerceAtLeast(0)) }
        }
    }

    // Open on the current page.
    var didInitialScroll by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading, currentIndex) {
        if (!isLoading && !didInitialScroll && currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 2).coerceAtLeast(0))
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

    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .border(1.dp, Color.Black, RectangleShape)
            .background(Color.White)
            .padding(vertical = 8.dp)
    ) {
        // --- Header ---
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (pageIds.isEmpty()) "Pages"
                else "Pages  ·  ${if (currentIndex >= 0) currentIndex + 1 else "–"} of ${pageIds.size}",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            Spacer(Modifier.weight(1f))
            Pill("Jump to current", filled = true, enabled = currentIndex >= 0) { jumpToCurrent() }
            Spacer(Modifier.width(8.dp))
            Pill("All pages") { onOpenAllPages() }
        }

        // --- Long-press actions for the selected page ---
        val selectedId = selectedPageId
        val selectedIndex = selectedId?.let { pageIds.indexOf(it) } ?: -1
        if (selectedId != null && selectedIndex >= 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Page ${selectedIndex + 1}:", color = Color.Black)
                if (confirmDelete) {
                    Text("Delete this page?", color = Color.Black)
                    Pill("Delete", filled = true) {
                        onDelete(selectedId)
                        selectedPageId = null
                        confirmDelete = false
                    }
                    Pill("Cancel") { confirmDelete = false }
                } else {
                    val isOpenPage = selectedId == currentPageId
                    // The page shown in the editor can't be deleted out from under it.
                    Pill(
                        if (isOpenPage) "Delete (open page)" else "Delete",
                        enabled = !isOpenPage
                    ) { confirmDelete = true }
                    Pill("Duplicate") { onDuplicate(selectedId) }
                    Pill("Add page after") { onAddAfter(selectedIndex) }
                    Pill("◀ Move", enabled = selectedIndex > 0) {
                        onMove(selectedId, selectedIndex - 1)
                    }
                    Pill("Move ▶", enabled = selectedIndex < pageIds.size - 1) {
                        onMove(selectedId, selectedIndex + 1)
                    }
                    Pill(if (previewsRequested) "Generating previews…" else "Generate previews") {
                        onGeneratePreviews()
                        previewsRequested = true
                    }
                    Pill("Done", filled = true) { selectedPageId = null }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- Thumbnails ---
        if (isLoading) {
            Text(
                "Loading…",
                color = Color.Black,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
    Column(
        Modifier
            .width(110.dp)
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
                .border(
                    width = when {
                        isSelected -> 4.dp
                        isCurrent -> 2.dp
                        else -> 1.dp
                    },
                    color = Color.Black,
                    shape = RectangleShape
                )
        ) {
            PagePreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .padding(if (isSelected) 4.dp else if (isCurrent) 2.dp else 1.dp),
                pageId = pageId
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isCurrent) "${index + 1} · current" else "${index + 1}",
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) Color.White else Color.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isCurrent) Color.Black else Color.White)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** E-ink friendly pill button: black outline, optionally filled black. */
@Composable
private fun Pill(
    text: String,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val textColor = when {
        !enabled -> Color.Gray
        filled -> Color.White
        else -> Color.Black
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (filled && enabled) Color.Black else Color.White)
            .border(1.dp, if (enabled) Color.Black else Color.Gray, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = textColor)
    }
}
