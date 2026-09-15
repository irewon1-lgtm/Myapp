package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.futuretech.poweruser.textbook.*

private val FlowBg = Color(0xFF111315)
private val FlowPaper = Color(0xFF181B1E)
private val FlowPanel = Color(0xFF202428)
private val FlowBorder = Color(0xFF33383E)
private val FlowText = Color(0xFFF2F0EA)
private val FlowMuted = Color(0xFFB8B5AE)
private val FlowAccent = Color(0xFF8FB7A5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V1LearningFlowScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit
) {
    val context = LocalContext.current
    val store = remember { TextbookProgressStore(context) }
    var selectedId by rememberSaveable {
        mutableStateOf(store.selectedChapterId()?.takeIf { V1TextbookCatalog.chapterById(it) != null } ?: "V1-C01")
    }
    var readCompleted by remember { mutableStateOf(store.readCompletedIds()) }
    val chapter = V1TextbookCatalog.chapterById(selectedId) ?: V1TextbookCatalog.chapters.first()
    val lesson = remember(chapter.practiceLessonId) {
        CurriculumDataRepository.lessonById(chapter.practiceLessonId)
            ?: error("Missing textbook practice lesson ${chapter.practiceLessonId}")
    }
    val markdown = remember(chapter.id) {
        context.assets.open(chapter.assetPath).bufferedReader().use { it.readText() }
    }
    val blocks = remember(markdown) { TextbookMarkdownParser.parse(markdown) }
    val sections = remember(chapter.id, blocks, lesson.lessonId) {
        TextbookLearningFlow.buildSections(chapter, blocks, lesson)
    }
    var sectionIndex by rememberSaveable(chapter.id) {
        mutableIntStateOf(store.sectionIndex(chapter.id).coerceIn(0, sections.lastIndex.coerceAtLeast(0)))
    }
    val current = sections[sectionIndex]
    val readComplete = chapter.id in readCompleted
    val practiceComplete = chapter.practiceLessonId in practiceCompletedIds

    fun selectChapter(id: String) {
        selectedId = id
        store.saveSelectedChapter(id)
    }

    fun moveSection(target: Int) {
        val bounded = target.coerceIn(0, sections.lastIndex)
        sectionIndex = bounded
        store.saveSectionIndex(chapter.id, bounded)
    }

    Scaffold(
        modifier = Modifier.testTag("v1_textbook_root"),
        containerColor = FlowBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${chapter.number}. ${chapter.title}", fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            "개념 ${sectionIndex + 1}/${sections.size} · ${current.title}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 학습") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowBg)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).background(FlowBg)
        ) {
            ChapterStrip(
                selectedId = chapter.id,
                readCompleted = readCompleted,
                practiceCompletedIds = practiceCompletedIds,
                onSelect = ::selectChapter
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(FlowBorder))
            key(current.id) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier.fillMaxWidth().widthIn(max = 760.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionHeader(
                            chapter = chapter,
                            sectionIndex = sectionIndex,
                            sectionCount = sections.size,
                            sectionTitle = current.title,
                            readComplete = readComplete,
                            practiceComplete = practiceComplete
                        )

                        current.blocks.forEach { block ->
                            FlowBlockView(block, Modifier.fillMaxWidth())
                        }

                        InlineProblemCard(
                            problem = current.problem,
                            onOpenPractice = { onStartPractice(chapter.practiceLessonId) }
                        )

                        SectionNavigation(
                            index = sectionIndex,
                            count = sections.size,
                            readComplete = readComplete,
                            practiceComplete = practiceComplete,
                            onPrevious = { moveSection(sectionIndex - 1) },
                            onNext = { moveSection(sectionIndex + 1) },
                            onMarkRead = {
                                store.markReadComplete(chapter.id)
                                readCompleted = store.readCompletedIds()
                            },
                            onPractice = { onStartPractice(chapter.practiceLessonId) }
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterStrip(
    selectedId: String,
    readCompleted: Set<String>,
    practiceCompletedIds: Set<String>,
    onSelect: (String) -> Unit
) {
    LazyRow(
        Modifier.fillMaxWidth().background(FlowBg).padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(V1TextbookCatalog.chapters, key = { it.id }) { chapter ->
            val done = chapter.id in readCompleted
            val practiceDone = chapter.practiceLessonId in practiceCompletedIds
            FilterChip(
                selected = chapter.id == selectedId,
                onClick = { onSelect(chapter.id) },
                label = {
                    Text(
                        buildString {
                            append(chapter.number)
                            if (done) append(" ✓")
                            if (practiceDone) append(" · 실습")
                        }
                    )
                },
                modifier = Modifier.testTag("textbook_chapter_${chapter.id}")
            )
        }
    }
}

@Composable
private fun SectionHeader(
    chapter: TextbookChapter,
    sectionIndex: Int,
    sectionCount: Int,
    sectionTitle: String,
    readComplete: Boolean,
    practiceComplete: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "CHAPTER ${chapter.number.toString().padStart(2, '0')}  ·  개념 ${sectionIndex + 1}/$sectionCount",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = FlowAccent,
            letterSpacing = .6.sp
        )
        Text(
            sectionTitle,
            modifier = Modifier.testTag("textbook_chapter_title"),
            fontSize = 30.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            color = FlowText
        )
        Text(
            chapter.summary,
            fontSize = 14.sp,
            lineHeight = 23.sp,
            color = FlowMuted
        )
        LinearProgressIndicator(
            progress = { (sectionIndex + 1f) / sectionCount.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = FlowAccent,
            trackColor = FlowPanel
        )
        Text(
            "본문 ${if (readComplete) "✓ 완료" else "읽는 중"}  ·  실습 ${if (practiceComplete) "✓ 완료" else "대기"}",
            fontSize = 12.sp,
            color = FlowMuted
        )
    }
}

@Composable
private fun InlineProblemCard(
    problem: InlineLearningProblem,
    onOpenPractice: () -> Unit
) {
    key(problem.id) {
        var selectedOption by rememberSaveable { mutableIntStateOf(-1) }
        var textAnswer by rememberSaveable { mutableStateOf("") }
        var checked by rememberSaveable { mutableStateOf(false) }
        var pickedOrder by remember { mutableStateOf(emptyList<String>()) }

        Surface(
            modifier = Modifier.fillMaxWidth().testTag("inline_problem_${problem.id}"),
            color = FlowPaper,
            border = BorderStroke(1.dp, FlowBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("짧은 확인 · ${problem.type.displayName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FlowAccent)
                Text(problem.prompt, fontSize = 16.sp, lineHeight = 25.sp, color = FlowText)

                problem.code?.let { MiniCodeBlock(it) }

                when (problem.type) {
                    LearningProblemType.CONCEPT_CHOICE,
                    LearningProblemType.AI_ANSWER_AUDIT -> {
                        problem.options.forEachIndexed { index, option ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    selectedOption = index
                                    checked = false
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedOption == index, onClick = {
                                    selectedOption = index
                                    checked = false
                                })
                                Text(option, Modifier.weight(1f), fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        }
                        Button(
                            onClick = { checked = true },
                            enabled = selectedOption >= 0
                        ) { Text("확인") }
                        if (checked) {
                            val correct = selectedOption == problem.correctOptionIndex
                            MiniResult(
                                correct = correct,
                                text = if (correct) "근거에 맞습니다. 다음 개념으로 넘어가도 됩니다."
                                else "다시 본문 근거를 확인하세요. 이 미니문제는 숙련도 점수를 깎지 않습니다."
                            )
                        }
                    }

                    LearningProblemType.ORDERING -> {
                        Text("아래 항목을 올바른 순서대로 눌러 보세요.", fontSize = 13.sp, color = FlowMuted)
                        problem.shuffledOrder.forEach { item ->
                            OutlinedButton(
                                onClick = {
                                    if (item !in pickedOrder) {
                                        pickedOrder = pickedOrder + item
                                        checked = false
                                    }
                                },
                                enabled = item !in pickedOrder,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(item) }
                        }
                        if (pickedOrder.isNotEmpty()) {
                            Text("내 순서: ${pickedOrder.joinToString(" → ")}", fontSize = 13.sp, lineHeight = 21.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { checked = true },
                                enabled = pickedOrder.size == problem.correctOrder.size
                            ) { Text("순서 확인") }
                            OutlinedButton(onClick = {
                                pickedOrder = emptyList()
                                checked = false
                            }) { Text("다시") }
                        }
                        if (checked) {
                            val correct = pickedOrder == problem.correctOrder
                            MiniResult(correct, if (correct) "순서가 맞습니다." else "순서를 다시 조합해 보세요. 정답은 아직 자동으로 노출하지 않습니다.")
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
                        Button(onClick = { checked = true }, enabled = textAnswer.isNotBlank()) { Text("확인") }
                        if (checked) {
                            val answer = TextbookLearningFlow.normalizeAnswer(textAnswer)
                            val correct = problem.acceptedAnswers.any { TextbookLearningFlow.normalizeAnswer(it) == answer }
                            MiniResult(
                                correct = correct,
                                text = if (correct) "맞았습니다."
                                else "아직 다릅니다. 본문의 개념과 코드 차이를 다시 확인하세요."
                            )
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
                                enabled = textAnswer.isNotBlank()
                            ) { Text(if (problem.type == LearningProblemType.OUTPUT_PREDICTION) "예상 저장" else "작성 완료") }
                            OutlinedButton(onClick = onOpenPractice) { Text("전체 실습에서 실행") }
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
    Surface(color = FlowPanel, shape = RoundedCornerShape(12.dp)) {
        Text("$icon  $text", Modifier.fillMaxWidth().padding(12.dp), fontSize = 13.sp, lineHeight = 20.sp, color = FlowText)
    }
}

@Composable
private fun SectionNavigation(
    index: Int,
    count: Int,
    readComplete: Boolean,
    practiceComplete: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit
) {
    val last = index == count - 1
    Surface(color = FlowPaper, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, FlowBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrevious, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("← 이전 개념") }
                Button(onClick = onNext, enabled = !last, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = FlowAccent, contentColor = Color(0xFF102019))) {
                    Text("다음 개념 →")
                }
            }
            if (last) {
                Button(
                    onClick = onMarkRead,
                    enabled = !readComplete,
                    modifier = Modifier.fillMaxWidth().testTag("textbook_mark_read")
                ) { Text(if (readComplete) "본문 완료 ✓" else "이 Chapter 읽기 완료") }
                OutlinedButton(
                    onClick = onPractice,
                    modifier = Modifier.fillMaxWidth().testTag("textbook_practice_button")
                ) { Text(if (practiceComplete) "실습 다시 풀기 ✓" else "문제·실습으로 이어가기") }
            }
        }
    }
}

@Composable
private fun FlowBlockView(block: TextbookBlock, modifier: Modifier) {
    when (block) {
        is TextbookBlock.Heading -> {
            val size = when (block.level) {
                1 -> 27.sp
                2 -> 24.sp
                3 -> 21.sp
                else -> 18.sp
            }
            Text(block.text, modifier.padding(top = 6.dp), fontSize = size, lineHeight = size * 1.35f, fontWeight = FontWeight.Bold, color = FlowText)
        }
        is TextbookBlock.Paragraph -> Text(block.text, modifier, fontSize = 16.sp, lineHeight = 29.sp, color = FlowText)
        is TextbookBlock.BulletList -> Surface(modifier, color = FlowPaper, border = BorderStroke(1.dp, FlowBorder), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(if (block.ordered) "${index + 1}." else "•", Modifier.width(28.dp), color = FlowAccent, fontWeight = FontWeight.Bold)
                        Text(item, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 24.sp, color = FlowText)
                    }
                }
            }
        }
        is TextbookBlock.Code -> MiniCodeBlock(block.text, block.language)
        is TextbookBlock.Table -> Surface(modifier, color = FlowPaper, border = BorderStroke(1.dp, FlowBorder), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth()) {
                block.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(FlowBorder))
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.headers.forEachIndexed { index, header ->
                            val value = row.getOrNull(index).orEmpty()
                            if (value.isNotBlank()) {
                                Text(header, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlowAccent)
                                Text(value, fontSize = 14.sp, lineHeight = 21.sp, color = FlowText)
                            }
                        }
                    }
                }
            }
        }
        TextbookBlock.Divider -> Box(modifier.height(1.dp).background(FlowBorder))
    }
}

@Composable
private fun MiniCodeBlock(text: String, language: String = "") {
    Surface(color = Color(0xFF0D0F11), border = BorderStroke(1.dp, FlowBorder), shape = RoundedCornerShape(13.dp)) {
        Column {
            if (language.isNotBlank()) {
                Text(language.uppercase(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FlowAccent)
            }
            SelectionContainer {
                Text(
                    text,
                    Modifier.fillMaxWidth().padding(13.dp).horizontalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    softWrap = false,
                    color = FlowText
                )
            }
        }
    }
}
