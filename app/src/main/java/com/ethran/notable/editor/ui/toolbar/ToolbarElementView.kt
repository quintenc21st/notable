package com.ethran.notable.editor.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.ethran.notable.R
import com.ethran.notable.data.datastore.BUTTON_SIZE
import com.ethran.notable.data.datastore.GlobalAppSettings
import com.ethran.notable.editor.ToolbarAction
import com.ethran.notable.editor.ToolbarUiState
import com.ethran.notable.editor.state.Mode
import com.ethran.notable.editor.ui.toolbar.model.ActionElement
import com.ethran.notable.editor.ui.toolbar.model.CustomElement
import com.ethran.notable.editor.ui.toolbar.model.CustomKind
import com.ethran.notable.editor.ui.toolbar.model.DividerElement
import com.ethran.notable.editor.ui.toolbar.model.EraserSubmenuSpec
import com.ethran.notable.editor.ui.toolbar.model.IconRef
import com.ethran.notable.editor.ui.toolbar.model.ModeElement
import com.ethran.notable.editor.ui.toolbar.model.PenElement
import com.ethran.notable.editor.ui.toolbar.model.ShapeElement
import com.ethran.notable.editor.ui.toolbar.model.ToolbarElement
import com.ethran.notable.editor.utils.Eraser
import com.ethran.notable.ui.components.OnOffSwitch
import com.ethran.notable.ui.components.ScaledPopup
import com.ethran.notable.ui.noRippleClickable
import com.ethran.notable.ui.theme.NotableStyle
import com.ethran.notable.ui.theme.notablePanel

/**
 * The single generic renderer for toolbar elements: draws the button (via [ToolbarButton]),
 * handles selected state, and opens the element's declared submenu. Stateless except
 * transient popup-open state; all real mutation flows through [ToolbarAction].
 *
 * [onPickImage] exists because the image picker's activity-result launcher is Compose
 * infrastructure owned by ToolbarContent, not something a [ToolbarAction] can express.
 */
@Composable
fun ToolbarElementView(
    element: ToolbarElement,
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
    onPickImage: () -> Unit,
) {
    when (element) {
        is DividerElement -> ToolbarVerticalDivider()

        is PenElement -> PenElementView(element, uiState, onAction)

        is ShapeElement ->
            // One shape (LINE) for now: the picker submenu is stubbed to a plain toggle,
            // matching the old LineToolbarButton (click again deselects back to Draw).
            ToolbarButton(
                isSelected = element.isSelected(uiState),
                onSelect = {
                    onAction(
                        ToolbarAction.ChangeMode(
                            if (element.isSelected(uiState)) Mode.Draw else Mode.Line
                        )
                    )
                },
                penColor = Color.LightGray,
                iconId = (element.icon as? IconRef.Drawable)?.resId,
                vectorIcon = (element.icon as? IconRef.Vector)?.imageVector,
                contentDescription = element.contentDescription,
            )

        is ModeElement -> ModeElementView(element, uiState, onAction)

        is ActionElement ->
            ToolbarButton(
                isSelected = element.isSelected(uiState),
                onSelect = { onAction(element.action) },
                iconId = (element.icon as? IconRef.Drawable)?.resId,
                vectorIcon = (element.icon as? IconRef.Vector)?.imageVector,
                contentDescription = element.contentDescription,
            )

        is CustomElement -> when (element.kind) {
            CustomKind.IMAGE_PICKER ->
                ToolbarButton(
                    iconId = (element.icon as? IconRef.Drawable)?.resId,
                    vectorIcon = (element.icon as? IconRef.Vector)?.imageVector,
                    contentDescription = element.contentDescription,
                    onSelect = onPickImage,
                )

            CustomKind.PAGE_NAV -> PageNavButton(uiState, onAction)

            CustomKind.MENU ->
                Column {
                    ToolbarButton(
                        onSelect = { onAction(ToolbarAction.ToggleMenu) },
                        iconId = (element.icon as? IconRef.Drawable)?.resId,
                        contentDescription = element.contentDescription,
                    )
                    if (uiState.isMenuOpen) {
                        ToolbarMenu(
                            uiState = uiState,
                            onAction = onAction
                        )
                    }
                }
        }
    }
}

@Composable
private fun PenElementView(
    element: PenElement,
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
) {
    var isStrokeMenuOpen by remember { mutableStateOf(false) }
    val isSelected = element.isSelected(uiState)
    val penSetting =
        uiState.penSettings[element.presetId] ?: element.setting.copy()

    Box {
        ToolbarButton(
            isSelected = isSelected,
            onSelect = {
                if (isSelected) isStrokeMenuOpen = !isStrokeMenuOpen
                else onAction(ToolbarAction.ChangePen(element.presetId))
            },
            penColor = Color(penSetting.color),
            iconId = (element.icon as? IconRef.Drawable)?.resId,
            vectorIcon = (element.icon as? IconRef.Vector)?.imageVector,
            contentDescription = element.contentDescription,
        )

        if (isStrokeMenuOpen) {
            StrokeMenu(
                value = penSetting,
                onChange = { onAction(ToolbarAction.ChangePenSetting(element.presetId, it)) },
                onClose = { isStrokeMenuOpen = false },
                sizeOptions = element.submenu.sizeOptions,
                colorOptions = element.submenu.colorOptions,
            )
        }
    }
}

@Composable
private fun ModeElementView(
    element: ModeElement,
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
) {
    val isSelected = element.isSelected(uiState)
    val submenu = element.submenu

    // The eraser button reflects the active eraser type, not its static spec icon.
    val iconId =
        if (submenu is EraserSubmenuSpec) eraserIcon(uiState.eraser)
        else (element.icon as? IconRef.Drawable)?.resId

    Box {
        ToolbarButton(
            isSelected = isSelected,
            onSelect = {
                if (isSelected && submenu is EraserSubmenuSpec)
                    onAction(ToolbarAction.ToggleEraserManu(!uiState.isStrokeSelectionOpen))
                else if (!isSelected)
                    onAction(ToolbarAction.ChangeMode(element.mode))
            },
            iconId = iconId,
            vectorIcon = (element.icon as? IconRef.Vector)?.imageVector,
            contentDescription = element.contentDescription,
        )

        if (submenu is EraserSubmenuSpec && uiState.isStrokeSelectionOpen) {
            EraserSubmenu(
                spec = submenu,
                uiState = uiState,
                onAction = onAction,
            )
        }
    }
}

private fun eraserIcon(eraser: Eraser): Int =
    when (eraser) {
        Eraser.PEN -> R.drawable.eraser
        Eraser.SELECT -> R.drawable.eraser_select
    }

/** The eraser popup: eraser-type picker plus the global scribble-to-erase toggle. */
@Composable
private fun EraserSubmenu(
    spec: EraserSubmenuSpec,
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
) {
    val (alignment, offset) = toolbarPopupPlacement()

    ScaledPopup(
        offset = offset,
        onDismissRequest = {
            onAction(ToolbarAction.ToggleEraserManu(false))
        },
        properties = PopupProperties(focusable = true),
        alignment = alignment
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(bottom = (BUTTON_SIZE + 5).dp)
                .notablePanel()
                .height(IntrinsicSize.Max)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.toolbar_eraser),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                spec.erasers.forEach { eraser ->
                    ToolbarButton(
                        iconId = eraserIcon(eraser),
                        isSelected = uiState.eraser == eraser,
                        onSelect = {
                            onAction(ToolbarAction.ChangeEraser(eraser))
                        },
                        modifier = Modifier.height(BUTTON_SIZE.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = stringResource(
                        R.string.toolbar_scribble_to_erase_two_lined_short
                    ),
                    modifier = Modifier.padding(end = 6.dp),
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                )

                val initialState =
                    GlobalAppSettings.current.scribbleToEraseEnabled

                var isChecked by remember {
                    mutableStateOf(initialState)
                }

                Spacer(modifier = Modifier.width(15.dp))

                OnOffSwitch(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        isChecked = checked
                        onAction(
                            ToolbarAction.ToggleScribbleToErase(checked)
                        )
                    }
                )
            }
        }
    }
}

/**
 * The page number, as a pill so it reads as a button; filled with the accent while the pages
 * popup it opens is showing. On a side toolbar there is no room for "12/34" on one line, so
 * the page sits above the total.
 */
@Composable
private fun PageNavButton(
    uiState: ToolbarUiState,
    onAction: (ToolbarAction) -> Unit,
) {
    val open = uiState.isPagesPopupOpen
    val content = if (open) NotableStyle.OnAccent else Color.Black
    val pill = Modifier
        .noRippleClickable { onAction(ToolbarAction.TogglePagesPopup(!open)) }
        .background(if (open) NotableStyle.Accent else Color.White, NotableStyle.ControlShape)
        .border(
            1.dp, if (open) NotableStyle.Accent else Color.Black, NotableStyle.ControlShape
        )

    if (LocalToolbarPosition.current.isVertical) {
        val current = uiState.pageNumberInfo.substringBefore('/')
        val total = uiState.pageNumberInfo.substringAfter('/', "")
        Column(
            modifier = Modifier
                .padding(horizontal = 3.dp, vertical = 4.dp)
                .fillMaxWidth()
                .then(pill)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = current,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = content,
                maxLines = 1
            )
            if (total.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(vertical = 2.dp)
                        .width(14.dp)
                        .height(1.dp)
                        .background(content)
                )
                Text(text = total, fontSize = 11.sp, color = content, maxLines = 1)
            }
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(BUTTON_SIZE.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .then(pill)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = uiState.pageNumberInfo,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = content,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Separator between toolbar groups: across the bar, whichever way the bar runs. */
@Composable
internal fun ToolbarVerticalDivider() {
    if (LocalToolbarPosition.current.isVertical) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.Black)
        )
        return
    }
    Box(
        Modifier
            .fillMaxHeight()
            .width(0.5.dp)
            .background(Color.Black)
    )
}
