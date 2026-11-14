package com.ethran.notable.editor.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.ethran.notable.R
import com.ethran.notable.data.datastore.BUTTON_SIZE
import com.ethran.notable.editor.ToolbarAction
import com.ethran.notable.editor.ToolbarUiState
import com.ethran.notable.editor.state.Mode
import com.ethran.notable.editor.ui.toolbar.model.ToolbarPen
import com.ethran.notable.editor.utils.Pen
import com.ethran.notable.io.ExportFormat
import com.ethran.notable.ui.components.ScaledPopup
import com.ethran.notable.ui.noRippleClickable
import com.ethran.notable.ui.theme.notablePanel
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.File
import compose.icons.feathericons.FileText
import compose.icons.feathericons.Home
import compose.icons.feathericons.Image
import compose.icons.feathericons.Layout
import compose.icons.feathericons.Link
import compose.icons.feathericons.Trash2

/**
 * Menu for the toolbar, providing export options and other page-level actions.
 * Centralizes actions via [ToolbarAction].
 */
@Composable
fun ToolbarMenu(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
) {
    val density = LocalDensity.current
    // Beside the button on a side toolbar; under it, tucked in from the edge, otherwise.
    val (alignment, offset) =
        if (LocalToolbarPosition.current.isVertical) toolbarPopupPlacement()
        else Alignment.TopEnd to with(density) {
            IntOffset(
                (-10).dp.roundToPx(),
                50.dp.roundToPx()
            )
        }

    ScaledPopup(
        alignment = alignment,
        onDismissRequest = {
            onAction(ToolbarAction.ToggleMenu)
        },
        offset = offset,
        properties = PopupProperties(focusable = true),
    ) {
        ToolbarMenuContent(
            uiState = uiState,
            onAction = onAction
        )
    }
}

@Composable
private fun ToolbarMenuContent(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit
) {
    Column(
        Modifier
            .padding(bottom = (BUTTON_SIZE + 5).dp)
            .notablePanel()
            .width(IntrinsicSize.Max)
            .padding(vertical = 6.dp)
    ) {
        // Home / Library
        MenuItem(FeatherIcons.Home, stringResource(R.string.home_view_name)) {
            onAction(ToolbarAction.NavigateToLibrary)
            onAction(ToolbarAction.ToggleMenu)
        }
        DividerCentered()

        // Page exports
        MenuItem(FeatherIcons.FileText, stringResource(R.string.export_page_to, "PDF")) {
            onAction(ToolbarAction.ExportPage(ExportFormat.PDF))
            onAction(ToolbarAction.ToggleMenu)
        }
        MenuItem(FeatherIcons.Image, stringResource(R.string.export_page_to, "PNG")) {
            onAction(ToolbarAction.ExportPage(ExportFormat.PNG))
            onAction(ToolbarAction.ToggleMenu)
        }
        MenuItem(FeatherIcons.Image, stringResource(R.string.export_page_to, "JPEG")) {
            onAction(ToolbarAction.ExportPage(ExportFormat.JPEG))
            onAction(ToolbarAction.ToggleMenu)
        }
        MenuItem(FeatherIcons.File, stringResource(R.string.export_page_to, "xopp")) {
            onAction(ToolbarAction.ExportPage(ExportFormat.XOPP))
            onAction(ToolbarAction.ToggleMenu)
        }
        MenuItem(FeatherIcons.Link, stringResource(R.string.copy_page_link)) {
            onAction(ToolbarAction.CopyPageLink)
            onAction(ToolbarAction.ToggleMenu)
        }
        DividerCentered()

        // Book exports
        if (uiState.notebookId != null) {
            MenuItem(FeatherIcons.FileText, stringResource(R.string.export_book_to, "PDF")) {
                onAction(ToolbarAction.ExportBook(ExportFormat.PDF))
                onAction(ToolbarAction.ToggleMenu)
            }
            MenuItem(FeatherIcons.Image, stringResource(R.string.export_book_to, "PNG")) {
                onAction(ToolbarAction.ExportBook(ExportFormat.PNG))
                onAction(ToolbarAction.ToggleMenu)
            }
            MenuItem(FeatherIcons.File, stringResource(R.string.export_book_to, "xopp")) {
                onAction(ToolbarAction.ExportBook(ExportFormat.XOPP))
                onAction(ToolbarAction.ToggleMenu)
            }
            MenuItem(FeatherIcons.Link, stringResource(R.string.copy_book_link)) {
                onAction(ToolbarAction.CopyBookLink)
                onAction(ToolbarAction.ToggleMenu)
            }
            DividerCentered()
        }

        MenuItem(FeatherIcons.Trash2, stringResource(R.string.clean_all_strokes)) {
            onAction(ToolbarAction.ClearAllStrokes)
            onAction(ToolbarAction.ToggleMenu)
        }
        DividerCentered()

        MenuItem(FeatherIcons.Layout, stringResource(R.string.change_background)) {
            onAction(ToolbarAction.ToggleBackgroundSelector(true))
            onAction(ToolbarAction.ToggleMenu)
        }

        MenuItem(FeatherIcons.AlertCircle, stringResource(R.string.bug_report)) {
            onAction(ToolbarAction.NavigateToBugReport)
            onAction(ToolbarAction.ToggleMenu)
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .noRippleClickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.Black,
            fontSize = 16.sp,
            maxLines = 1
        )
    }
}

/** Hairline between menu groups. */
@Composable
private fun ColumnScope.DividerCentered() {
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Black)
    )
}

@Composable
@Preview(showBackground = true)
fun ToolbarMenuPreview() {
    ToolbarMenuContent(
        uiState = ToolbarUiState(
            isMenuOpen = true,
            notebookId = "book1",
            mode = Mode.Draw,
            pen = Pen.BALLPEN,
            penSettings = ToolbarPen.defaultPenSettings
        ),
        onAction = {}
    )
}
