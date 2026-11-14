package com.ethran.notable.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethran.notable.ui.theme.NotableStyle
import androidx.navigation.NavController
import com.ethran.notable.data.AppRepository
import com.ethran.notable.data.db.Page
import com.ethran.notable.editor.ui.PageMenu
import com.ethran.notable.editor.utils.autoEInkAnimationOnScroll
import com.ethran.notable.ui.noRippleClickable
import com.onyx.android.sdk.extension.isNullOrEmpty
import compose.icons.FeatherIcons
import compose.icons.feathericons.FilePlus
import io.shipbook.shipbooksdk.ShipBook


@Composable
fun ShowPagesRow(
    appRepository: AppRepository,
    pages: List<Page>?,
    currentPageId: String? = null,
    title: String? = "Quick Pages",
    onSelectPage: (String) -> Unit,
    showAddQuickPage: Boolean = false,
    onCreateNewQuickPage: () -> Unit = {},
    onPreviewMissing: (String) -> Unit = {},
) {

    if (title != null) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .autoEInkAnimationOnScroll()
    ) {
        // Add the "Add quick page" button
        if (showAddQuickPage) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(3f / 4f)
                        .clip(NotableStyle.SmallShape)
                        .border(1.5.dp, Color.Black, NotableStyle.SmallShape)
                        .noRippleClickable {
                            onCreateNewQuickPage()

                        }) {
                    Icon(
                        imageVector = FeatherIcons.FilePlus,
                        contentDescription = "Add Quick Page",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        // Render existing pages
        if (!pages.isNullOrEmpty()) {
            items(pages.reversed()) { page ->
                val pageId = page.id
                var isPageSelected by remember { mutableStateOf(false) }
                Box {
                    PagePreview(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    onSelectPage(pageId)
                                },
                                onLongClick = {
                                    isPageSelected = true
                                },
                            )
                            .width(120.dp)
                            .aspectRatio(3f / 4f)
                            .clip(NotableStyle.SmallShape)
                            .border(
                                if (currentPageId == pageId) 4.dp else 1.5.dp,
                                if (currentPageId == pageId) NotableStyle.Accent else Color.Black,
                                NotableStyle.SmallShape
                            ),
                        pageId = pageId,
                        onPreviewMissing = onPreviewMissing
                    )
                    if (isPageSelected) PageMenu(
                        appRepository = appRepository,
                        pageId = pageId, canDelete = true, onClose = { isPageSelected = false })
                }
            }
        }
    }
}