package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.textbook.LearningConcept
import com.futuretech.poweruser.textbook.TextbookBlock
import com.futuretech.poweruser.textbook.TextbookChapter
import com.futuretech.poweruser.textbook.TextbookContentPage
import com.futuretech.poweruser.textbook.TextbookLearningFlow
import com.futuretech.poweruser.textbook.TextbookMarkdownParser
import com.futuretech.poweruser.textbook.TextbookPageComposer
import com.futuretech.poweruser.textbook.TextbookPageLayout
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.TextbookSection
import com.futuretech.poweruser.textbook.TextbookSectioner
import com.futuretech.poweruser.textbook.V1TextbookCatalog

private val ReaderShell = Color(0xFF07080A)
private val ReaderPaper = Color(0xFF101216)
private val ReaderInk = Color(0xFFF5F7F8)
private val ReaderMuted = Color(0xFFA7AFBA)
private val ReaderBorder = Color(0xFF2A2F36)
private val ReaderAccent = Color(0xFF94B9EE)
private val ReaderAction = Color(0xFF2B527F)
private val ReaderSoft = Color(0xFF181C22)
private val ReaderCode = Color(0xFF060709)
private val ReaderCodeText = Color(0xFFE9EDF3)
private const val OPEN_LAST_PAGE = Int.MAX_VALUE

private sealed interface ReaderPage {
    data object LessonCover : ReaderPage
    data class Content(val content: TextbookContentPage) : ReaderPage
    data object Recall : ReaderPage
    data object LessonEnd : ReaderPage
}

@Composable
fun V1TextbookScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    initialChapterId: String = "V1-C01",
    onOpenToc: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember { TextbookProgressStore(context) }
    val safeInitial = initialChapterId.takeIf { V1TextbookCatalog.chapterById(it) != null }
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

    fun selectChapter(id: String) {
        if (V1TextbookCatalog.chapterById(id) != null) {
            selectedId = id
            store.saveSelectedChapter(id)
        }
    }

    Scaffold(
        modifier = Modifier.testTag("v1_textbook_root"),
        containerColor = ReaderShell,
        topBar = {
            ReaderTopBar(
                chapter = selected,
                readCount = readCompleted.size,
                onNavigateBack = onNavigateBack,
                onOpenToc = onOpenToc
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

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ReaderShell)
        ) {
            if (currentSection != null) {
                SectionStrip(currentSection, sections.size)
                HorizontalDivider(color = ReaderBorder)

                val concepts = remember(selected.id, currentSection.id, lesson.lessonId) {
                    TextbookLearningFlow.buildConcepts(selected.id, currentSection, lesson)
                }
                val restoredConceptIndex = store.selectedConceptIndex(currentSection.id)
                    .coerceIn(0, concepts.lastIndex.coerceAtLeast(0))
                var conceptIndex by rememberSaveable(currentSection.id) {
                    mutableIntStateOf(restoredConceptIndex)
                }
                val currentConcept = concepts.getOrNull(conceptIndex) ?: concepts.firstOrNull()

                if (currentConcept != null) {
                    key(selected.id, currentSection.id, currentConcept.id) {
                        ConceptReader(
                            chapter = selected,
                            section = currentSection,
                            sectionCount = sections.size,
                            concept = currentConcept,
                            conceptCount = concepts.size,
                            readComplete = selected.id in readCompleted,
                            practiceComplete = selected.practiceLessonId in practiceCompletedIds,
                            initialPageIndex = store.selectedPageIndex(currentConcept.id),
                            onPageChanged = { store.saveSelectedPageIndex(currentConcept.id, it) },
                            onPreviousConcept = when {
                                conceptIndex > 0 -> {
                                    {
                                        val target = concepts[conceptIndex - 1]
                                        store.saveSelectedPageIndex(target.id, OPEN_LAST_PAGE)
                                        conceptIndex -= 1
                                        store.saveSelectedConceptIndex(currentSection.id, conceptIndex)
                                    }
                                }
                                sectionIndex > 0 -> {
                                    {
                                        val previousSection = sections[sectionIndex - 1]
                                        val previousConcepts = TextbookLearningFlow.buildConcepts(selected.id, previousSection, lesson)
                                        store.saveSelectedConceptIndex(previousSection.id, previousConcepts.lastIndex.coerceAtLeast(0))
                                        previousConcepts.lastOrNull()?.let {
                                            store.saveSelectedPageIndex(it.id, OPEN_LAST_PAGE)
                                        }
                                        sectionIndex -= 1
                                        store.saveSelectedSectionIndex(selected.id, sectionIndex)
                                    }
                                }
                                else -> null
                            },
                            onNextConcept = when {
                                conceptIndex < concepts.lastIndex -> {
                                    {
                                        val target = concepts[conceptIndex + 1]
                                        store.saveSelectedPageIndex(target.id, 0)
                                        conceptIndex += 1
                                        store.saveSelectedConceptIndex(currentSection.id, conceptIndex)
                                    }
                                }
                                sectionIndex < sections.lastIndex -> {
                                    {
                                        val nextSection = sections[sectionIndex + 1]
                                        val nextConcepts = TextbookLearningFlow.buildConcepts(selected.id, nextSection, lesson)
                                        store.saveSelectedConceptIndex(nextSection.id, 0)
                                        nextConcepts.firstOrNull()?.let { store.saveSelectedPageIndex(it.id, 0) }
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
private fun ReaderTopBar(
    chapter: TextbookChapter,
    readCount: Int,
    onNavigateBack: () -> Unit,
    onOpenToc: (() -> Unit)?
) {
    Surface(color = ReaderPaper, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("‹ 서재", color = ReaderInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TRACK ${chapter.number.toString().padStart(2, '0')} / ${V1TextbookCatalog.TRACK_COUNT}",
                    color = ReaderMuted,
                    fontSize = 10.sp
                )
                Text(
                    chapter.title,
                    color = ReaderInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            TextButton(
                onClick = { onOpenToc?.invoke() },
                enabled = onOpenToc != null,
                modifier = Modifier.testTag("reader_toc_button")
            ) {
                Text("목차", color = ReaderAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionStrip(section: TextbookSection, sectionCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReaderPaper)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .testTag("textbook_section_strip"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "LESSON ${section.index + 1}/$sectionCount",
            color = ReaderAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(10.dp))
        Text(
            section.title,
            modifier = Modifier.weight(1f),
            color = ReaderInk,
            fontSize = 12.sp,
            maxLines = 1
        )
        Text("약 ${section.estimatedMinutes}분", color = ReaderMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ConceptReader(
    chapter: TextbookChapter,
    section: TextbookSection,
    sectionCount: Int,
    concept: LearningConcept,
    conceptCount: Int,
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
    val configuration = LocalConfiguration.current
    val charsPerLine = when {
        configuration.screenWidthDp >= 1000 -> 52
        configuration.screenWidthDp >= 700 -> 42
        configuration.screenWidthDp >= 480 -> 30
        else -> 22
    }
    val maxLines = when {
        configuration.screenHeightDp >= 1000 -> 22
        configuration.screenHeightDp >= 760 -> 18
        else -> 14
    }
    val layout = remember(charsPerLine, maxLines) { TextbookPageLayout(charsPerLine, maxLines) }
    val contentPages = remember(concept.id, concept.blocks, layout) {
        TextbookPageComposer.paginate(concept.blocks, layout)
    }
    val pages = remember(concept.id, contentPages) {
        buildList<ReaderPage> {
            add(ReaderPage.LessonCover)
            contentPages.forEach { add(ReaderPage.Content(it)) }
            add(ReaderPage.Recall)
            add(ReaderPage.LessonEnd)
        }
    }
    val start = initialPageIndex.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    var pageIndex by rememberSaveable(concept.id) { mutableIntStateOf(start) }
    val page = pages[pageIndex]
    val isLastConceptInSection = concept.index == conceptCount - 1
    val isLastConceptInChapter = section.index == sectionCount - 1 && isLastConceptInSection
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    fun previous() {
        when {
            pageIndex > 0 -> pageIndex -= 1
            onPreviousConcept != null -> onPreviousConcept()
            onPreviousChapter != null -> onPreviousChapter()
        }
    }

    fun next() {
        when {
            pageIndex < pages.lastIndex -> pageIndex += 1
            onNextConcept != null -> onNextConcept()
            isLastConceptInChapter && onNextChapter != null -> onNextChapter()
        }
    }

    LaunchedEffect(concept.id, pageIndex) { onPageChanged(pageIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReaderShell)
            .testTag("textbook_reader")
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
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            color = ReaderPaper,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ReaderBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 22.dp)
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (page) {
                        ReaderPage.LessonCover -> LessonCoverPage(chapter, section, sectionCount, concept)
                        is ReaderPage.Content -> ContentBookPage(page.content)
                        ReaderPage.Recall -> RecallPage(concept, onPractice)
                        ReaderPage.LessonEnd -> LessonEndPage(
                            chapter = chapter,
                            isLastConceptInSection = isLastConceptInSection,
                            isLastConceptInChapter = isLastConceptInChapter,
                            readComplete = readComplete,
                            practiceComplete = practiceComplete,
                            onPreviousConcept = onPreviousConcept,
                            onNextConcept = onNextConcept,
                            onMarkRead = onMarkRead,
                            onPractice = onPractice,
                            onPreviousChapter = onPreviousChapter,
                            onNextChapter = onNextChapter
                        )
                    }
                }
                HorizontalDivider(color = ReaderBorder)
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.title, color = ReaderMuted, fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${pageIndex + 1} / ${pages.size}",
                        modifier = Modifier.testTag("textbook_page_indicator"),
                        color = ReaderMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(48.dp)
                .clickable { previous() }
                .testTag("textbook_left_tap_zone")
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(48.dp)
                .clickable { next() }
                .testTag("textbook_right_tap_zone")
        )
    }
}

@Composable
private fun LessonCoverPage(
    chapter: TextbookChapter,
    section: TextbookSection,
    sectionCount: Int,
    concept: LearningConcept
) {
    Column(
        modifier = Modifier.fillMaxSize().testTag("textbook_lesson_cover"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "TRACK ${chapter.number.toString().padStart(2, '0')}",
            color = ReaderAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "LESSON ${section.index + 1} / $sectionCount",
            modifier = Modifier.testTag("textbook_section_progress"),
            color = ReaderMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            concept.title,
            modifier = Modifier.testTag("textbook_section_title"),
            color = ReaderInk,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Text(
            chapter.summary,
            color = ReaderMuted,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 620.dp)
        )
        Spacer(Modifier.height(26.dp))
        Surface(color = ReaderSoft, shape = RoundedCornerShape(12.dp)) {
            Text(
                "오른쪽 끝을 누르거나 왼쪽으로 밀면 다음 장",
                color = ReaderInk,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun ContentBookPage(page: TextbookContentPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        page.blocks.forEach { block ->
            BlockView(block, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RecallPage(concept: LearningConcept, onPractice: () -> Unit) {
    var answer by rememberSaveable(concept.problem.id) { mutableStateOf("") }
    var saved by rememberSaveable(concept.problem.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text("읽은 내용 꺼내보기", color = ReaderAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(concept.problem.prompt, color = ReaderInk, fontSize = 17.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("내 말로 설명") },
            minLines = 4,
            colors = readerTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { saved = true },
                enabled = answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ReaderAction, contentColor = ReaderInk)
            ) { Text("작성 완료") }
            OutlinedButton(onClick = onPractice) { Text("전체 실습", color = ReaderAccent) }
        }
        if (saved) {
            Spacer(Modifier.height(10.dp))
            Surface(color = ReaderSoft, shape = RoundedCornerShape(10.dp)) {
                Text(
                    "여기서는 점수를 깎지 않습니다. 다음 장에서 LESSON을 마치거나 전체 실습으로 확인하세요.",
                    color = ReaderInk,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun LessonEndPage(
    chapter: TextbookChapter,
    isLastConceptInSection: Boolean,
    isLastConceptInChapter: Boolean,
    readComplete: Boolean,
    practiceComplete: Boolean,
    onPreviousConcept: (() -> Unit)?,
    onNextConcept: (() -> Unit)?,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit,
    onPreviousChapter: (() -> Unit)?,
    onNextChapter: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isLastConceptInChapter) "TRACK 읽기 완료" else "LESSON 읽기 완료",
            color = ReaderAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isLastConceptInChapter) {
                "이 TRACK의 본문을 끝까지 읽었습니다. 문제·실습으로 이해를 확인할 수 있습니다."
            } else {
                "여기까지 읽었습니다. 다음 LESSON도 같은 방식으로 한 장씩 이어집니다."
            },
            color = ReaderInk,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(18.dp))

        if (!isLastConceptInChapter) {
            Button(
                onClick = { onNextConcept?.invoke() },
                enabled = onNextConcept != null,
                modifier = Modifier.fillMaxWidth().testTag(if (isLastConceptInSection) "textbook_next_section" else "textbook_next_concept"),
                colors = ButtonDefaults.buttonColors(containerColor = ReaderAction, contentColor = ReaderInk)
            ) { Text("다음 LESSON →") }
        } else {
            Button(
                onClick = onMarkRead,
                enabled = !readComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ReaderAction, contentColor = ReaderInk)
            ) { Text(if (readComplete) "TRACK 읽기 완료 ✓" else "이 TRACK 읽기 완료") }
        }

        Spacer(Modifier.height(9.dp))
        OutlinedButton(
            onClick = onPractice,
            modifier = Modifier.fillMaxWidth().testTag("textbook_practice_button"),
            border = BorderStroke(1.dp, ReaderAccent)
        ) { Text(if (practiceComplete) "전체 문제·실습 다시 풀기 ✓" else "전체 문제·실습 열기", color = ReaderAccent) }

        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onPreviousConcept?.invoke() },
                enabled = onPreviousConcept != null,
                modifier = Modifier.weight(1f)
            ) { Text("← 이전 LESSON", color = ReaderInk) }
            if (isLastConceptInChapter) {
                OutlinedButton(
                    onClick = { onNextChapter?.invoke() },
                    enabled = onNextChapter != null,
                    modifier = Modifier.weight(1f).testTag("textbook_next_chapter")
                ) { Text("다음 TRACK", color = ReaderInk) }
            }
        }

        if (isLastConceptInChapter && chapter.number > 1) {
            Spacer(Modifier.height(7.dp))
            TextButton(onClick = { onPreviousChapter?.invoke() }, enabled = onPreviousChapter != null) {
                Text("이전 TRACK으로 돌아가기", color = ReaderMuted)
            }
        }
    }
}

@Composable
private fun readerTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ReaderInk,
    unfocusedTextColor = ReaderInk,
    disabledTextColor = ReaderMuted,
    focusedContainerColor = ReaderPaper,
    unfocusedContainerColor = ReaderPaper,
    cursorColor = ReaderAccent,
    focusedBorderColor = ReaderAccent,
    unfocusedBorderColor = ReaderBorder,
    focusedLabelColor = ReaderAccent,
    unfocusedLabelColor = ReaderMuted
)

@Composable
private fun BlockView(block: TextbookBlock, modifier: Modifier) {
    when (block) {
        is TextbookBlock.Heading -> {
            val size = when (block.level) {
                1 -> 23.sp
                2 -> 20.sp
                3 -> 18.sp
                4 -> 16.sp
                else -> 15.sp
            }
            Text(
                text = block.text,
                modifier = modifier,
                color = ReaderInk,
                fontSize = size,
                lineHeight = (size.value + 7).sp,
                fontWeight = if (block.level <= 3) FontWeight.Bold else FontWeight.SemiBold
            )
        }

        is TextbookBlock.Paragraph -> Text(
            text = block.text,
            modifier = modifier,
            color = ReaderInk,
            fontSize = 16.sp,
            lineHeight = 26.sp
        )

        is TextbookBlock.BulletList -> Surface(
            modifier = modifier,
            color = ReaderSoft,
            border = BorderStroke(1.dp, ReaderBorder),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(
                            if (block.ordered) "${index + 1}." else "•",
                            modifier = Modifier.width(26.dp),
                            color = ReaderAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(item, Modifier.weight(1f), color = ReaderInk, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }
        }

        is TextbookBlock.Code -> MiniCodeBlock(block.text, block.language, modifier)

        is TextbookBlock.Table -> Surface(
            modifier = modifier,
            color = ReaderSoft,
            border = BorderStroke(1.dp, ReaderBorder),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                block.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) HorizontalDivider(color = ReaderBorder)
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        block.headers.forEachIndexed { columnIndex, header ->
                            row.getOrNull(columnIndex)?.takeIf { it.isNotBlank() }?.let { value ->
                                Text(header, color = ReaderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(value, color = ReaderInk, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        TextbookBlock.Divider -> HorizontalDivider(modifier = modifier, color = ReaderBorder)
    }
}

@Composable
private fun MiniCodeBlock(
    text: String,
    language: String = "",
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Surface(
        modifier = modifier,
        color = ReaderCode,
        border = BorderStroke(1.dp, Color(0xFF343A44)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column {
            if (language.isNotBlank()) {
                Text(
                    language.uppercase(),
                    color = ReaderAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF151920)).padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            SelectionContainer {
                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    color = ReaderCodeText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    softWrap = true
                )
            }
        }
    }
}
