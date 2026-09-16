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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.textbook.LearningConcept
import com.futuretech.poweruser.textbook.TextbookBlock
import com.futuretech.poweruser.textbook.TextbookChapter
import com.futuretech.poweruser.textbook.TextbookContentPage
import com.futuretech.poweruser.textbook.TextbookLearningFlow
import com.futuretech.poweruser.textbook.TextbookMarkdownParser
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.TextbookSection
import com.futuretech.poweruser.textbook.TextbookSectioner
import com.futuretech.poweruser.textbook.V1TextbookCatalog
import com.futuretech.poweruser.textbook.V4BookDepthLibrary

private val BookShell = Color(0xFF050608)
private val BookPaper = Color(0xFF101216)
private val BookPaperRaised = Color(0xFF171A20)
private val BookInk = Color(0xFFF2F3F5)
private val BookMuted = Color(0xFFA2A9B3)
private val BookFaint = Color(0xFF707782)
private val BookRule = Color(0xFF292D34)
private val BookAccent = Color(0xFFA8C4E8)
private val BookCode = Color(0xFF08090B)

private val BOOK_TOP_BAR_HEIGHT = 34.dp
private val BOOK_LESSON_STRIP_HEIGHT = 24.dp
private val BOOK_OUTER_HORIZONTAL_PADDING = 2.dp
private val BOOK_OUTER_VERTICAL_PADDING = 1.dp
private val BOOK_PAGE_HORIZONTAL_PADDING = 16.dp
private val BOOK_PAGE_VERTICAL_PADDING = 8.dp
private val BOOK_FOOTER_HEIGHT = 22.dp
private const val OPEN_LAST_PAGE_V4 = Int.MAX_VALUE

private sealed interface V4ReaderPage {
    data object LessonCover : V4ReaderPage
    data class Content(val page: TextbookContentPage, val utilization: Double) : V4ReaderPage
    data object Recall : V4ReaderPage
    data object LessonEnd : V4ReaderPage
}

@Composable
fun V4PagedBookScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    initialChapterId: String = "V1-C01",
    onOpenToc: (() -> Unit)? = null,
    onChapterSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember { TextbookProgressStore(context) }
    val safeInitial = initialChapterId.takeIf { V1TextbookCatalog.chapterById(it) != null }
        ?: store.selectedChapterId()?.takeIf { V1TextbookCatalog.chapterById(it) != null }
        ?: "V1-C01"
    var selectedId by rememberSaveable { mutableStateOf(safeInitial) }
    var readCompleted by remember { mutableStateOf(store.readCompletedIds()) }
    val chapter = V1TextbookCatalog.chapterById(selectedId) ?: V1TextbookCatalog.chapters.first()
    val lesson = remember(chapter.practiceLessonId) {
        requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId)) {
            "Missing textbook practice lesson ${chapter.practiceLessonId}"
        }
    }

    fun selectChapter(id: String) {
        if (V1TextbookCatalog.chapterById(id) != null) {
            selectedId = id
            store.saveSelectedChapter(id)
            onChapterSelected?.invoke(id)
        }
    }

    Scaffold(
        modifier = Modifier.testTag("v1_textbook_root"),
        containerColor = BookShell,
        topBar = {
            V4BookTopBar(chapter = chapter, onNavigateBack = onNavigateBack, onOpenToc = onOpenToc)
        }
    ) { scaffoldPadding ->
        val markdown = remember(chapter.id) {
            context.assets.open(chapter.assetPath).bufferedReader().use { it.readText() }
        }
        val blocks = remember(markdown) { TextbookMarkdownParser.parse(markdown) }
        val sections = remember(chapter.id, blocks) { TextbookSectioner.split(chapter.id, blocks) }
        val restoredSection = store.selectedSectionIndex(chapter.id)
            .coerceIn(0, sections.lastIndex.coerceAtLeast(0))
        var sectionIndex by rememberSaveable(chapter.id) { mutableIntStateOf(restoredSection) }
        val section = sections.getOrNull(sectionIndex) ?: return@Scaffold

        val concepts = remember(chapter.id, section.id, lesson.lessonId) {
            TextbookLearningFlow.buildConcepts(chapter.id, section, lesson)
        }
        val restoredConcept = store.selectedConceptIndex(section.id)
            .coerceIn(0, concepts.lastIndex.coerceAtLeast(0))
        var conceptIndex by rememberSaveable(section.id) { mutableIntStateOf(restoredConcept) }
        val concept = concepts.getOrNull(conceptIndex) ?: return@Scaffold

        Column(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding).background(BookShell)
        ) {
            V4LessonStrip(
                section = section,
                sectionCount = sections.size,
                extraMinutes = V4BookDepthLibrary.extraEstimatedMinutes(section.id)
            )

            key(chapter.id, section.id, concept.id) {
                V4BookReader(
                    modifier = Modifier.weight(1f),
                    chapter = chapter,
                    section = section,
                    sectionCount = sections.size,
                    concept = concept,
                    readComplete = chapter.id in readCompleted,
                    practiceComplete = chapter.practiceLessonId in practiceCompletedIds,
                    initialPageIndex = store.selectedPageIndex(concept.id),
                    onPageChanged = { store.saveSelectedPageIndex(concept.id, it) },
                    onPreviousLesson = when {
                        conceptIndex > 0 -> {
                            {
                                val target = concepts[conceptIndex - 1]
                                store.saveSelectedPageIndex(target.id, OPEN_LAST_PAGE_V4)
                                conceptIndex -= 1
                                store.saveSelectedConceptIndex(section.id, conceptIndex)
                            }
                        }
                        sectionIndex > 0 -> {
                            {
                                val previousSection = sections[sectionIndex - 1]
                                val previousConcepts = TextbookLearningFlow.buildConcepts(chapter.id, previousSection, lesson)
                                store.saveSelectedConceptIndex(previousSection.id, previousConcepts.lastIndex.coerceAtLeast(0))
                                previousConcepts.lastOrNull()?.let {
                                    store.saveSelectedPageIndex(it.id, OPEN_LAST_PAGE_V4)
                                }
                                sectionIndex -= 1
                                store.saveSelectedSectionIndex(chapter.id, sectionIndex)
                            }
                        }
                        else -> null
                    },
                    onNextLesson = when {
                        conceptIndex < concepts.lastIndex -> {
                            {
                                val target = concepts[conceptIndex + 1]
                                store.saveSelectedPageIndex(target.id, 0)
                                conceptIndex += 1
                                store.saveSelectedConceptIndex(section.id, conceptIndex)
                            }
                        }
                        sectionIndex < sections.lastIndex -> {
                            {
                                val nextSection = sections[sectionIndex + 1]
                                val nextConcepts = TextbookLearningFlow.buildConcepts(chapter.id, nextSection, lesson)
                                store.saveSelectedConceptIndex(nextSection.id, 0)
                                nextConcepts.firstOrNull()?.let { store.saveSelectedPageIndex(it.id, 0) }
                                sectionIndex += 1
                                store.saveSelectedSectionIndex(chapter.id, sectionIndex)
                            }
                        }
                        else -> null
                    },
                    onMarkRead = {
                        store.markReadComplete(chapter.id)
                        readCompleted = store.readCompletedIds()
                    },
                    onPractice = { onStartPractice(chapter.practiceLessonId) },
                    onPreviousTrack = V1TextbookCatalog.chapters.getOrNull(chapter.number - 2)?.let { previous ->
                        { selectChapter(previous.id) }
                    },
                    onNextTrack = V1TextbookCatalog.chapters.getOrNull(chapter.number)?.let { next ->
                        { selectChapter(next.id) }
                    }
                )
            }
        }
    }
}

@Composable
private fun V4BookTopBar(
    chapter: TextbookChapter,
    onNavigateBack: () -> Unit,
    onOpenToc: (() -> Unit)?
) {
    Surface(color = BookShell) {
        Row(
            modifier = Modifier.fillMaxWidth().height(BOOK_TOP_BAR_HEIGHT).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹ 서재",
                modifier = Modifier.clickable(onClick = onNavigateBack).padding(horizontal = 6.dp, vertical = 7.dp),
                color = BookInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "TRACK ${chapter.number.toString().padStart(2, '0')} / ${V1TextbookCatalog.TRACK_COUNT}",
                    color = BookFaint,
                    fontSize = 8.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    chapter.title,
                    color = BookInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                "목차",
                modifier = Modifier
                    .clickable(enabled = onOpenToc != null) { onOpenToc?.invoke() }
                    .padding(horizontal = 7.dp, vertical = 7.dp)
                    .testTag("reader_toc_button"),
                color = BookAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun V4LessonStrip(section: TextbookSection, sectionCount: Int, extraMinutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BOOK_LESSON_STRIP_HEIGHT)
            .background(BookShell)
            .padding(horizontal = 12.dp)
            .testTag("textbook_section_strip"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${section.index + 1}/$sectionCount",
            color = BookAccent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(7.dp))
        Text(
            section.title,
            modifier = Modifier.weight(1f),
            color = BookInk,
            fontSize = 10.sp,
            maxLines = 1
        )
        Text("${section.estimatedMinutes + extraMinutes}분", color = BookFaint, fontSize = 9.sp)
    }
    HorizontalDivider(color = BookRule)
}

@Composable
private fun V4BookReader(
    modifier: Modifier,
    chapter: TextbookChapter,
    section: TextbookSection,
    sectionCount: Int,
    concept: LearningConcept,
    readComplete: Boolean,
    practiceComplete: Boolean,
    initialPageIndex: Int,
    onPageChanged: (Int) -> Unit,
    onPreviousLesson: (() -> Unit)?,
    onNextLesson: (() -> Unit)?,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit,
    onPreviousTrack: (() -> Unit)?,
    onNextTrack: (() -> Unit)?
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(BookShell).testTag("textbook_reader")
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer(cacheSize = 128)
        val measuredWidthDp = maxWidth - BOOK_OUTER_HORIZONTAL_PADDING * 2 - BOOK_PAGE_HORIZONTAL_PADDING * 2
        val measuredHeightDp = maxHeight - BOOK_OUTER_VERTICAL_PADDING * 2 - BOOK_PAGE_VERTICAL_PADDING * 2 - BOOK_FOOTER_HEIGHT
        val contentWidthPx = with(density) { measuredWidthDp.roundToPx().coerceAtLeast(1) }
        val contentHeightPx = with(density) { measuredHeightDp.roundToPx().coerceAtLeast(1) }

        val measuredPages = remember(
            concept.id,
            concept.blocks,
            contentWidthPx,
            contentHeightPx,
            density.density,
            density.fontScale,
            textMeasurer
        ) {
            MeasuredTextbookPageComposer.paginate(
                blocks = concept.blocks,
                contentWidthPx = contentWidthPx,
                contentHeightPx = contentHeightPx,
                density = density,
                textMeasurer = textMeasurer
            )
        }
        val pages = remember(concept.id, measuredPages) {
            buildList<V4ReaderPage> {
                add(V4ReaderPage.LessonCover)
                measuredPages.forEach { measured -> add(V4ReaderPage.Content(measured.content, measured.utilization)) }
                add(V4ReaderPage.Recall)
                add(V4ReaderPage.LessonEnd)
            }
        }
        val start = initialPageIndex.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        var pageIndex by rememberSaveable(concept.id, contentWidthPx, contentHeightPx, density.fontScale) {
            mutableIntStateOf(start)
        }
        val page = pages[pageIndex]
        val swipeThresholdPx = with(density) { 48.dp.toPx() }
        val isLastLessonInTrack = section.index == sectionCount - 1

        fun previous() {
            when {
                pageIndex > 0 -> pageIndex -= 1
                onPreviousLesson != null -> onPreviousLesson()
                onPreviousTrack != null -> onPreviousTrack()
            }
        }

        fun next() {
            when {
                pageIndex < pages.lastIndex -> pageIndex += 1
                onNextLesson != null -> onNextLesson()
                isLastLessonInTrack && onNextTrack != null -> onNextTrack()
            }
        }

        LaunchedEffect(concept.id, pageIndex) { onPageChanged(pageIndex) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = BOOK_OUTER_HORIZONTAL_PADDING, vertical = BOOK_OUTER_VERTICAL_PADDING)
                .pointerInput(concept.id, pageIndex, pages.size) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onHorizontalDrag = { _, amount -> total += amount },
                        onDragEnd = {
                            when {
                                total <= -swipeThresholdPx -> next()
                                total >= swipeThresholdPx -> previous()
                            }
                            total = 0f
                        },
                        onDragCancel = { total = 0f }
                    )
                }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().align(Alignment.Center).testTag("textbook_page_surface"),
                color = BookPaper,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, BookRule)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = BOOK_PAGE_HORIZONTAL_PADDING, vertical = BOOK_PAGE_VERTICAL_PADDING)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (page) {
                            V4ReaderPage.LessonCover -> V4LessonCoverPage(
                                chapter = chapter,
                                section = section,
                                sectionCount = sectionCount,
                                concept = concept,
                                extraMinutes = V4BookDepthLibrary.extraEstimatedMinutes(section.id)
                            )
                            is V4ReaderPage.Content -> V4ContentBookPage(page.page)
                            V4ReaderPage.Recall -> V4RecallPage(concept, onPractice)
                            V4ReaderPage.LessonEnd -> V4LessonEndPage(
                                isLastLessonInTrack = isLastLessonInTrack,
                                readComplete = readComplete,
                                practiceComplete = practiceComplete,
                                onNextLesson = onNextLesson,
                                onMarkRead = onMarkRead,
                                onPractice = onPractice,
                                onPreviousLesson = onPreviousLesson,
                                onNextTrack = onNextTrack,
                                onPreviousTrack = onPreviousTrack
                            )
                        }
                    }
                    V4PageFooter(section, pageIndex, pages.size)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(44.dp)
                    .clickable { previous() }
                    .testTag("textbook_left_tap_zone")
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(44.dp)
                    .clickable { next() }
                    .testTag("textbook_right_tap_zone")
            )
        }
    }
}

@Composable
private fun V4PageFooter(section: TextbookSection, pageIndex: Int, pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(BOOK_FOOTER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            section.title,
            modifier = Modifier.weight(1f),
            color = BookFaint,
            fontSize = 8.sp,
            maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${pageIndex + 1} / $pageCount",
            modifier = Modifier.testTag("textbook_page_indicator"),
            color = BookMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun V4LessonCoverPage(
    chapter: TextbookChapter,
    section: TextbookSection,
    sectionCount: Int,
    concept: LearningConcept,
    extraMinutes: Int
) {
    Column(
        modifier = Modifier.fillMaxSize().testTag("textbook_lesson_cover"),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "TRACK ${chapter.number.toString().padStart(2, '0')}",
            color = BookAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "LESSON ${section.index + 1}",
            modifier = Modifier.testTag("textbook_section_progress"),
            color = BookMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            concept.title,
            modifier = Modifier.testTag("textbook_section_title"),
            color = BookInk,
            fontSize = 29.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = BookRule)
        Spacer(Modifier.height(12.dp))
        Text(chapter.summary, color = BookMuted, fontSize = 15.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("${section.index + 1}/$sectionCount", color = BookFaint, fontSize = 10.sp)
            Text("약 ${section.estimatedMinutes + extraMinutes}분", color = BookFaint, fontSize = 10.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("오른쪽 끝 터치 · 왼쪽으로 밀기 → 다음 장", color = BookAccent, fontSize = 10.sp)
    }
}

@Composable
private fun V4ContentBookPage(page: TextbookContentPage) {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            page.blocks.forEach { block -> V4BookBlock(block, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun V4BookBlock(block: TextbookBlock, modifier: Modifier) {
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
                text = block.text,
                modifier = modifier.padding(top = if (block.level <= 3) 4.dp else 1.dp),
                color = BookInk,
                fontSize = size,
                lineHeight = (size.value + 7).sp,
                fontWeight = if (block.level <= 3) FontWeight.Bold else FontWeight.SemiBold
            )
        }

        is TextbookBlock.Paragraph -> Text(
            text = block.text,
            modifier = modifier,
            color = BookInk,
            fontSize = 16.5.sp,
            lineHeight = 27.sp
        )

        is TextbookBlock.BulletList -> Column(
            modifier = modifier.padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            block.items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        if (block.ordered) "${block.startNumber + index}." else "•",
                        modifier = Modifier.width(28.dp),
                        color = BookAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item,
                        modifier = Modifier.weight(1f),
                        color = BookInk,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )
                }
            }
        }

        is TextbookBlock.Code -> V4CodeBlock(block, modifier)

        is TextbookBlock.Table -> Surface(
            modifier = modifier,
            color = BookPaperRaised,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, BookRule)
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                block.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) HorizontalDivider(color = BookRule)
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        block.headers.forEachIndexed { columnIndex, header ->
                            row.getOrNull(columnIndex)?.takeIf { it.isNotBlank() }?.let { value ->
                                Text(header, color = BookAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(value, color = BookInk, fontSize = 13.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }
        }

        TextbookBlock.Divider -> HorizontalDivider(
            modifier = modifier.padding(vertical = 3.dp),
            color = BookRule
        )
    }
}

@Composable
private fun V4CodeBlock(block: TextbookBlock.Code, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = BookCode,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, BookRule)
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (block.language.isNotBlank()) {
                Text(
                    block.language.uppercase(),
                    color = BookAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
                HorizontalDivider(color = BookRule)
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
}

@Composable
private fun V4RecallPage(concept: LearningConcept, onPractice: () -> Unit) {
    var answer by rememberSaveable(concept.problem.id) { mutableStateOf("") }
    var saved by rememberSaveable(concept.problem.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("책을 잠깐 덮고 기억에서 꺼내기", color = BookAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            concept.problem.prompt,
            color = BookInk,
            fontSize = 17.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("내 말로 설명") },
            minLines = 4,
            colors = v4ReaderTextFieldColors()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { saved = true },
                enabled = answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334F70), contentColor = BookInk)
            ) { Text("작성 완료") }
            OutlinedButton(onClick = onPractice) { Text("전체 실습", color = BookAccent) }
        }
        if (saved) {
            Spacer(Modifier.height(10.dp))
            Text(
                "정의만 반복하지 말고 원인·작동 순서·실패 조건까지 빠졌는지 확인하세요.",
                color = BookMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun V4LessonEndPage(
    isLastLessonInTrack: Boolean,
    readComplete: Boolean,
    practiceComplete: Boolean,
    onNextLesson: (() -> Unit)?,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit,
    onPreviousLesson: (() -> Unit)?,
    onNextTrack: (() -> Unit)?,
    onPreviousTrack: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            if (isLastLessonInTrack) "TRACK 읽기 완료" else "LESSON 읽기 완료",
            color = BookAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isLastLessonInTrack) "이제 문제·실습으로 실제 이해를 검증합니다."
            else "다음 LESSON에서 앞의 원리를 이어서 확장합니다.",
            color = BookInk,
            fontSize = 21.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (!isLastLessonInTrack) {
            Button(
                onClick = { onNextLesson?.invoke() },
                enabled = onNextLesson != null,
                modifier = Modifier.fillMaxWidth().testTag("textbook_next_section"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334F70), contentColor = BookInk)
            ) { Text("다음 LESSON →") }
        } else {
            Button(
                onClick = onMarkRead,
                enabled = !readComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334F70), contentColor = BookInk)
            ) { Text(if (readComplete) "TRACK 읽기 완료 ✓" else "이 TRACK 읽기 완료") }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onPractice,
            modifier = Modifier.fillMaxWidth().testTag("textbook_practice_button"),
            border = BorderStroke(1.dp, BookAccent)
        ) { Text(if (practiceComplete) "전체 문제·실습 다시 풀기 ✓" else "전체 문제·실습 열기", color = BookAccent) }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onPreviousLesson?.invoke() },
                enabled = onPreviousLesson != null,
                modifier = Modifier.weight(1f)
            ) { Text("← 이전 LESSON", color = BookInk) }
            if (isLastLessonInTrack) {
                OutlinedButton(
                    onClick = { onNextTrack?.invoke() },
                    enabled = onNextTrack != null,
                    modifier = Modifier.weight(1f).testTag("textbook_next_chapter")
                ) { Text("다음 TRACK", color = BookInk) }
            }
        }

        if (isLastLessonInTrack && onPreviousTrack != null) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onPreviousTrack) { Text("이전 TRACK으로 돌아가기", color = BookMuted) }
        }
    }
}

@Composable
private fun v4ReaderTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = BookInk,
    unfocusedTextColor = BookInk,
    disabledTextColor = BookMuted,
    focusedContainerColor = BookPaper,
    unfocusedContainerColor = BookPaper,
    cursorColor = BookAccent,
    focusedBorderColor = BookAccent,
    unfocusedBorderColor = BookRule,
    focusedLabelColor = BookAccent,
    unfocusedLabelColor = BookMuted
)
