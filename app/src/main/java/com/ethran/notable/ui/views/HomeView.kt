package com.ethran.notable.ui.views

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.ethran.notable.data.db.Page
import com.ethran.notable.ui.components.NotableButton
import com.ethran.notable.ui.theme.NotableStyle
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Home
import compose.icons.feathericons.Plus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ethran.notable.R
import com.ethran.notable.data.AppRepository
import com.ethran.notable.data.db.Folder
import com.ethran.notable.data.db.Notebook
import com.ethran.notable.editor.EditorDestination
import com.ethran.notable.editor.ui.Topbar
import com.ethran.notable.editor.utils.autoEInkAnimationOnScroll
import com.ethran.notable.navigation.NavigationDestination
import com.ethran.notable.sync.SyncBadge
import com.ethran.notable.ui.LocalSnackContext
import com.ethran.notable.ui.SnackConf
import com.ethran.notable.ui.components.BreadCrumb
import com.ethran.notable.ui.components.NotebookCard
import com.ethran.notable.ui.components.ShowPagesRow
import com.ethran.notable.ui.dialogs.ConflictResolutionDialog
import com.ethran.notable.ui.dialogs.EmptyBookWarningHandler
import com.ethran.notable.ui.dialogs.FolderConfigDialog
import com.ethran.notable.ui.dialogs.NotebookConfigDialog
import com.ethran.notable.ui.dialogs.PdfImportChoiceDialog
import com.ethran.notable.ui.noRippleClickable
import com.ethran.notable.ui.viewmodels.LibraryUiState
import com.ethran.notable.ui.viewmodels.LibraryViewModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.FilePlus
import compose.icons.feathericons.Folder
import compose.icons.feathericons.FolderPlus
import compose.icons.feathericons.Settings
import compose.icons.feathericons.Upload
import io.shipbook.shipbooksdk.ShipBook


object LibraryDestination : NavigationDestination {
    override val route = "library"
    const val FOLDER_ID_ARG = "folderId"
    val routeWithArgs = "$route?$FOLDER_ID_ARG={$FOLDER_ID_ARG}"
    fun createRoute(folderId: String? = null): String {
        return if (folderId != null) "$route?$FOLDER_ID_ARG=$folderId" else route
    }
}

private val log = ShipBook.getLogger("HomeView")

@Composable
fun Library(
    navController: NavController,
    folderId: String? = null,
    goToPage: (String) -> Unit = {},
    onCreateNewQuickPage: (String?) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(folderId) {
        viewModel.loadFolder(folderId)
    }

    LibraryContent(
        appRepository = viewModel.appRepository,
        uiState = uiState,
        onNavigateToFolder = { id -> navController.navigate(LibraryDestination.createRoute(id)) },
        onNavigateToSettings = { navController.navigate("settings") },
        onNavigateToEditor = { pageId, bookId ->
            navController.navigate(EditorDestination.createRoute(pageId, bookId))
        },
        goToPage = goToPage,
        onCreateNewQuickPage = { onCreateNewQuickPage(uiState.folderId) },
        onCreateNewFolder = viewModel::createNewFolder,
        onDeleteEmptyBook = viewModel::deleteEmptyBook,
        onCreateNewNotebook = {
            navController.navigate(NewNotebookDestination.createRoute(uiState.folderId))
        },
        onImportPdf = viewModel::onPdfFile,
        onImportXopp = viewModel::onXoppFile,
        onPreviewMissing = viewModel::onPreviewRequested

    )
}


/**
 * The library, laid out for a landscape tablet: a sidebar for moving between folders, and a main
 * area with the current folder's quick pages and notebooks in one scrolling grid.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LibraryContent(
    appRepository: AppRepository,
    uiState: LibraryUiState,
    onNavigateToFolder: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditor: (String, String) -> Unit,
    goToPage: (String) -> Unit,
    onCreateNewQuickPage: () -> Unit,
    onCreateNewFolder: () -> Unit,
    onDeleteEmptyBook: (String) -> Unit,
    onCreateNewNotebook: () -> Unit,
    onImportPdf: (Uri, Boolean) -> Unit,
    onImportXopp: (Uri) -> Unit,
    onPreviewMissing: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LibrarySidebar(
            appRepository = appRepository,
            uiState = uiState,
            onNavigateToFolder = onNavigateToFolder,
            onNavigateToSettings = onNavigateToSettings,
            onCreateNewFolder = onCreateNewFolder
        )
        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.Black)
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            LibraryHeader(
                uiState = uiState,
                onCreateNewQuickPage = onCreateNewQuickPage,
                onCreateNewNotebook = onCreateNewNotebook,
                onImportPdf = onImportPdf,
                onImportXopp = onImportXopp
            )
            NotebookGrid(
                appRepository = appRepository,
                books = uiState.books,
                singlePages = uiState.singlePages,
                isImporting = uiState.isImporting,
                syncBadges = uiState.syncBadges,
                onNavigateToEditor = onNavigateToEditor,
                goToPage = goToPage,
                onDeleteEmptyBook = onDeleteEmptyBook,
                onPreviewMissing = onPreviewMissing
            )
        }
    }
}

/**
 * Left rail: the app name, where you are (Library › folder › folder, as a tree), the folders
 * inside the current one, and Settings pinned to the bottom.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibrarySidebar(
    appRepository: AppRepository,
    uiState: LibraryUiState,
    onNavigateToFolder: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onCreateNewFolder: () -> Unit
) {
    val path = uiState.breadcrumbFolders
    Column(
        Modifier
            .width(250.dp)
            .fillMaxHeight()
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Notable",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            modifier = Modifier.padding(start = 10.dp, bottom = 18.dp)
        )

        // Where you are. The last entry is the open folder.
        SidebarItem(
            icon = FeatherIcons.Home,
            label = stringResource(R.string.home_view_name),
            selected = path.isEmpty(),
            onClick = { onNavigateToFolder(null) }
        )
        path.forEachIndexed { index, folder ->
            SidebarItem(
                icon = FeatherIcons.Folder,
                label = folder.title,
                selected = index == path.lastIndex,
                indent = ((index + 1) * 14).coerceAtMost(56),
                onClick = { onNavigateToFolder(folder.id) }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Folders")
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .autoEInkAnimationOnScroll()
        ) {
            items(uiState.folders, key = { it.id }) { folder ->
                var isFolderSettingsOpen by remember { mutableStateOf(false) }
                if (isFolderSettingsOpen) FolderConfigDialog(
                    appRepository.folderRepository,
                    folderId = folder.id,
                    onClose = {
                        log.i("Closing Directory Dialog")
                        isFolderSettingsOpen = false
                    })
                SidebarItem(
                    icon = FeatherIcons.Folder,
                    label = folder.title,
                    onClick = { onNavigateToFolder(folder.id) },
                    onLongClick = { isFolderSettingsOpen = true }
                )
            }
            item {
                SidebarItem(
                    icon = FeatherIcons.FolderPlus,
                    label = stringResource(R.string.home_add_new_folder),
                    onClick = onCreateNewFolder
                )
            }
        }

        Box(
            Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
        SidebarItem(
            icon = FeatherIcons.Settings,
            label = stringResource(R.string.settings_title),
            showDot = !uiState.isLatestVersion,
            onClick = onNavigateToSettings
        )
    }
}

/** One row of the sidebar. The [selected] row is filled with the accent color. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    indent: Int = 0,
    showDot: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val content = if (selected) NotableStyle.OnAccent else Color.Black
    Row(
        Modifier
            .padding(start = indent.dp, top = 2.dp, bottom = 2.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(NotableStyle.ControlShape)
            .background(if (selected) NotableStyle.Accent else Color.White)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = content,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showDot) {
            // An update is available.
            Box(
                Modifier
                    .size(10.dp)
                    .background(NotableStyle.Accent, CircleShape)
            )
        }
    }
}

/** Small all-caps label above a group, optionally with a count. */
@Composable
private fun SectionLabel(text: String, count: Int? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = Color.Black,
            modifier = Modifier.padding(start = 10.dp)
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            )
        }
    }
}

/** Title of the open folder, with the actions that create things in it. */
@Composable
private fun LibraryHeader(
    uiState: LibraryUiState,
    onCreateNewQuickPage: () -> Unit,
    onCreateNewNotebook: () -> Unit,
    onImportPdf: (Uri, Boolean) -> Unit,
    onImportXopp: (Uri) -> Unit,
) {
    val path = uiState.breadcrumbFolders
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = path.lastOrNull()?.title ?: stringResource(R.string.home_view_name),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (path.size > 1) {
                Text(
                    text = path.dropLast(1).joinToString("  ›  ") { it.title },
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        NotebookImportPanel(
            onCreateNewNotebook = onCreateNewNotebook,
            onCreateNewQuickPage = onCreateNewQuickPage,
            onImportPdf = onImportPdf,
            onImportXopp = onImportXopp
        )
    }
}

/**
 * Everything in the open folder, in one scrolling grid: the quick pages as a row across the
 * top, then the notebooks.
 */
@Composable
fun NotebookGrid(
    appRepository: AppRepository,
    books: List<Notebook>,
    singlePages: List<Page>,
    isImporting: Boolean,
    syncBadges: Map<String, SyncBadge>,
    onNavigateToEditor: (String, String) -> Unit,
    goToPage: (String) -> Unit,
    onDeleteEmptyBook: (String) -> Unit,
    onPreviewMissing: (String) -> Unit
) {
    val notebookCount = books.count { it.pageIds.isNotEmpty() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .autoEInkAnimationOnScroll()
    ) {
        if (singlePages.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionLabel(
                        stringResource(R.string.home_quick_pages),
                        count = singlePages.size,
                        modifier = Modifier.offset(x = (-10).dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    ShowPagesRow(
                        appRepository = appRepository,
                        pages = singlePages,
                        currentPageId = null,
                        title = null,
                        onSelectPage = goToPage,
                        onPreviewMissing = onPreviewMissing
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionLabel(
                stringResource(R.string.home_notebooks),
                count = notebookCount,
                modifier = Modifier.offset(x = (-10).dp)
            )
        }

        if (notebookCount == 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(NotableStyle.PanelShape)
                        .border(1.5.dp, Color.Black, NotableStyle.PanelShape)
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = FeatherIcons.BookOpen,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No notebooks here yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        "Use New notebook or Import, at the top right.",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        if (books.isNotEmpty()) {
            // Keyed so an open dialog stays with its notebook when one is added before it.
            items(books.reversed(), key = { it.id }) { book ->
                if (book.pageIds.isEmpty()) {
                    if (!isImporting) {
                        EmptyBookWarningHandler(
                            emptyBook = book,
                            onDelete = { onDeleteEmptyBook(book.id) },
                            onDismiss = { })
                    }
                    return@items
                }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isConflictOpen by remember { mutableStateOf(false) }
                NotebookCard(
                    bookId = book.id,
                    title = book.title,
                    pageIds = book.pageIds,
                    openPageId = book.openPageId,
                    syncBadge = syncBadges[book.id],
                    // A conflicted notebook opens the resolution dialog on tap — the reachable entry
                    // point for the CONFLICT badge — instead of the editor.
                    onOpen = { bookId, pageId ->
                        if (syncBadges[bookId] == SyncBadge.CONFLICT) isConflictOpen = true
                        else onNavigateToEditor(pageId, bookId)
                    },
                    onOpenSettings = { isSettingsOpen = true },
                    onPreviewMissing = onPreviewMissing
                )

                if (isSettingsOpen) {
                    NotebookConfigDialog(bookId = book.id, onClose = { isSettingsOpen = false })
                }

                if (isConflictOpen) {
                    ConflictResolutionDialog(
                        bookId = book.id,
                        title = book.title,
                        onClose = { isConflictOpen = false })
                }
            }
        }
    }
}

/** The create actions for the open folder: quick page, import (PDF or xopp), new notebook. */
@Composable
fun NotebookImportPanel(
    onCreateNewNotebook: () -> Unit,
    onCreateNewQuickPage: () -> Unit,
    onImportPdf: (Uri, Boolean) -> Unit,
    onImportXopp: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackState = LocalSnackContext.current
    var showPdfImportChoiceDialog by remember { mutableStateOf<Uri?>(null) }

    showPdfImportChoiceDialog?.let { uri ->
        PdfImportChoiceDialog(uri = uri, onCopy = { uri ->
            showPdfImportChoiceDialog = null
            onImportPdf(uri, /* copy= */ true)
        }, onObserve = {
            showPdfImportChoiceDialog = null
            onImportPdf(it, /* copy= */ false)
        }, onDismiss = { showPdfImportChoiceDialog = null })
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            log.w("PickVisualMedia: uri is null (user cancelled or provider returned null)")
            return@rememberLauncherForActivityResult
        }
        try {
            val mimeType = context.contentResolver.getType(uri)
            log.d("Selected file mimeType: $mimeType, uri: $uri")
            if (mimeType == "application/pdf" || uri.toString()
                    .endsWith(".pdf", ignoreCase = true)
            ) {
                showPdfImportChoiceDialog = uri
            } else {
                onImportXopp(uri)
            }
        } catch (e: Exception) {
            log.e("contentPicker failed: ${e.message}", e)
            snackState.showOrUpdateSnack(SnackConf(text = "Importing failed: ${e.message}"))
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotableButton(
            text = "Quick page",
            icon = FeatherIcons.FilePlus,
            onClick = onCreateNewQuickPage
        )
        NotableButton(
            text = "Import",
            icon = FeatherIcons.Upload,
            onClick = {
                launcher.launch(
                    arrayOf(
                        "application/x-xopp",
                        "application/gzip",
                        "application/octet-stream",
                        "application/pdf"
                    )
                )
            }
        )
        NotableButton(
            text = stringResource(R.string.new_notebook_screen_title),
            icon = FeatherIcons.Plus,
            primary = true,
            onClick = onCreateNewNotebook
        )
    }
}


@Preview(
    showBackground = true,
    name = "Library - Default State",
    widthDp = 800,
    heightDp = 1200
)
@Composable
fun LibraryContentPreview() {
    // 1. Create a dummy UI state with mock data
    val mockUiState = LibraryUiState(
        folderId = null,
        isLatestVersion = true,
        isImporting = false,
        breadcrumbFolders = listOf(
            // Optional: Add mock breadcrumbs if you want to preview nested folder state
             Folder(id = "root", title = "Home", parentFolderId = null)
        ),
        folders = listOf(
            // Adjust constructor arguments based on your exact entity definition
            Folder(id = "folder_1", title = "Work Notes", parentFolderId = null),
            Folder(id = "folder_2", title = "Personal", parentFolderId = null)
        ),
        books = listOf(
            // Needs pageIds to render the card (empty books show a warning)
            Notebook(id = "book_1", title = "Meeting Minutes", pageIds = listOf("page1", "page2")),
            Notebook(id = "book_2", title = "Journal", pageIds = listOf("page3"))
        ),
        singlePages = emptyList() // Populate with mock Page() objects if you want to see Quick Pages
    )

    // 2. Render the stateless component with empty lambdas
//    LibraryContent(
//        uiState = mockUiState,
//        onNavigateToFolder = {},
//        onNavigateToSettings = {},
//        onNavigateToEditor = { _, _ -> },
//        goToPage = {},
//        onCreateNewQuickPage = {},
//        onCreateNewFolder = {},
//        onDeleteEmptyBook = {},
//        onCreateNewNotebook = {},
//        onImportPdf = { _, _ -> },
//        onImportXopp = {})
}

@Suppress("UnusedVariable")
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Preview(showBackground = true, name = "Library - Update Available & Importing")
@Composable
fun LibraryContentUpdatePreview() {
    val mockUiState = LibraryUiState(
        folderId = "folder_1",
        isLatestVersion = false, // Will show the red badge on the settings icon
        isImporting = true,      // Will hide the delete warning for empty books
        breadcrumbFolders = emptyList(),
        folders = emptyList(),
        books = emptyList(),
        singlePages = emptyList()
    )

//    LibraryContent(
//        uiState = mockUiState,
//        onNavigateToFolder = {},
//        onNavigateToSettings = {},
//        onNavigateToEditor = { _, _ -> },
//        goToPage = {},
//        onCreateNewQuickPage = {},
//        onCreateNewFolder = {},
//        onDeleteEmptyBook = {},
//        onCreateNewNotebook = {},
//        onImportPdf = { _, _ -> },
//        onImportXopp = {})
}