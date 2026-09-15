package com.futuretech.poweruser.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.*
import kotlinx.coroutines.flow.distinctUntilChanged

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
    fun select(id: String) { selectedId = id; store.saveSelectedChapter(id) }

    Scaffold(
        modifier = Modifier.testTag("v1_textbook_root"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(V1TextbookCatalog.BOOK_TITLE, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("본문 ${readCompleted.size}/11 · 실습 ${V1TextbookCatalog.chapters.count { it.practiceLessonId in practiceCompletedIds }}/11", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 홈") } }
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            when (TextbookLayoutMode.fromWidthDp(maxWidth.value.toInt())) {
                TextbookLayoutMode.EXPANDED -> Row(Modifier.fillMaxSize().testTag("textbook_layout_expanded")) {
                    Toc(selected.id, readCompleted, practiceCompletedIds, ::select, Modifier.width(260.dp).fillMaxHeight())
                    Separator()
                    Reader(selected, store, selected.id in readCompleted, selected.practiceLessonId in practiceCompletedIds, false,
                        onRead = { store.markReadComplete(selected.id); readCompleted = store.readCompletedIds() },
                        onPractice = { onStartPractice(selected.practiceLessonId) },
                        onSelect = ::select, Modifier.weight(1f).fillMaxHeight())
                    Separator()
                    Insight(selected, selected.id in readCompleted, selected.practiceLessonId in practiceCompletedIds,
                        Modifier.width(300.dp).fillMaxHeight().testTag("textbook_insight_rail"))
                }
                TextbookLayoutMode.MEDIUM -> Row(Modifier.fillMaxSize().testTag("textbook_layout_medium")) {
                    Toc(selected.id, readCompleted, practiceCompletedIds, ::select, Modifier.width(220.dp).fillMaxHeight())
                    Separator()
                    Reader(selected, store, selected.id in readCompleted, selected.practiceLessonId in practiceCompletedIds, true,
                        onRead = { store.markReadComplete(selected.id); readCompleted = store.readCompletedIds() },
                        onPractice = { onStartPractice(selected.practiceLessonId) },
                        onSelect = ::select, Modifier.weight(1f).fillMaxHeight())
                }
                TextbookLayoutMode.COMPACT -> Column(Modifier.fillMaxSize().testTag("textbook_layout_compact")) {
                    CompactStrip(selected.id, readCompleted, ::select)
                    ThinDivider()
                    Reader(selected, store, selected.id in readCompleted, selected.practiceLessonId in practiceCompletedIds, true,
                        onRead = { store.markReadComplete(selected.id); readCompleted = store.readCompletedIds() },
                        onPractice = { onStartPractice(selected.practiceLessonId) },
                        onSelect = ::select, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Toc(selectedId: String, read: Set<String>, practices: Set<String>, onSelect: (String) -> Unit, modifier: Modifier) {
    Surface(modifier.testTag("textbook_toc"), color = MaterialTheme.colorScheme.surface) {
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("목차", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("11 Chapter · 위치 자동 저장", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) }
            items(V1TextbookCatalog.chapters, key = { it.id }) { chapter ->
                val selected = chapter.id == selectedId
                Card(
                    Modifier.fillMaxWidth().clickable { onSelect(chapter.id) }.testTag("textbook_chapter_${chapter.id}"),
                    colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))
                ) {
                    Column(Modifier.padding(11.dp)) {
                        Text("${chapter.number.toString().padStart(2,'0')} · ${if (chapter.id in read) "본문✓" else "본문○"} ${if (chapter.practiceLessonId in practices) "실습✓" else "실습○"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        Text(chapter.title, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStrip(selectedId: String, read: Set<String>, onSelect: (String) -> Unit) {
    LazyRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(V1TextbookCatalog.chapters, key = { it.id }) { chapter ->
            FilterChip(selected = chapter.id == selectedId, onClick = { onSelect(chapter.id) }, label = { Text("${chapter.number}${if (chapter.id in read) " ✓" else ""}") })
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
        val state = rememberLazyListState(initialFirstVisibleItemIndex = store.scrollIndex(chapter.id).coerceAtMost(blocks.lastIndex.coerceAtLeast(0)), initialFirstVisibleItemScrollOffset = store.scrollOffset(chapter.id))
        LaunchedEffect(chapter.id, state) {
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.distinctUntilChanged().collect { (i, o) -> store.saveScroll(chapter.id, i, o) }
        }
        LazyColumn(
            modifier = modifier.testTag("textbook_reader"), state = state,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item(key = "head-${chapter.id}") {
                Column(Modifier.fillMaxWidth().widthIn(max = 780.dp)) {
                    Text("Chapter ${chapter.number.toString().padStart(2,'0')}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(chapter.title, Modifier.testTag("textbook_chapter_title"), fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp)); Text(chapter.summary, fontSize = 15.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { chapter.number / 11f }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (inlineGuide) item(key = "guide-${chapter.id}") { InlineGuide(chapter, readComplete, practiceComplete, Modifier.fillMaxWidth().widthIn(max = 780.dp)) }
            itemsIndexed(blocks, key = { index, _ -> "${chapter.id}-$index" }) { _, block -> BlockView(block, Modifier.fillMaxWidth().widthIn(max = 780.dp)) }
            item(key = "actions-${chapter.id}") {
                Card(Modifier.fillMaxWidth().widthIn(max = 780.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("이 Chapter를 끝내는 기준", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("읽기만 완료가 아닙니다. 본문 뒤 10단계 실습에서 예상→실행→디버깅→AI 판별→응용→설명까지 통과합니다.", fontSize = 14.sp, lineHeight = 22.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRead, enabled = !readComplete, modifier = Modifier.weight(1f).testTag("textbook_mark_read")) { Text(if (readComplete) "본문 읽음 ✓" else "본문 읽기 완료") }
                            Button(onClick = onPractice, modifier = Modifier.weight(1f).testTag("textbook_practice_button")) { Text(if (practiceComplete) "실습 다시보기 ✓" else "10단계 실습 시작") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { V1TextbookCatalog.chapters.getOrNull(chapter.number - 2)?.let { onSelect(it.id) } }, enabled = chapter.number > 1, modifier = Modifier.weight(1f)) { Text("← 이전") }
                            OutlinedButton(onClick = { V1TextbookCatalog.chapters.getOrNull(chapter.number)?.let { onSelect(it.id) } }, enabled = chapter.number < 11, modifier = Modifier.weight(1f).testTag("textbook_next_chapter")) { Text("다음 →") }
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
            Text(block.text, modifier.padding(top = 5.dp), fontSize = size, lineHeight = when (block.level) { 1 -> 34.sp; 2 -> 31.sp; 3 -> 27.sp; else -> 24.sp }, fontWeight = FontWeight.Bold)
        }
        is TextbookBlock.Paragraph -> Text(block.text, modifier, fontSize = 16.sp, lineHeight = 27.sp)
        is TextbookBlock.BulletList -> Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { block.items.forEachIndexed { i, item -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { Text(if (block.ordered) "${i+1}." else "•", Modifier.width(28.dp), fontWeight = FontWeight.Bold); Text(item, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 24.sp) } } }
        }
        is TextbookBlock.Code -> Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(12.dp)) { if (block.language.isNotBlank()) Text(block.language.uppercase(), fontSize=10.sp, fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.primary); Spacer(Modifier.height(5.dp)); SelectionContainer { Text(block.text, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), fontFamily=FontFamily.Monospace, fontSize=13.sp, lineHeight=20.sp, softWrap=false) } }
        }
        is TextbookBlock.Table -> Card(modifier) { Column(Modifier.fillMaxWidth()) { block.rows.forEachIndexed { ri, row -> if (ri>0) ThinDivider(); Column(Modifier.padding(12.dp)) { block.headers.forEachIndexed { ci, h -> row.getOrNull(ci)?.takeIf { it.isNotBlank() }?.let { Text(h, fontSize=10.sp, fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.primary); Text(it, fontSize=14.sp, lineHeight=21.sp); Spacer(Modifier.height(5.dp)) } } } } } }
        TextbookBlock.Divider -> ThinDivider(modifier.padding(vertical=5.dp))
    }
}

@Composable
private fun Insight(chapter: TextbookChapter, read: Boolean, practice: Boolean, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("학습 가이드", fontSize=18.sp, fontWeight=FontWeight.Bold); Status("본문",read); Status("10단계 실습",practice); ThinDivider(); Text("핵심 개념", fontSize=13.sp, fontWeight=FontWeight.Bold); chapter.keyConcepts.forEach { Text("• $it", fontSize=13.sp, lineHeight=19.sp) }; ThinDivider(); Guide("사람이 반드시 판단",chapter.humanMustKnow,MaterialTheme.colorScheme.primaryContainer); Guide("AI에게 맡겨도 됨",chapter.aiCanHelp,MaterialTheme.colorScheme.secondaryContainer); Spacer(Modifier.weight(1f)); Text("원본 Lesson ${chapter.sourceLessonIds.joinToString(", ")}", fontSize=10.sp, color=MaterialTheme.colorScheme.outline)
    } }
}

@Composable
private fun InlineGuide(chapter: TextbookChapter, read:Boolean, practice:Boolean, modifier:Modifier) { Card(modifier, colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.5f))) { Column(Modifier.padding(15.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("학습 가이드 · 본문 ${if(read) "✓" else "○"} · 실습 ${if(practice) "✓" else "○"}", fontWeight=FontWeight.Bold); Text("핵심: ${chapter.keyConcepts.joinToString(" · ")}", fontSize=13.sp, lineHeight=20.sp); Text("사람이 판단: ${chapter.humanMustKnow}", fontSize=13.sp, lineHeight=20.sp); Text("AI 보조: ${chapter.aiCanHelp}", fontSize=13.sp, lineHeight=20.sp) } } }
@Composable private fun Guide(title:String,text:String,color:androidx.compose.ui.graphics.Color) { Surface(color=color,shape=RoundedCornerShape(12.dp)) { Column(Modifier.padding(12.dp)) { Text(title,fontSize=12.sp,fontWeight=FontWeight.Bold); Text(text,fontSize=12.sp,lineHeight=18.sp) } } }
@Composable private fun Status(label:String,done:Boolean) { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text(label,fontSize=13.sp); Text(if(done) "완료 ✓" else "진행 전",fontSize=12.sp,fontWeight=FontWeight.Bold,color=if(done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline) } }
@Composable private fun ThinDivider(modifier:Modifier=Modifier) { Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant)) }
@Composable private fun Separator() { Box(Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant)) }
