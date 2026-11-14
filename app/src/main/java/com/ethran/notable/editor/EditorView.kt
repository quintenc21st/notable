package com.ethran.notable.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.data.datastore.SIDE_TOOLBAR_WIDTH
import com.ethran.notable.editor.canvas.CanvasEventBus
import com.ethran.notable.editor.state.ClipboardStore
import com.ethran.notable.editor.ui.EditorSurface
import com.ethran.notable.editor.ui.HorizontalScrollIndicator
import com.ethran.notable.editor.ui.PagesPopup
import com.ethran.notable.editor.ui.ScrollIndicator
import com.ethran.notable.editor.ui.SelectedBitmap
import com.ethran.notable.editor.ui.toolbar.PositionedToolbar
import com.ethran.notable.editor.ui.toolbar.toolbarPositionForOrientation
import com.ethran.notable.gestures.EditorGestureReceiver
import com.ethran.notable.navigation.NavigationDestination
import com.ethran.notable.ui.LocalSnackContext
import com.ethran.notable.ui.SnackConf
import com.ethran.notable.ui.convertDpToPixel
import com.ethran.notable.ui.theme.InkaTheme
import io.shipbook.shipbooksdk.ShipBook
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.math.abs

private val log = ShipBook.getLogger("EditorView")

object EditorDestination : NavigationDestination {
    override val route = "editor"

    const val PAGE_ID_ARG = "pageId"
    const val BOOK_ID_ARG = "bookId"

    // Unified route: editor/{pageId}?bookId={bookId}
    val routeWithArgs = "$route/{$PAGE_ID_ARG}?$BOOK_ID_ARG={$BOOK_ID_ARG}"

    /**
     * Helper to create the path. If bookId is null, it just won't be appended.
     */
    fun createRoute(pageId: String, bookId: String? = null): String {
        return "$route/$pageId" + if (bookId != null) "?$BOOK_ID_ARG=$bookId" else ""
    }
}


@Composable
fun EditorView(
    initialPageId: String,
    bookId: String?,
    isQuickNavOpen: Boolean,

    // navigation callbacks
    onPageChange: (String) -> Unit,
    goToLibrary: (folderId: String?) -> Unit,
    goToPages: (bookId: String) -> Unit,
    goToBugReport: () -> Unit,

    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackManager = LocalSnackContext.current
    val scope = rememberCoroutineScope()

    // Single point of entry for loading book data based on the pageId from Navigation
    // Should not be used for regular page switching
    LaunchedEffect(initialPageId) {
        log.v("EditorView: pageId changed to $initialPageId, loading data")
        viewModel.loadToolbarState(bookId, initialPageId)
    }

    // Sync isQuickNavOpen to ViewModel
    LaunchedEffect(isQuickNavOpen) {
        viewModel.onToolbarAction(ToolbarAction.UpdateQuickNavOpen(isQuickNavOpen))
    }


    BoxWithConstraints {
        val height = convertDpToPixel(this.maxHeight, context).toInt()
        val width = convertDpToPixel(this.maxWidth, context).toInt()

        // Here we load initial page into the memory
        val page = remember {
            PageView(
                context = context,
                coroutineScope = scope,
                pageDataManager = viewModel.pageDataManager,
                pageLeases = viewModel.pageLeases,
                appRepository = viewModel.appRepository,
                initialPageId = initialPageId,
                viewWidth = width,
                viewHeight = height,
                snackManager = snackManager,
                onPageShown = viewModel::onPageShown,
                onPageLeft = viewModel::onPageLeft,
            )
        }

        val history = remember(page) { viewModel.createHistory(page) }

        val editorControlTower = remember {
            EditorControlTower(
                scope = scope,
                page = page,
                history = history,
                viewModel = viewModel,
                clipboardStore = ClipboardStore,
            )
        }


        // Initialize ViewModel with persisted settings on first composition
        LaunchedEffect(Unit) {
            viewModel.initFromPersistedSettings()
            viewModel.updateDrawingState()
        }

//         val editorControlTower = remember {
//             EditorControlTower(scope, page, history, editorState, context, appRepository).apply { registerObservers() }
        DisposableEffect(editorControlTower) {
            editorControlTower.registerObservers()
            onDispose {
                editorControlTower.unregisterObservers()
            }
        }

        // Collect UI Events from ViewModel (navigation )
        LaunchedEffect(Unit) {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    is EditorUiEvent.NavigateToLibrary -> {
                        goToLibrary(event.folderId)
                    }

                    is EditorUiEvent.NavigateToPages -> {
                        goToPages(event.bookId)
                    }

                    EditorUiEvent.NavigateToBugReport -> {
                        goToBugReport()
                    }
                }
            }
        }

        // Collect Canvas Commands from ViewModel
        LaunchedEffect(Unit) {
            viewModel.canvasCommands.collect { command ->
                when (command) {
                    CanvasCommand.Undo -> editorControlTower.undo()
                    CanvasCommand.Redo -> editorControlTower.redo()
                    CanvasCommand.Paste -> editorControlTower.pasteFromClipboard()
                    CanvasCommand.ResetView -> editorControlTower.resetZoomAndScroll()
                    CanvasCommand.ClearAllStrokes -> {
                        CanvasEventBus.clearPageSignal.emit(Unit)
                        snackManager.displaySnack(
                            SnackConf(
                                text = "Cleared all strokes",
                                duration = 2000
                            )
                        )
                    }

                    CanvasCommand.RefreshCanvas -> {
                        CanvasEventBus.reloadFromDb.emit(Unit)
                    }

                    is CanvasCommand.CopyImageToCanvas -> {
                        CanvasEventBus.addImageByUri.value = command.uri
                    }
                }
            }
        }

        // Handle Canvas signals in UI
        LaunchedEffect(Unit) {
            CanvasEventBus.closeMenusSignal.collect {
                log.d("Closing all menus")
                viewModel.onToolbarAction(ToolbarAction.CloseAllMenus)
            }
        }

        // Handle focus changes from Canvas
//        LaunchedEffect(Unit) {
//            CanvasEventBus.onFocusChange.collect { hasFocus ->
//                log.d("Canvas has focus: $hasFocus")
//                if (hasFocus)
//                    viewModel.updateDrawingState()
//
//            }
//        }

        val toolbarState by viewModel.toolbarState.collectAsStateWithLifecycle()

        // Observe pageId changes from ViewModel state for navigation
        LaunchedEffect(viewModel) {
            snapshotFlow { toolbarState.pageId }.filterNotNull().distinctUntilChanged()
                .drop(1) // Skip initial emission from loadBookData
                .collect { newPageId ->
                    log.v("EditorView: snapshotFlow detected pageId change to $newPageId, triggering onPageChange")
                    // update the PageView
                    page.changePage(newPageId)

                    // update the navigation state
                    onPageChange(newPageId)
                }
        }

        // Sync PageView state to ViewModel for Toolbar rendering
        // Only zoom matters here; collecting the whole viewport would recompose on every scroll.
        val zoomFlow = remember(page) { page.viewport.map { it.zoom }.distinctUntilChanged() }
        val zoomLevel by zoomFlow.collectAsStateWithLifecycle(page.zoom)
        val selectionActive = viewModel.selectionState.isNonEmpty()
        LaunchedEffect(
            zoomLevel, selectionActive
        ) {
            log.v("EditorView: zoomLevel=$zoomLevel, selectionActive=$selectionActive")
            // "Reset view" is offered whenever the page is not at its resting, fit-to-width zoom.
            viewModel.setShowResetView(abs(zoomLevel - page.fitZoom) > 0.005f)
            viewModel.setSelectionActive(selectionActive)
        }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.onDispose(page)
            }
        }



        InkaTheme {
            // A side toolbar gets a strip of its own and the page takes the rest, so the bar never
            // covers the page's left or right edge. A top or bottom toolbar lies over the page.
            val toolbarPosition = toolbarPositionForOrientation()
            val sideStrip =
                if (toolbarPosition.isVertical && toolbarState.isToolbarOpen) SIDE_TOOLBAR_WIDTH.dp
                else 0.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (toolbarPosition == AppSettings.Position.Left) sideStrip else 0.dp,
                        end = if (toolbarPosition == AppSettings.Position.Right) sideStrip else 0.dp
                    )
            ) {
                EditorGestureReceiver(actions = editorControlTower)
                EditorSurface(
                    viewModel = viewModel,
                    page = page,
                    history = history
                )
                SelectedBitmap(
                    context = context, controlTower = editorControlTower
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    ScrollIndicator(viewModel = viewModel, page = page)
                }
                HorizontalScrollIndicator(viewModel = viewModel, page = page)
            }
            PositionedToolbar(
                viewModel = viewModel, onDrawingStateCheck = { viewModel.updateDrawingState() })

            val popupBookId = toolbarState.notebookId
            if (toolbarState.isPagesPopupOpen && popupBookId != null) {
                PagesPopup(
                    bookId = popupBookId,
                    currentPageId = toolbarState.pageId,
                    onSwitchToPage = viewModel::switchPageFromPopup,
                    onPagesChanged = viewModel::syncPageNumber,
                    onOpenAllPages = { viewModel.onToolbarAction(ToolbarAction.NavigateToPages) },
                    onDismiss = {
                        viewModel.onToolbarAction(ToolbarAction.TogglePagesPopup(false))
                    }
                )
            }
        }
    }
}
