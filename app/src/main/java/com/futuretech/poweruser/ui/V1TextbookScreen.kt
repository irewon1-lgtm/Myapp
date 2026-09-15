package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.*
import kotlinx.coroutines.flow.distinctUntilChanged

private val AiBg = Color(0xFF070A12)
private val AiPanel = Color(0xFF0D1320)
private val AiPanel2 = Color(0xFF121A2B)
private val AiBorder = Color(0xFF263248)
private val AiCyan = Color(0xFF6BE7FF)
private val AiViolet = Color(0xFF9B8CFF)
private val AiGreen = Color(0xFF59F2AE)
private val AiMuted = Color(0xFF9AA8BF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V1TextbookScreen(
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
    val selected = V1TextbookCatalog.chapterById(selectedId) ?: V1TextbookCatalog.chapters.first()
    val practiceCount = V1TextbookCatalog.chapters.count { it.practiceLessonId in practiceCompletedIds }
    fun select(id: String) { selectedId = id; store.saveSelectedChapter(id) }

    Scaffold(
        modifier = Modifier.testTag("v1_textbook_root"),
        containerColor = AiBg,
        topBar = {
            AiCommandBar(
                chapter = selected,
                readCount = readCompleted.size,
                practiceCount = practiceCount,
                onOpenLab = onNavigateBack
            )
        }
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF090D17), AiBg, Color(0xFF060810))
                    )
                )
        ) {
            when (TextbookLayoutMode.fromWidthDp(maxWidth.value.toInt())) {
                TextbookLayoutMode.EXPANDED -> Row(
                    Modifier.fillMaxSize().testTag("textbook_layout_expanded")
                ) {
                    Toc(
                        selected.id, readCompleted, practiceCompletedIds, ::select,
                        Modifier.width(286.dp).fillMaxHeight()
                    )
                    Separator()
                    Reader(
                        selected, store, selected.id in readCompleted,
                        selected.practiceLessonId in practiceCompletedIds, false,
                        onRead = { store.markReadComplete(selected.id); readCompleted = store.readCompletedIds() },
                        onPractice = { onStartPractice(selected.practiceLessonId) },
                        onSelect = ::select,
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    Separator()
                    Insight(
                        selected, selected.id in readCompleted,
                        selected.practiceLessonId in practiceCompletedIds,
                        Modifier.width(318.dp).fillMaxHeight().testTag("textbook_insight_rail")
                    )
                }
                TextbookLayoutMode.MEDIUM -> Row(
                    Modifier.fillMaxSize().testTag("textbook_layout_medium")
                ) {
                    Toc(
                        selected.id, readCompleted, practiceCompletedIds, ::select,
                        Modifier.width(228.dp).fillMaxHeight()
                    )
                    Separator()
                    Reader(
                        selected, store, selected.id in readCompleted,
                        selected.practiceLessonId in practiceCompletedIds, true,
                        onRead = { store.markReadComplete(selected.id); readCompleted = store.readCompletedIds() },
                        onPractice = { onStartPractice(selected.practiceLessonId) },
                        onSelect = ::select,
                        Modifier.weight(1f).fillMaxHeight()
                    )
                }
                TextbookLayoutMode.COMPACT -> Column(
                    Modifier.fillMaxSize().testTag("textbook_layout_compact")
                ) {
                    CompactStrip(selected.id, readCompleted, ::select)
                    ThinDivider()
                    Reader(
                        selected, store, selected.id in readCompleted,
                        selected.practiceLessonId in practiceCompletedIds, true,
                        onRead = { store.markReadComplete(selected.id); readCompleted = store.readCompletedIds() },
                        onPractice = { onStartPractice(selected.practiceLessonId) },
                        onSelect = ::select,
                        Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AiCommandBar(
    chapter: TextbookChapter,
    readCount: Int,
    practiceCount: Int,
    onOpenLab: () -> Unit
) {
    Surface(color = Color(0xFF090E18), tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF102331),
                border = BorderStroke(1.dp, AiCyan.copy(alpha = .35f))
            ) {
                Text(
                    "✦", modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 20.sp, color = AiCyan, fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI CODING OS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = AiCyan, letterSpacing = 1.2.sp)
                    StatusPill("v1.2.0", AiViolet)
                    StatusPill("ONLINE", AiGreen)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "${chapter.number.toString().padStart(2, '0')} / 11  ${chapter.title}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("LEARNING MATRIX", fontSize = 9.sp, color = AiMuted, letterSpacing = .8.sp)
                Text("본문 $readCount/11  ·  실습 $practiceCount/11", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onOpenLab,
                border = BorderStroke(1.dp, AiBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AiCyan)
            ) { Text(">_ LAB", fontWeight = FontWeight.Bold) }
        }
        ThinDivider()
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = .12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = .35f))
    ) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Toc(
    selectedId: String,
    read: Set<String>,
    practices: Set<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
    Surface(modifier.testTag("textbook_toc"), color = Color(0xFF080D16)) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("KNOWLEDGE GRAPH", fontSize = 10.sp, color = AiCyan, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(4.dp))
                Text("11 Chapter", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("진도 · 실습 · 현재 위치 자동 동기화", fontSize = 11.sp, color = AiMuted)
                Spacer(Modifier.height(8.dp))
            }
            items(V1TextbookCatalog.chapters, key = { it.id }) { chapter ->
                val selected = chapter.id == selectedId
                val done = chapter.id in read
                val practiceDone = chapter.practiceLessonId in practices
                Surface(
                    Modifier.fillMaxWidth().clickable { onSelect(chapter.id) }.testTag("textbook_chapter_${chapter.id}"),
                    color = if (selected) Color(0xFF102231) else Color.Transparent,
                    border = BorderStroke(1.dp, if (selected) AiCyan.copy(alpha = .55f) else Color.Transparent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.padding(11.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) AiCyan.copy(alpha = .16f) else AiPanel2
                        ) {
                            Text(
                                chapter.number.toString().padStart(2, '0'),
                                Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) AiCyan else AiMuted
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(chapter.title, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "${if (done) "● READ" else "○ READ"}   ${if (practiceDone) "● RUN" else "○ RUN"}",
                                fontSize = 9.sp,
                                color = if (done && practiceDone) AiGreen else AiMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStrip(selectedId: String, read: Set<String>, onSelect: (String) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().background(Color(0xFF080D16)).padding(vertical = 9.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(V1TextbookCatalog.chapters, key = { it.id }) { chapter ->
            FilterChip(
                selected = chapter.id == selectedId,
                onClick = { onSelect(chapter.id) },
                label = { Text("${chapter.number}${if (chapter.id in read) " ✓" else ""}") }
            )
        }
    }
}

@Composable
private fun Reader(
    chapter: TextbookChapter,
    store: TextbookProgressStore,
    readComplete: Boolean,
    practiceComplete: Boolean,
    inlineGuide: Boolean,
    onRead: () -> Unit,
    onPractice: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val markdown = remember(chapter.id) { context.assets.open(chapter.assetPath).bufferedReader().use { it.readText() } }
    val blocks = remember(markdown) { TextbookMarkdownParser.parse(markdown) }
    key(chapter.id) {
        val state = rememberLazyListState(
            initialFirstVisibleItemIndex = store.scrollIndex(chapter.id).coerceAtMost(blocks.lastIndex.coerceAtLeast(0)),
            initialFirstVisibleItemScrollOffset = store.scrollOffset(chapter.id)
        )
        LaunchedEffect(chapter.id, state) {
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
                .distinctUntilChanged()
                .collect { (i, o) -> store.saveScroll(chapter.id, i, o) }
        }
        LazyColumn(
            modifier = modifier.testTag("textbook_reader"),
            state = state,
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item(key = "head-${chapter.id}") {
                Column(Modifier.fillMaxWidth().widthIn(max = 820.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("CHAPTER ${chapter.number.toString().padStart(2, '0')}", AiCyan)
                        StatusPill(if (readComplete) "READ ✓" else "READING", if (readComplete) AiGreen else AiViolet)
                        StatusPill(if (practiceComplete) "RUN ✓" else "PRACTICE", if (practiceComplete) AiGreen else AiViolet)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(chapter.title, Modifier.testTag("textbook_chapter_title"), fontSize = 31.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(9.dp))
                    Text(chapter.summary, fontSize = 15.sp, lineHeight = 25.sp, color = AiMuted)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { chapter.number / 11f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = AiCyan,
                        trackColor = AiPanel2
                    )
                }
            }
            if (inlineGuide) item(key = "guide-${chapter.id}") {
                InlineGuide(chapter, readComplete, practiceComplete, Modifier.fillMaxWidth().widthIn(max = 820.dp))
            }
            itemsIndexed(blocks, key = { index, _ -> "${chapter.id}-$index" }) { _, block ->
                BlockView(block, Modifier.fillMaxWidth().widthIn(max = 820.dp))
            }
            item(key = "actions-${chapter.id}") {
                Surface(
                    Modifier.fillMaxWidth().widthIn(max = 820.dp),
                    color = Color(0xFF101B27),
                    border = BorderStroke(1.dp, AiCyan.copy(alpha = .3f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("EXECUTION GATE", fontSize = 10.sp, color = AiCyan, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Text("읽었으면 끝이 아니라, 직접 실행해서 증명", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("예상 → 실행 → 디버깅 → AI 판별 → 응용 → 설명의 10단계 루프를 통과합니다.", fontSize = 14.sp, lineHeight = 22.sp, color = AiMuted)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Button(
                                onClick = onRead,
                                enabled = !readComplete,
                                modifier = Modifier.weight(1f).testTag("textbook_mark_read")
                            ) { Text(if (readComplete) "본문 완료 ✓" else "본문 완료") }
                            Button(
                                onClick = onPractice,
                                modifier = Modifier.weight(1f).testTag("textbook_practice_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = AiViolet, contentColor = Color(0xFF0C0720))
                            ) { Text(if (practiceComplete) "실습 재실행 ✓" else ">_ 10단계 실행") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            OutlinedButton(
                                onClick = { V1TextbookCatalog.chapters.getOrNull(chapter.number - 2)?.let { onSelect(it.id) } },
                                enabled = chapter.number > 1,
                                modifier = Modifier.weight(1f)
                            ) { Text("← PREV") }
                            OutlinedButton(
                                onClick = { V1TextbookCatalog.chapters.getOrNull(chapter.number)?.let { onSelect(it.id) } },
                                enabled = chapter.number < 11,
                                modifier = Modifier.weight(1f).testTag("textbook_next_chapter")
                            ) { Text("NEXT →") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockView(block: TextbookBlock, modifier: Modifier) {
    when (block) {
        is TextbookBlock.Heading -> {
            val size = when (block.level) { 1 -> 27.sp; 2 -> 24.sp; 3 -> 20.sp; else -> 17.sp }
            Column(modifier.padding(top = 9.dp)) {
                if (block.level <= 2) Box(Modifier.width(34.dp).height(3.dp).background(AiCyan, RoundedCornerShape(9.dp)))
                if (block.level <= 2) Spacer(Modifier.height(8.dp))
                Text(block.text, fontSize = size, lineHeight = when (block.level) { 1 -> 34.sp; 2 -> 31.sp; 3 -> 27.sp; else -> 24.sp }, fontWeight = FontWeight.Bold)
            }
        }
        is TextbookBlock.Paragraph -> Text(block.text, modifier, fontSize = 16.sp, lineHeight = 28.sp, color = Color(0xFFE7ECF5))
        is TextbookBlock.BulletList -> Surface(
            modifier,
            color = AiPanel,
            border = BorderStroke(1.dp, AiBorder),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.items.forEachIndexed { i, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(if (block.ordered) "${i + 1}." else "›", Modifier.width(28.dp), color = AiCyan, fontWeight = FontWeight.Bold)
                        Text(item, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 24.sp)
                    }
                }
            }
        }
        is TextbookBlock.Code -> Surface(
            modifier,
            color = Color(0xFF050910),
            border = BorderStroke(1.dp, Color(0xFF213049)),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF0E1624)).padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("● ● ●", fontSize = 10.sp, color = AiMuted)
                    Spacer(Modifier.width(10.dp))
                    Text(if (block.language.isBlank()) "CODE" else block.language.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AiCyan)
                    Spacer(Modifier.weight(1f))
                    Text("EXECUTION CELL", fontSize = 9.sp, color = AiMuted)
                }
                SelectionContainer {
                    Text(
                        block.text,
                        Modifier.fillMaxWidth().padding(14.dp).horizontalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        softWrap = false,
                        color = Color(0xFFD7F8FF)
                    )
                }
            }
        }
        is TextbookBlock.Table -> Surface(
            modifier,
            color = AiPanel,
            border = BorderStroke(1.dp, AiBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                block.rows.forEachIndexed { ri, row ->
                    if (ri > 0) ThinDivider()
                    Column(Modifier.padding(13.dp)) {
                        block.headers.forEachIndexed { ci, h ->
                            row.getOrNull(ci)?.takeIf { it.isNotBlank() }?.let {
                                Text(h.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = AiCyan, letterSpacing = .7.sp)
                                Text(it, fontSize = 14.sp, lineHeight = 21.sp)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
        TextbookBlock.Divider -> ThinDivider(modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun Insight(chapter: TextbookChapter, read: Boolean, practice: Boolean, modifier: Modifier) {
    Surface(modifier, color = Color(0xFF080D16)) {
        Column(Modifier.fillMaxSize().padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("AI COPILOT CONTEXT", fontSize = 10.sp, color = AiViolet, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("학습 컨텍스트", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Status("본문 이해", read)
            Status("실행 실습", practice)
            ThinDivider()
            Text("CORE TOKENS", fontSize = 10.sp, color = AiMuted, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            chapter.keyConcepts.forEach { concept ->
                Surface(color = AiPanel2, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AiBorder)) {
                    Text("# $concept", Modifier.fillMaxWidth().padding(10.dp), fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            ThinDivider()
            Guide("HUMAN DECISION", chapter.humanMustKnow, Color(0xFF16313B), AiCyan)
            Guide("AI DELEGATION", chapter.aiCanHelp, Color(0xFF282241), AiViolet)
            Spacer(Modifier.weight(1f))
            Text("SOURCE · ${chapter.sourceLessonIds.joinToString(" / ")}", fontSize = 9.sp, color = AiMuted)
        }
    }
}

@Composable
private fun InlineGuide(chapter: TextbookChapter, read: Boolean, practice: Boolean, modifier: Modifier) {
    Surface(modifier, color = AiPanel, border = BorderStroke(1.dp, AiBorder), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("AI CONTEXT WINDOW", fontSize = 10.sp, color = AiViolet, fontWeight = FontWeight.ExtraBold, letterSpacing = .8.sp)
            Text("본문 ${if (read) "✓" else "○"}  ·  실습 ${if (practice) "✓" else "○"}", fontWeight = FontWeight.Bold)
            Text("핵심: ${chapter.keyConcepts.joinToString(" · ")}", fontSize = 13.sp, lineHeight = 20.sp, color = AiMuted)
            Text("사람이 판단: ${chapter.humanMustKnow}", fontSize = 13.sp, lineHeight = 20.sp)
            Text("AI 보조: ${chapter.aiCanHelp}", fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun Guide(title: String, text: String, color: Color, accent: Color) {
    Surface(color = color, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Column(Modifier.padding(13.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = accent, letterSpacing = .8.sp)
            Spacer(Modifier.height(5.dp))
            Text(text, fontSize = 12.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun Status(label: String, done: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = AiMuted)
        StatusPill(if (done) "PASS" else "WAIT", if (done) AiGreen else AiViolet)
    }
}

@Composable
private fun ThinDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1B2639)))
}

@Composable
private fun Separator() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF1B2639)))
}
