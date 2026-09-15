package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.textbook.InlineLearningProblem
import com.futuretech.poweruser.textbook.LearningConcept
import com.futuretech.poweruser.textbook.LearningProblemType
import com.futuretech.poweruser.textbook.TextbookBlock
import com.futuretech.poweruser.textbook.TextbookChapter
import com.futuretech.poweruser.textbook.TextbookLearningFlow
import com.futuretech.poweruser.textbook.TextbookMarkdownParser
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.TextbookSection
import com.futuretech.poweruser.textbook.TextbookSectioner
import com.futuretech.poweruser.textbook.V1TextbookCatalog

private val ReaderShell = Color(0xFFEDEAE3)
private val ReaderPaper = Color(0xFFF9F7F2)
private val ReaderInk = Color(0xFF22252A)
private val ReaderMuted = Color(0xFF656B74)
private val ReaderBorder = Color(0xFFD7D2C8)
private val ReaderAccent = Color(0xFF315A94)
private val ReaderSoft = Color(0xFFE8EDF5)
private val ReaderCode = Color(0xFF171A20)
private val ReaderCodeText = Color(0xFFE9EDF3)
private const val ORDER_SEPARATOR = "\u001F"

@Composable
fun V1TextbookScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    initialChapterId: String = "V1-C01"
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

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ReaderShell)
        ) {
            SectionStrip(
                sections = sections,
                selectedIndex = sectionIndex,
                onSelect = { index ->
                    sectionIndex = index
                    store.saveSelectedSectionIndex(selected.id, index)
                }
            )
            HorizontalDivider(color = ReaderBorder)

            if (currentSection != null) {
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
                            onPreviousConcept = when {
                                conceptIndex > 0 -> {
                                    {
                                        conceptIndex -= 1
                                        store.saveSelectedConceptIndex(currentSection.id, conceptIndex)
                                    }
                                }
                                sectionIndex > 0 -> {
                                    {
                                        val previousSection = sections[sectionIndex - 1]
                                        val previousConcepts = TextbookLearningFlow.buildConcepts(selected.id, previousSection, lesson)
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
                                        conceptIndex += 1
                                        store.saveSelectedConceptIndex(currentSection.id, conceptIndex)
                                    }
                                }
                                sectionIndex < sections.lastIndex -> {
                                    {
                                        val nextSection = sections[sectionIndex + 1]
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
private fun ReaderTopBar(
    chapter: TextbookChapter,
    readCount: Int,
    onNavigateBack: () -> Unit
) {
    Surface(color = ReaderPaper, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                border = BorderStroke(1.dp, ReaderBorder)
            ) {
                Text("전체 과정", color = ReaderInk)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "1권 · Chapter ${chapter.number}/11",
                    color = ReaderMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = chapter.title,
                    color = ReaderInk,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
            Text(
                text = "읽기 완료 $readCount/11",
                color = ReaderMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SectionStrip(
    sections: List<TextbookSection>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(ReaderPaper).testTag("textbook_section_strip"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(sections, key = { _, section -> section.id }) { index, section ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = {
                    Text(
                        text = "${index + 1}. ${section.title.take(18)} · ${section.estimatedMinutes}분",
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.testTag("textbook_section_${index + 1}")
            )
        }
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
    onPreviousConcept: (() -> Unit)?,
    onNextConcept: (() -> Unit)?,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit,
    onPreviousChapter: (() -> Unit)?,
    onNextChapter: (() -> Unit)?
) {
    val state = rememberLazyListState()
    val isLastConceptInSection = concept.index == conceptCount - 1
    val isLastConceptInChapter = section.index == sectionCount - 1 && isLastConceptInSection

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("textbook_reader"),
        state = state,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "concept-head-${concept.id}") {
            Column(
                Modifier.fillMaxWidth().widthIn(max = 780.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "Section ${section.index + 1}/$sectionCount · 약 ${section.estimatedMinutes}분",
                    modifier = Modifier.testTag("textbook_section_progress"),
                    color = ReaderAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "개념 ${concept.index + 1}/$conceptCount",
                    modifier = Modifier.testTag("textbook_concept_progress"),
                    color = ReaderMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = concept.title,
                    modifier = Modifier.testTag("textbook_section_title"),
                    color = ReaderInk,
                    fontSize = 28.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold
                )
                if (concept.index == 0) {
                    Text(
                        text = chapter.summary,
                        color = ReaderMuted,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        itemsIndexed(concept.blocks, key = { index, _ -> "${concept.id}-block-$index" }) { _, block ->
            BlockView(block, Modifier.fillMaxWidth().widthIn(max = 780.dp))
        }

        item(key = "inline-problem-${concept.problem.id}") {
            InlineProblemCard(
                problem = concept.problem,
                onOpenPractice = onPractice,
                modifier = Modifier.fillMaxWidth().widthIn(max = 780.dp)
            )
        }

        item(key = "concept-actions-${concept.id}") {
            ConceptActions(
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
}

@Composable
private fun InlineProblemCard(
    problem: InlineLearningProblem,
    onOpenPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    key(problem.id) {
        var selectedOption by rememberSaveable { mutableIntStateOf(-1) }
        var textAnswer by rememberSaveable { mutableStateOf("") }
        var checked by rememberSaveable { mutableStateOf(false) }
        var pickedOrderEncoded by rememberSaveable { mutableStateOf("") }
        val pickedOrder = pickedOrderEncoded
            .split(ORDER_SEPARATOR)
            .filter { it.isNotBlank() }

        Surface(
            modifier = modifier.testTag("inline_problem_${problem.id}"),
            color = ReaderPaper,
            border = BorderStroke(1.dp, ReaderBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "짧은 확인 · ${problem.type.displayName}",
                    modifier = Modifier.testTag("inline_problem_type"),
                    color = ReaderAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(problem.prompt, color = ReaderInk, fontSize = 16.sp, lineHeight = 25.sp)

                problem.code?.let { MiniCodeBlock(it) }

                when (problem.type) {
                    LearningProblemType.CONCEPT_CHOICE,
                    LearningProblemType.AI_ANSWER_AUDIT -> {
                        problem.options.forEachIndexed { index, option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedOption = index
                                        checked = false
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedOption == index,
                                    onClick = {
                                        selectedOption = index
                                        checked = false
                                    }
                                )
                                Text(option, Modifier.weight(1f), color = ReaderInk, fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        }
                        Button(
                            onClick = { checked = true },
                            enabled = selectedOption >= 0,
                            colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                        ) { Text("확인") }
                        if (checked) {
                            val correct = selectedOption == problem.correctOptionIndex
                            MiniResult(
                                correct = correct,
                                text = if (correct) {
                                    "근거에 맞습니다. 다음 개념으로 넘어가도 됩니다."
                                } else {
                                    "본문 근거를 다시 확인하세요. 이 미니문제는 숙련도 점수를 깎지 않습니다."
                                }
                            )
                        }
                    }

                    LearningProblemType.ORDERING -> {
                        Text("아래 항목을 올바른 순서대로 누르세요.", color = ReaderMuted, fontSize = 13.sp)
                        problem.shuffledOrder.forEach { item ->
                            OutlinedButton(
                                onClick = {
                                    if (item !in pickedOrder) {
                                        pickedOrderEncoded = (pickedOrder + item).joinToString(ORDER_SEPARATOR)
                                        checked = false
                                    }
                                },
                                enabled = item !in pickedOrder,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(item, color = ReaderInk) }
                        }
                        if (pickedOrder.isNotEmpty()) {
                            Text(
                                text = "내 순서: ${pickedOrder.joinToString(" → ")}",
                                color = ReaderMuted,
                                fontSize = 13.sp,
                                lineHeight = 21.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { checked = true },
                                enabled = pickedOrder.size == problem.correctOrder.size,
                                colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                            ) { Text("순서 확인") }
                            OutlinedButton(onClick = {
                                pickedOrderEncoded = ""
                                checked = false
                            }) { Text("다시", color = ReaderInk) }
                        }
                        if (checked) {
                            val correct = pickedOrder == problem.correctOrder
                            MiniResult(
                                correct = correct,
                                text = if (correct) "순서가 맞습니다." else "순서를 다시 조합해 보세요. 정답은 자동 노출하지 않습니다."
                            )
                        }
                    }

                    LearningProblemType.FILL_CODE,
                    LearningProblemType.ONE_LINE_FIX -> {
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = {
                                textAnswer = it
                                checked = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (problem.type == LearningProblemType.FILL_CODE) "빈칸 답" else "고칠 한 줄") },
                            minLines = if (problem.type == LearningProblemType.ONE_LINE_FIX) 2 else 1
                        )
                        Button(
                            onClick = { checked = true },
                            enabled = textAnswer.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                        ) { Text("확인") }
                        if (checked) {
                            if (problem.acceptedAnswers.isEmpty()) {
                                MiniResult(
                                    correct = null,
                                    text = "자동 문자열 판정이 안전하지 않은 문제입니다. 전체 실습에서 실제 실행으로 확인하세요."
                                )
                            } else {
                                val answer = TextbookLearningFlow.normalizeAnswer(textAnswer)
                                val correct = problem.acceptedAnswers.any {
                                    TextbookLearningFlow.normalizeAnswer(it) == answer
                                }
                                MiniResult(
                                    correct = correct,
                                    text = if (correct) "맞았습니다." else "아직 다릅니다. 본문의 개념과 코드 차이를 다시 확인하세요."
                                )
                            }
                        }
                    }

                    LearningProblemType.OUTPUT_PREDICTION,
                    LearningProblemType.DIRECT_WRITE,
                    LearningProblemType.DEBUGGING -> {
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = {
                                textAnswer = it
                                checked = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    when (problem.type) {
                                        LearningProblemType.OUTPUT_PREDICTION -> "내 예상"
                                        LearningProblemType.DIRECT_WRITE -> "직접 작성"
                                        else -> "수정 코드"
                                    }
                                )
                            },
                            minLines = if (problem.type == LearningProblemType.OUTPUT_PREDICTION) 3 else 5
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { checked = true },
                                enabled = textAnswer.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                            ) {
                                Text(if (problem.type == LearningProblemType.OUTPUT_PREDICTION) "예상 저장" else "작성 완료")
                            }
                            OutlinedButton(onClick = onOpenPractice) {
                                Text("전체 실습에서 실행", color = ReaderAccent)
                            }
                        }
                        if (checked) {
                            MiniResult(
                                correct = null,
                                text = "여기서는 점수화하지 않습니다. 실제 실행·테스트 판정은 전체 실습에서 확인합니다."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniResult(correct: Boolean?, text: String) {
    val icon = when (correct) {
        true -> "✓"
        false -> "!"
        null -> "↔"
    }
    Surface(color = ReaderSoft, shape = RoundedCornerShape(11.dp)) {
        Text(
            text = "$icon  $text",
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            color = ReaderInk,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ConceptActions(
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
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 780.dp),
        color = ReaderPaper,
        border = BorderStroke(1.dp, ReaderBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(
                text = if (isLastConceptInChapter) {
                    "이 Chapter의 읽기 흐름을 마쳤습니다. 전체 실습으로 확인하세요."
                } else {
                    "짧은 확인을 마친 뒤 다음 개념으로 이어갑니다."
                },
                color = ReaderInk,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onPreviousConcept?.invoke() },
                    enabled = onPreviousConcept != null,
                    modifier = Modifier.weight(1f)
                ) { Text("← 이전 개념", color = ReaderInk) }

                if (!isLastConceptInChapter) {
                    val nextTag = if (isLastConceptInSection) "textbook_next_section" else "textbook_next_concept"
                    Button(
                        onClick = { onNextConcept?.invoke() },
                        enabled = onNextConcept != null,
                        modifier = Modifier.weight(1f).testTag(nextTag),
                        colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                    ) {
                        Text(if (isLastConceptInSection) "다음 Section →" else "다음 개념 →")
                    }
                } else {
                    Button(
                        onClick = onMarkRead,
                        enabled = !readComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                    ) {
                        Text(if (readComplete) "읽기 완료 ✓" else "Chapter 읽기 완료")
                    }
                }
            }

            OutlinedButton(
                onClick = onPractice,
                modifier = Modifier.fillMaxWidth().testTag("textbook_practice_button"),
                border = BorderStroke(1.dp, ReaderAccent)
            ) {
                Text(if (practiceComplete) "전체 문제·실습 다시 풀기 ✓" else "전체 문제·실습 열기", color = ReaderAccent)
            }

            if (isLastConceptInChapter) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onPreviousChapter?.invoke() },
                        enabled = onPreviousChapter != null && chapter.number > 1,
                        modifier = Modifier.weight(1f)
                    ) { Text("이전 Chapter", color = ReaderInk) }
                    OutlinedButton(
                        onClick = { onNextChapter?.invoke() },
                        enabled = onNextChapter != null,
                        modifier = Modifier.weight(1f).testTag("textbook_next_chapter")
                    ) { Text("다음 Chapter", color = ReaderInk) }
                }
            }
        }
    }
}

@Composable
private fun BlockView(block: TextbookBlock, modifier: Modifier) {
    when (block) {
        is TextbookBlock.Heading -> {
            val size = when (block.level) {
                1 -> 24.sp
                2 -> 21.sp
                3 -> 18.sp
                else -> 16.sp
            }
            Text(
                text = block.text,
                modifier = modifier.padding(top = 6.dp),
                color = ReaderInk,
                fontSize = size,
                lineHeight = when (block.level) {
                    1 -> 31.sp
                    2 -> 28.sp
                    3 -> 25.sp
                    else -> 23.sp
                },
                fontWeight = FontWeight.Bold
            )
        }

        is TextbookBlock.Paragraph -> Text(
            text = block.text,
            modifier = modifier,
            color = ReaderInk,
            fontSize = 16.sp,
            lineHeight = 27.sp
        )

        is TextbookBlock.BulletList -> Surface(
            modifier = modifier,
            color = ReaderPaper,
            border = BorderStroke(1.dp, ReaderBorder),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(
                            text = if (block.ordered) "${index + 1}." else "•",
                            modifier = Modifier.width(28.dp),
                            color = ReaderAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(item, Modifier.weight(1f), color = ReaderInk, fontSize = 15.sp, lineHeight = 24.sp)
                    }
                }
            }
        }

        is TextbookBlock.Code -> MiniCodeBlock(block.text, block.language, modifier)

        is TextbookBlock.Table -> Surface(
            modifier = modifier,
            color = ReaderPaper,
            border = BorderStroke(1.dp, ReaderBorder),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                block.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) HorizontalDivider(color = ReaderBorder)
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.headers.forEachIndexed { columnIndex, header ->
                            row.getOrNull(columnIndex)?.takeIf { it.isNotBlank() }?.let { value ->
                                Text(header, color = ReaderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(value, color = ReaderInk, fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                }
            }
        }

        TextbookBlock.Divider -> HorizontalDivider(modifier = modifier.padding(vertical = 5.dp), color = ReaderBorder)
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
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF20242B)).padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.uppercase(),
                        color = Color(0xFFB8C7DF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            SelectionContainer {
                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth().padding(14.dp).horizontalScroll(rememberScrollState()),
                    color = ReaderCodeText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    softWrap = false
                )
            }
        }
    }
}
