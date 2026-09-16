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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.textbook.InlineLearningProblem
import com.futuretech.poweruser.textbook.LearningConcept
import com.futuretech.poweruser.textbook.TextbookBlock
import com.futuretech.poweruser.textbook.TextbookChapter
import com.futuretech.poweruser.textbook.TextbookLearningFlow
import com.futuretech.poweruser.textbook.TextbookMarkdownParser
import com.futuretech.poweruser.textbook.TextbookPage
import com.futuretech.poweruser.textbook.TextbookPageMetrics
import com.futuretech.poweruser.textbook.TextbookPaginator
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.TextbookSection
import com.futuretech.poweruser.textbook.TextbookSectioner
import com.futuretech.poweruser.textbook.V1TextbookCatalog

private val BookShell = Color(0xFF090A0D)
private val BookPaper = Color(0xFF111318)
private val BookInk = Color(0xFFF4F6F8)
private val BookMuted = Color(0xFFAAB2BE)
private val BookBorder = Color(0xFF2A2F38)
private val BookAccent = Color(0xFF8AB4F8)
private val BookAction = Color(0xFF315A94)
private val BookSoft = Color(0xFF171C24)
private val BookCode = Color(0xFF07090C)
private val BookCodeText = Color(0xFFE9EDF3)

private sealed interface EbookPage {
    data object TrackCover : EbookPage
    data object LessonOpening : EbookPage
    data class Content(val page: TextbookPage) : EbookPage
    data object Recall : EbookPage
    data object TrackEnd : EbookPage
}

@Composable
fun EbookTextbookScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    initialChapterId: String? = null,
    onChapterChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { TextbookProgressStore(context) }
    val safeInitial = initialChapterId?.takeIf { V1TextbookCatalog.chapterById(it) != null }
        ?: store.selectedChapterId()?.takeIf { V1TextbookCatalog.chapterById(it) != null }
        ?: "V1-C01"
    var selectedId by rememberSaveable { mutableStateOf(safeInitial) }
    var readCompleted by remember { mutableStateOf(store.readCompletedIds()) }
    val selected = V1TextbookCatalog.chapterById(selectedId) ?: V1TextbookCatalog.chapters.first()
    val lesson = remember(selected.practiceLessonId) {
        requireNotNull(CurriculumDataRepository.lessonById(selected.practiceLessonId)) {
            "Missing textbook practice lesson ${selected.practiceLessonId}"
        }
    }

    LaunchedEffect(selectedId) {
        store.saveSelectedChapter(selectedId)
    }

    LaunchedEffect(initialChapterId) {
        val requested = initialChapterId?.takeIf { V1TextbookCatalog.chapterById(it) != null }
        if (requested != null && requested != selectedId) {
            selectedId = requested
            store.saveSelectedChapter(requested)
        }
    }

    fun selectChapter(id: String) {
        if (V1TextbookCatalog.chapterById(id) != null) {
            selectedId = id
            store.saveSelectedChapter(id)
            onChapterChanged(id)
        }
    }

    Scaffold(
        modifier = Modifier.testTag("ebook_textbook_root"),
        containerColor = BookShell,
        topBar = {
            EbookTopBar(
                chapter = selected,
                readCount = readCompleted.size,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        val markdown = remember(selected.id) {
            context.assets.open(selected.assetPath).bufferedReader().use { it.readText() }
        }
        val blocks = remember(markdown) { TextbookMarkdownParser.parse(markdown) }
        val sections = remember(selected.id, blocks) { TextbookSectioner.split(selected.id, blocks) }
        val restoredSectionIndex = store.selectedSectionIndex(selected.id)
            .coerceIn(0, sections.lastIndex.coerceAtLeast(0))
        var sectionIndex by rememberSaveable(selected.id) { mutableIntStateOf(restoredSectionIndex) }
        val currentSection = sections.getOrNull(sectionIndex) ?: sections.firstOrNull()

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BookShell)
        ) {
            if (currentSection != null) {
                val concepts = remember(selected.id, currentSection.id, lesson.lessonId) {
                    TextbookLearningFlow.buildConcepts(selected.id, currentSection, lesson)
                }
                val restoredConceptIndex = store.selectedConceptIndex(currentSection.id)
                    .coerceIn(0, concepts.lastIndex.coerceAtLeast(0))
                var conceptIndex by rememberSaveable(currentSection.id) { mutableIntStateOf(restoredConceptIndex) }
                val currentConcept = concepts.getOrNull(conceptIndex) ?: concepts.firstOrNull()

                if (currentConcept != null) {
                    key(selected.id, currentSection.id, currentConcept.id) {
                        EbookReader(
                            chapter = selected,
                            section = currentSection,
                            sectionCount = sections.size,
                            concept = currentConcept,
                            readComplete = selected.id in readCompleted,
                            practiceComplete = selected.practiceLessonId in practiceCompletedIds,
                            initialPageIndex = store.pageIndex(currentConcept.id),
                            onPageChanged = { index -> store.savePageIndex(currentConcept.id, index) },
                            onPreviousConcept = when {
                                conceptIndex > 0 -> {
                                    {
                                        val previous = concepts[conceptIndex - 1]
                                        store.savePageIndex(previous.id, Int.MAX_VALUE)
                                        conceptIndex -= 1
                                        store.saveSelectedConceptIndex(currentSection.id, conceptIndex)
                                    }
                                }
                                sectionIndex > 0 -> {
                                    {
                                        val previousSection = sections[sectionIndex - 1]
                                        val previousConcepts = TextbookLearningFlow.buildConcepts(selected.id, previousSection, lesson)
                                        val previousConcept = previousConcepts.lastOrNull()
                                        if (previousConcept != null) store.savePageIndex(previousConcept.id, Int.MAX_VALUE)
                                        store.saveSelectedConceptIndex(previousSection.id, previousConcepts.lastIndex.coerceAtLeast(0))
                                        sectionIndex -= 1
                                        store.saveSelectedSectionIndex(selected.id, sectionIndex)
                                    }
                                }
                                else -> null
                            },
                            onNextConcept = when {
                                conceptIndex < concepts.lastIndex -> {
                                    {
                                        val next = concepts[conceptIndex + 1]
                                        store.savePageIndex(next.id, 0)
                                        conceptIndex += 1
                                        store.saveSelectedConceptIndex(currentSection.id, conceptIndex)
                                    }
                                }
                                sectionIndex < sections.lastIndex -> {
                                    {
                                        val nextSection = sections[sectionIndex + 1]
                                        val nextConcepts = TextbookLearningFlow.buildConcepts(selected.id, nextSection, lesson)
                                        nextConcepts.firstOrNull()?.let { store.savePageIndex(it.id, 0) }
                                        store.saveSelectedConceptIndex(nextSection.id, 0)
                                        sectionIndex += 1
                                        store.saveSelectedSectionIndex(selected.id, sectionIndex)
                                    }
                                }
                                else -> null
                            },
                            onMarkRead = {
                                store.markReadComplete(selected.id)
                                readCompleted = store.readCompletedIds()
                            },
                            onPractice = { onStartPractice(selected.practiceLessonId) },
                            onPreviousChapter = V1TextbookCatalog.chapters.getOrNull(selected.number - 2)?.let { previous ->
                                { selectChapter(previous.id) }
                            },
                            onNextChapter = V1TextbookCatalog.chapters.getOrNull(selected.number)?.let { next ->
                                { selectChapter(next.id) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EbookTopBar(
    chapter: TextbookChapter,
    readCount: Int,
    onNavigateBack: () -> Unit
) {
    Surface(color = BookShell) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("서재", color = BookMuted)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "TRACK ${chapter.number.toString().padStart(2, '0')} / ${V1TextbookCatalog.TRACK_COUNT}",
                    color = BookMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = chapter.title,
                    color = BookInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Text(
                text = "$readCount/${V1TextbookCatalog.TRACK_COUNT}",
                color = BookMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EbookReader(
    chapter: TextbookChapter,
    section: TextbookSection,
    sectionCount: Int,
    concept: LearningConcept,
    readComplete: Boolean,
    practiceComplete: Boolean,
    initialPageIndex: Int,
    onPageChanged: (Int) -> Unit,
    onPreviousConcept: (() -> Unit)?,
    onNextConcept: (() -> Unit)?,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit,
    onPreviousChapter: (() -> Unit)?,
    onNextChapter: (() -> Unit)?
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().testTag("ebook_reader"),
        contentAlignment = Alignment.Center
    ) {
        val sidePadding = if (maxWidth >= 900.dp) 64.dp else 10.dp
        val density = LocalDensity.current
        val fontScale = density.fontScale.coerceAtLeast(1f)
        val metrics = remember(maxWidth, maxHeight, concept.id, fontScale) {
            TextbookPageMetrics(
                widthDp = (maxWidth.value - sidePadding.value * 2f - 52f).toInt().coerceAtLeast(240),
                heightDp = ((maxHeight.value - 176f) / fontScale).toInt().coerceAtLeast(336)
            )
        }
        val contentPages = remember(concept.id, metrics.widthDp, metrics.heightDp) {
            TextbookPaginator.paginate(concept.blocks, metrics)
        }
        val isFirstConceptInChapter = section.index == 0 && concept.index == 0
        val isLastConceptInChapter = section.index == sectionCount - 1
        val pages = remember(concept.id, contentPages, isFirstConceptInChapter, isLastConceptInChapter) {
            buildList<EbookPage> {
                if (isFirstConceptInChapter) add(EbookPage.TrackCover)
                add(EbookPage.LessonOpening)
                contentPages.forEach { add(EbookPage.Content(it)) }
                add(EbookPage.Recall)
                if (isLastConceptInChapter) add(EbookPage.TrackEnd)
            }
        }
        val restored = initialPageIndex.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        var pageIndex by rememberSaveable(concept.id, metrics.widthDp, metrics.heightDp) {
            mutableIntStateOf(restored)
        }
        val currentPage = pages.getOrNull(pageIndex) ?: pages.first()
        val previousBoundary = onPreviousConcept ?: onPreviousChapter
        val nextBoundary = onNextConcept ?: onNextChapter
        val swipeThresholdPx = with(density) { 56.dp.toPx() }

        fun goPrevious() {
            if (pageIndex > 0) pageIndex -= 1 else previousBoundary?.invoke()
        }

        fun goNext() {
            if (pageIndex < pages.lastIndex) pageIndex += 1 else nextBoundary?.invoke()
        }

        LaunchedEffect(pageIndex, concept.id) { onPageChanged(pageIndex) }

        Surface(
            modifier = Modifier
                .padding(horizontal = sidePadding, vertical = 8.dp)
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .pointerInput(concept.id, pages.size, swipeThresholdPx) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { _, amount -> dragTotal += amount },
                        onDragEnd = {
                            when {
                                dragTotal <= -swipeThresholdPx -> goNext()
                                dragTotal >= swipeThresholdPx -> goPrevious()
                            }
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f }
                    )
                },
            color = BookPaper,
            border = BorderStroke(1.dp, BookBorder),
            shape = RoundedCornerShape(if (maxWidth >= 600.dp) 18.dp else 0.dp),
            shadowElevation = if (maxWidth >= 600.dp) 6.dp else 0.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReaderBreadcrumb(chapter, section, sectionCount)
                    HorizontalDivider(color = BookBorder)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (currentPage) {
                            EbookPage.TrackCover -> TrackCoverPage(chapter)
                            EbookPage.LessonOpening -> LessonOpeningPage(section, sectionCount, concept)
                            is EbookPage.Content -> ContentBookPage(currentPage.page)
                            EbookPage.Recall -> RecallBookPage(concept.problem, onPractice)
                            EbookPage.TrackEnd -> TrackEndBookPage(
                                chapter = chapter,
                                readComplete = readComplete,
                                practiceComplete = practiceComplete,
                                onMarkRead = onMarkRead,
                                onPractice = onPractice,
                                onNextChapter = onNextChapter
                            )
                        }
                    }
                    PageFooter(
                        pageIndex = pageIndex,
                        pageCount = pages.size,
                        hasPrevious = pageIndex > 0 || previousBoundary != null,
                        hasNext = pageIndex < pages.lastIndex || nextBoundary != null,
                        onPrevious = ::goPrevious,
                        onNext = ::goNext
                    )
                }

                if (currentPage !is EbookPage.Recall) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(0.12f)
                            .clickable(enabled = pageIndex > 0 || previousBoundary != null) { goPrevious() }
                            .testTag("ebook_left_edge")
                    )
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(0.12f)
                            .clickable(enabled = pageIndex < pages.lastIndex || nextBoundary != null) { goNext() }
                            .testTag("ebook_right_edge")
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderBreadcrumb(chapter: TextbookChapter, section: TextbookSection, sectionCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TRACK ${chapter.number.toString().padStart(2, '0')}",
            color = BookAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "LESSON ${section.index + 1} / $sectionCount",
            modifier = Modifier.testTag("ebook_lesson_progress"),
            color = BookMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TrackCoverPage(chapter: TextbookChapter) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text("코딩 완전과정", color = BookMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "TRACK ${chapter.number.toString().padStart(2, '0')}",
            color = BookAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = chapter.title,
            modifier = Modifier.testTag("ebook_track_cover"),
            color = BookInk,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = chapter.summary,
            color = BookMuted,
            fontSize = 16.sp,
            lineHeight = 27.sp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = chapter.keyConcepts.joinToString("  ·  "),
            color = BookInk,
            fontSize = 13.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(28.dp))
        Text("오른쪽 끝을 누르거나 왼쪽으로 밀면 다음 페이지", color = BookMuted, fontSize = 12.sp)
    }
}

@Composable
private fun LessonOpeningPage(section: TextbookSection, sectionCount: Int, concept: LearningConcept) {
    val subheads = remember(concept.id) {
        concept.blocks.filterIsInstance<TextbookBlock.Heading>()
            .filter { it.level >= 3 }
            .map { it.text }
            .distinct()
            .take(4)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LESSON ${(section.index + 1).toString().padStart(2, '0')} / $sectionCount",
            color = BookAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = section.title,
            modifier = Modifier.testTag("ebook_lesson_opening"),
            color = BookInk,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text("약 ${section.estimatedMinutes}분", color = BookMuted, fontSize = 13.sp)
        if (subheads.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text("이 장에서 읽을 내용", color = BookMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            subheads.forEachIndexed { index, title ->
                Text(
                    text = "${index + 1}. $title",
                    color = BookInk,
                    fontSize = 14.sp,
                    lineHeight = 23.sp
                )
            }
        }
    }
}

@Composable
private fun ContentBookPage(page: TextbookPage) {
    Column(
        modifier = Modifier.fillMaxSize().testTag("ebook_content_page"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        page.blocks.forEach { block -> EbookBlockView(block, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun RecallBookPage(problem: InlineLearningProblem, onPractice: () -> Unit) {
    var answer by rememberSaveable(problem.id) { mutableStateOf("") }
    var saved by rememberSaveable(problem.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("책을 덮고 떠올리기", color = BookAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(problem.prompt, color = BookInk, fontSize = 17.sp, lineHeight = 28.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; saved = false },
            modifier = Modifier.fillMaxWidth().testTag("ebook_recall_input"),
            minLines = 4,
            maxLines = 6,
            label = { Text("내 말로 설명") },
            colors = ebookTextFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { saved = true },
                enabled = answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BookAction, contentColor = BookInk)
            ) { Text("기억 저장") }
            OutlinedButton(onClick = onPractice) { Text("실습 열기", color = BookAccent) }
        }
        if (saved) {
            Spacer(Modifier.height(12.dp))
            Text("저장했습니다. 이 페이지에서는 점수를 깎지 않습니다.", color = BookMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TrackEndBookPage(
    chapter: TextbookChapter,
    readComplete: Boolean,
    practiceComplete: Boolean,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit,
    onNextChapter: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TRACK ${chapter.number.toString().padStart(2, '0')} 끝", color = BookAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("여기까지 읽었습니다.", color = BookInk, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("읽기 완료를 남기고 실습으로 확인하거나 다음 TRACK으로 이어가세요.", color = BookMuted, fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onMarkRead,
            enabled = !readComplete,
            colors = ButtonDefaults.buttonColors(containerColor = BookAction, contentColor = BookInk)
        ) { Text(if (readComplete) "읽기 완료 ✓" else "읽기 완료") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onPractice, modifier = Modifier.fillMaxWidth()) {
            Text(if (practiceComplete) "TRACK 실습 다시 풀기 ✓" else "TRACK 실습 열기", color = BookAccent)
        }
        if (onNextChapter != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNextChapter, modifier = Modifier.fillMaxWidth()) {
                Text("다음 TRACK →", color = BookInk)
            }
        }
    }
}

@Composable
private fun PageFooter(
    pageIndex: Int,
    pageCount: Int,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val progress = ((pageIndex + 1).toFloat() / pageCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = BookAccent,
            trackColor = BookBorder
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevious, enabled = hasPrevious) { Text("‹", color = if (hasPrevious) BookInk else BookBorder, fontSize = 22.sp) }
            Text(
                text = "${pageIndex + 1} / $pageCount",
                modifier = Modifier.testTag("ebook_page_counter"),
                color = BookMuted,
                fontSize = 11.sp
            )
            TextButton(onClick = onNext, enabled = hasNext) { Text("›", color = if (hasNext) BookInk else BookBorder, fontSize = 22.sp) }
        }
    }
}

@Composable
private fun EbookBlockView(block: TextbookBlock, modifier: Modifier) {
    when (block) {
        is TextbookBlock.Heading -> {
            val size = when (block.level) {
                1 -> 24.sp
                2 -> 22.sp
                3 -> 19.sp
                4 -> 17.sp
                else -> 16.sp
            }
            Text(
                text = block.text,
                modifier = modifier.padding(top = if (block.level >= 3) 8.dp else 2.dp),
                color = BookInk,
                fontSize = size,
                lineHeight = (size.value + 8).sp,
                fontWeight = if (block.level <= 3) FontWeight.Bold else FontWeight.SemiBold
            )
        }
        is TextbookBlock.Paragraph -> Text(
            text = block.text,
            modifier = modifier,
            color = BookInk,
            fontSize = 16.sp,
            lineHeight = 27.sp
        )
        is TextbookBlock.BulletList -> Surface(
            modifier = modifier,
            color = BookSoft,
            border = BorderStroke(1.dp, BookBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(if (block.ordered) "${index + 1}." else "•", Modifier.width(26.dp), color = BookAccent, fontWeight = FontWeight.Bold)
                        Text(item, Modifier.weight(1f), color = BookInk, fontSize = 14.sp, lineHeight = 23.sp)
                    }
                }
            }
        }
        is TextbookBlock.Code -> EbookCodeBlock(block.text, block.language, modifier)
        is TextbookBlock.Table -> Surface(
            modifier = modifier,
            color = BookSoft,
            border = BorderStroke(1.dp, BookBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                block.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) HorizontalDivider(color = BookBorder)
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        block.headers.forEachIndexed { columnIndex, header ->
                            row.getOrNull(columnIndex)?.takeIf { it.isNotBlank() }?.let { value ->
                                Text(header, color = BookAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(value, color = BookInk, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }
        TextbookBlock.Divider -> HorizontalDivider(modifier = modifier.padding(vertical = 4.dp), color = BookBorder)
    }
}

@Composable
private fun EbookCodeBlock(
    text: String,
    language: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Surface(
        modifier = modifier,
        color = BookCode,
        border = BorderStroke(1.dp, Color(0xFF343A44)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            if (language.isNotBlank()) {
                Text(
                    text = language.uppercase(),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF151920)).padding(horizontal = 12.dp, vertical = 6.dp),
                    color = BookAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            SelectionContainer {
                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    color = BookCodeText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    softWrap = true
                )
            }
        }
    }
}

@Composable
private fun ebookTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = BookInk,
    unfocusedTextColor = BookInk,
    focusedContainerColor = BookPaper,
    unfocusedContainerColor = BookPaper,
    cursorColor = BookAccent,
    focusedBorderColor = BookAccent,
    unfocusedBorderColor = BookBorder,
    focusedLabelColor = BookAccent,
    unfocusedLabelColor = BookMuted
)
