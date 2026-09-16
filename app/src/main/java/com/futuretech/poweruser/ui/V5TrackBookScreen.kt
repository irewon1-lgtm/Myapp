package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.TextbookBlock
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.V1TextbookCatalog
import com.futuretech.poweruser.textbook.V5BookAssetRepository
import com.futuretech.poweruser.textbook.V5BookManifest
import com.futuretech.poweruser.textbook.V5BookPartRef

private val V5Shell = Color(0xFF050608)
private val V5Paper = Color(0xFF101216)
private val V5Raised = Color(0xFF171A20)
private val V5Ink = Color(0xFFF2F3F5)
private val V5Muted = Color(0xFFA2A9B3)
private val V5Faint = Color(0xFF707782)
private val V5Rule = Color(0xFF292D34)
private val V5Accent = Color(0xFFA8C4E8)
private val V5Code = Color(0xFF08090B)

private val V5_TOP_HEIGHT = 32.dp
private val V5_PART_HEIGHT = 22.dp
private val V5_OUTER_X = 1.dp
private val V5_OUTER_Y = 1.dp
private val V5_PAGE_X = 14.dp
private val V5_PAGE_Y = 6.dp
private val V5_FOOTER_HEIGHT = 20.dp

/**
 * V5 book-scale reader.
 *
 * Only the currently visible PART is loaded from assets. Multi-megabyte TRACKs therefore do not
 * become one giant String or one giant parsed block list. Normal pages have no artificial cover,
 * recall or end-card page: authored prose begins immediately and consumes the viewport like an
 * e-book. Page breaks are calculated with the same Compose TextMeasurer used by the V4 measured
 * engine, not characters-per-line guesses.
 */
@Composable
fun V5TrackBookScreen(
    trackNumber: Int,
    onNavigateBack: () -> Unit,
    onOpenToc: (() -> Unit)?,
    onStartPractice: (String) -> Unit,
    onNextTrack: (() -> Unit)? = null,
    onPreviousTrack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember { TextbookProgressStore(context) }
    val repo = remember { V5BookAssetRepository(context) }
    val manifest = remember(trackNumber) { repo.loadManifest(trackNumber) }
    val legacyChapter = remember(trackNumber) {
        requireNotNull(V1TextbookCatalog.chapters.firstOrNull { it.number == trackNumber })
    }

    val restored = store.selectedSectionIndex(legacyChapter.id)
        .coerceIn(0, manifest.parts.lastIndex.coerceAtLeast(0))
    var partIndex by rememberSaveable(trackNumber) { mutableIntStateOf(restored) }
    val part = manifest.parts[partIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(V5Shell)
            .testTag("v5_book_root")
    ) {
        V5TopBar(
            manifest = manifest,
            onNavigateBack = onNavigateBack,
            onOpenToc = onOpenToc
        )
        V5PartStrip(part, partIndex, manifest.parts.size)

        V5MeasuredPartReader(
            modifier = Modifier.weight(1f),
            repo = repo,
            part = part,
            partIndex = partIndex,
            partCount = manifest.parts.size,
            initialPageIndex = store.selectedPageIndex(part.id),
            onPageChanged = { store.saveSelectedPageIndex(part.id, it) },
            onPrevious = {
                when {
                    partIndex > 0 -> {
                        val target = manifest.parts[partIndex - 1]
                        store.saveSelectedSectionIndex(legacyChapter.id, partIndex - 1)
                        store.saveSelectedPageIndex(target.id, Int.MAX_VALUE)
                        partIndex -= 1
                    }
                    onPreviousTrack != null -> onPreviousTrack()
                }
            },
            onNext = {
                when {
                    partIndex < manifest.parts.lastIndex -> {
                        val target = manifest.parts[partIndex + 1]
                        store.saveSelectedSectionIndex(legacyChapter.id, partIndex + 1)
                        store.saveSelectedPageIndex(target.id, 0)
                        partIndex += 1
                    }
                    onNextTrack != null -> onNextTrack()
                }
            },
            onPractice = { onStartPractice(legacyChapter.practiceLessonId) }
        )
    }
}

@Composable
private fun V5TopBar(
    manifest: V5BookManifest,
    onNavigateBack: () -> Unit,
    onOpenToc: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(V5_TOP_HEIGHT)
            .background(V5Shell)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "‹ 서재",
            modifier = Modifier
                .clickable(onClick = onNavigateBack)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            color = V5Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "TRACK ${manifest.trackNumber.toString().padStart(2, '0')}  ·  ${manifest.title}",
            modifier = Modifier.weight(1f),
            color = V5Ink,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            "목차",
            modifier = Modifier
                .clickable(enabled = onOpenToc != null) { onOpenToc?.invoke() }
                .padding(horizontal = 7.dp, vertical = 6.dp)
                .testTag("v5_reader_toc"),
            color = V5Accent,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun V5PartStrip(part: V5BookPartRef, partIndex: Int, partCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(V5_PART_HEIGHT)
            .background(V5Shell)
            .padding(horizontal = 11.dp)
            .testTag("v5_part_strip"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${partIndex + 1}/$partCount",
            color = V5Accent,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(7.dp))
        Text(
            part.title,
            modifier = Modifier.weight(1f),
            color = V5Muted,
            fontSize = 9.5.sp,
            maxLines = 1
        )
    }
    HorizontalDivider(color = V5Rule)
}

@Composable
private fun V5MeasuredPartReader(
    modifier: Modifier,
    repo: V5BookAssetRepository,
    part: V5BookPartRef,
    partIndex: Int,
    partCount: Int,
    initialPageIndex: Int,
    onPageChanged: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPractice: () -> Unit
) {
    val blocks = remember(part.id) {
        // Source map is loaded together with prose so missing/unknown evidence fails fast before a
        // learner can read an untraceable PART.
        repo.loadSourceMap(part)
        repo.loadPart(part)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(V5Shell)
            .testTag("v5_part_reader")
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer(cacheSize = 192)
        val pageWidth = maxWidth - V5_OUTER_X * 2 - V5_PAGE_X * 2
        val pageHeight = maxHeight - V5_OUTER_Y * 2 - V5_PAGE_Y * 2 - V5_FOOTER_HEIGHT
        val widthPx = with(density) { pageWidth.roundToPx().coerceAtLeast(1) }
        val heightPx = with(density) { pageHeight.roundToPx().coerceAtLeast(1) }

        val measured = remember(
            part.id,
            blocks,
            widthPx,
            heightPx,
            density.density,
            density.fontScale,
            textMeasurer
        ) {
            MeasuredTextbookPageComposer.paginate(
                blocks = blocks,
                contentWidthPx = widthPx,
                contentHeightPx = heightPx,
                density = density,
                textMeasurer = textMeasurer
            )
        }

        val safeStart = when {
            measured.isEmpty() -> 0
            initialPageIndex == Int.MAX_VALUE -> measured.lastIndex
            else -> initialPageIndex.coerceIn(0, measured.lastIndex)
        }
        var pageIndex by rememberSaveable(part.id, widthPx, heightPx, density.fontScale) {
            mutableIntStateOf(safeStart)
        }
        val page = measured[pageIndex]
        val swipeThreshold = with(density) { 46.dp.toPx() }

        fun previousPage() {
            if (pageIndex > 0) pageIndex -= 1 else onPrevious()
        }

        fun nextPage() {
            if (pageIndex < measured.lastIndex) pageIndex += 1 else onNext()
        }

        LaunchedEffect(part.id, pageIndex) { onPageChanged(pageIndex) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = V5_OUTER_X, vertical = V5_OUTER_Y)
                .pointerInput(part.id, pageIndex, measured.size) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onHorizontalDrag = { _, delta -> total += delta },
                        onDragEnd = {
                            when {
                                total <= -swipeThreshold -> nextPage()
                                total >= swipeThreshold -> previousPage()
                            }
                            total = 0f
                        },
                        onDragCancel = { total = 0f }
                    )
                }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = V5Paper,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, V5Rule)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = V5_PAGE_X, vertical = V5_PAGE_Y)
                ) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            page.content.blocks.forEach { block ->
                                V5BookBlock(block, Modifier.fillMaxWidth())
                            }
                        }
                    }
                    V5Footer(
                        partIndex = partIndex,
                        partCount = partCount,
                        pageIndex = pageIndex,
                        pageCount = measured.size,
                        utilization = page.utilization,
                        onPractice = onPractice
                    )
                }
            }

            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(44.dp)
                    .clickable { previousPage() }
                    .testTag("v5_left_tap_zone")
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(44.dp)
                    .clickable { nextPage() }
                    .testTag("v5_right_tap_zone")
            )
        }
    }
}

@Composable
private fun V5Footer(
    partIndex: Int,
    partCount: Int,
    pageIndex: Int,
    pageCount: Int,
    utilization: Double,
    onPractice: () -> Unit
) {
    HorizontalDivider(color = V5Rule)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(V5_FOOTER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "PART ${partIndex + 1}/$partCount",
            color = V5Faint,
            fontSize = 8.sp
        )
        Text(
            "실습",
            modifier = Modifier
                .clickable(onClick = onPractice)
                .padding(horizontal = 7.dp, vertical = 2.dp),
            color = V5Accent,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${pageIndex + 1} / $pageCount",
            modifier = Modifier.testTag("v5_page_indicator"),
            color = V5Muted,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${(utilization * 100).toInt().coerceAtMost(999)}%",
            modifier = Modifier.testTag("v5_page_utilization"),
            color = V5Faint.copy(alpha = 0f),
            fontSize = 1.sp
        )
    }
}

@Composable
private fun V5BookBlock(block: TextbookBlock, modifier: Modifier) {
    when (block) {
        is TextbookBlock.Heading -> {
            val size = when (block.level) {
                1 -> 24.sp
                2 -> 21.sp
                3 -> 18.sp
                4 -> 16.sp
                else -> 15.sp
            }
            Text(
                block.text,
                modifier = modifier.padding(top = if (block.level <= 3) 4.dp else 1.dp),
                color = V5Ink,
                fontSize = size,
                lineHeight = (size.value + 7).sp,
                fontWeight = if (block.level <= 3) FontWeight.Bold else FontWeight.SemiBold
            )
        }
        is TextbookBlock.Paragraph -> Text(
            block.text,
            modifier = modifier,
            color = V5Ink,
            fontSize = 16.5.sp,
            lineHeight = 27.sp
        )
        is TextbookBlock.BulletList -> Column(
            modifier = modifier.padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            block.items.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        if (block.ordered) "${index + 1}." else "•",
                        modifier = Modifier.width(28.dp),
                        color = V5Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item,
                        modifier = Modifier.weight(1f),
                        color = V5Ink,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )
                }
            }
        }
        is TextbookBlock.Code -> Surface(
            modifier = modifier,
            color = V5Code,
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.dp, V5Rule)
        ) {
            Column(Modifier.fillMaxWidth()) {
                if (block.language.isNotBlank()) {
                    Text(
                        block.language.uppercase(),
                        color = V5Accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                    HorizontalDivider(color = V5Rule)
                }
                Text(
                    block.text,
                    color = Color(0xFFE4E8EE),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
        is TextbookBlock.Table -> Surface(
            modifier = modifier,
            color = V5Raised,
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.dp, V5Rule)
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                block.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) HorizontalDivider(color = V5Rule)
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        block.headers.forEachIndexed { columnIndex, header ->
                            row.getOrNull(columnIndex)?.takeIf(String::isNotBlank)?.let { value ->
                                Text(header, color = V5Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(value, color = V5Ink, fontSize = 13.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }
        }
        TextbookBlock.Divider -> HorizontalDivider(modifier = modifier.padding(vertical = 3.dp), color = V5Rule)
    }
}
