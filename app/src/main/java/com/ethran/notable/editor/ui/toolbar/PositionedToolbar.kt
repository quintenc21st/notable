package com.ethran.notable.editor.ui.toolbar

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethran.notable.data.datastore.AppSettings
import com.ethran.notable.data.datastore.GlobalAppSettings
import com.ethran.notable.editor.EditorViewModel

/**
 * Where the toolbar being composed sits. Provided by [PositionedToolbar], so the toolbar's
 * elements and their popups can lay themselves out for a horizontal or a vertical bar.
 */
val LocalToolbarPosition = compositionLocalOf { AppSettings.Position.Top }

/**
 * The toolbar position for the way the device is held now: the portrait or the landscape
 * setting. Reads the configuration, so it recomposes on rotation.
 */
@Composable
fun toolbarPositionForOrientation(): AppSettings.Position {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return GlobalAppSettings.current.toolbarPositionFor(isLandscape)
}

/**
 * Where a popup opened from a toolbar button goes, as an alignment against the button plus an
 * offset: below it for a horizontal toolbar, beside it (toward the page) for a vertical one.
 */
@Composable
internal fun toolbarPopupPlacement(): Pair<Alignment, IntOffset> {
    val gap = with(LocalDensity.current) { 43.dp.roundToPx() }
    return when (LocalToolbarPosition.current) {
        AppSettings.Position.Left -> Alignment.TopStart to IntOffset(gap, 0)
        AppSettings.Position.Right -> Alignment.TopEnd to IntOffset(-gap, 0)
        else -> Alignment.TopCenter to IntOffset(0, gap)
    }
}

/**
 * Container for the Toolbar that handles its positioning (top, bottom, left or right edge).
 *
 * This component is now decoupled from navigation and engine logic,
 * delegating actions to the [EditorViewModel].
 */
@Composable
fun PositionedToolbar(
    viewModel: EditorViewModel,
    onDrawingStateCheck: () -> Unit
) {
    val position = toolbarPositionForOrientation()
    val toolbarState by viewModel.toolbarState.collectAsStateWithLifecycle()

    val toolbar = @Composable {
        ToolbarContent(
            uiState = toolbarState,
            onAction = viewModel::onToolbarAction,
            onDrawingStateCheck = onDrawingStateCheck
        )
    }

    CompositionLocalProvider(LocalToolbarPosition provides position) {
        when (position) {
            AppSettings.Position.Top, AppSettings.Position.Left -> toolbar()
            AppSettings.Position.Bottom -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    toolbar()
                }
            }

            AppSettings.Position.Right -> {
                Row(Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(1f))
                    toolbar()
                }
            }
        }
    }
}
