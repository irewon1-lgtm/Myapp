package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.futuretech.poweruser.textbook.TextbookBlock
import com.futuretech.poweruser.textbook.TextbookChapter
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
        val restoredIndex = store.selectedSectionIndex(selected.id).coerceIn(0, sections.lastIndex.coerceAtLeast(0))
        var sectionIndex by rememberSaveable(selected.id) { mutableStateOf(restoredIndex) }
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
                key(selected.id, currentSection.id) {
                    SectionReader(
                        chapter = selected,
                        section = currentSection,
                        sectionCount = sections.size,
                        readComplete = selected.id in readCompleted,
                        practiceComplete = selected.practiceLessonId in practiceCompletedIds,
                        onPreviousSection = if (sectionIndex > 0) {
                            {
                                sectionIndex -= 1
                                store.saveSelectedSectionIndex(selected.id, sectionIndex)
                            }
                        } else null,
                        onNextSection = if (sectionIndex < sections.lastIndex) {
                            {
                                sectionIndex += 1
                                store.saveSelectedSectionIndex(selected.id, sectionIndex)
                            }
                        } else null,
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
private fun SectionReader(
    chapter: TextbookChapter,
    section: TextbookSection,
    sectionCount: Int,
    readComplete: Boolean,
    practiceComplete: Boolean,
    onPreviousSection: (() -> Unit)?,
    onNextSection: (() -> Unit)?,
    onMarkRead: () -> Unit,
    onPractice: () -> Unit,
    onPreviousChapter: (() -> Unit)?,
    onNextChapter: (() -> Unit)?
) {
    val state = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("textbook_reader"),
        state = state,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "section-head-${section.id}") {
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
                    text = section.title,
                    modifier = Modifier.testTag("textbook_section_title"),
                    color = ReaderInk,
                    fontSize = 28.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = chapter.summary,
                    color = ReaderMuted,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        itemsIndexed(section.blocks, key = { index, _ -> "${section.id}-block-$index" }) { _, block ->
            BlockView(block, Modifier.fillMaxWidth().widthIn(max = 780.dp))
        }

        item(key = "section-actions-${section.id}") {
            SectionActions(
                chapter = chapter,
                isLastSection = section.index == sectionCount - 1,
                readComplete = readComplete,
                practiceComplete = practiceComplete,
                onPreviousSection = onPreviousSection,
                onNextSection = onNextSection,
                onMarkRead = onMarkRead,
                onPractice = onPractice,
                onPreviousChapter = onPreviousChapter,
                onNextChapter = onNextChapter
            )
        }
    }
}

@Composable
private fun SectionActions(
    chapter: TextbookChapter,
    isLastSection: Boolean,
    readComplete: Boolean,
    practiceComplete: Boolean,
    onPreviousSection: (() -> Unit)?,
    onNextSection: (() -> Unit)?,
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
                text = if (isLastSection) "이 Chapter를 문제로 확인합니다." else "읽은 내용을 짧게 확인하거나 다음 Section으로 이동합니다.",
                color = ReaderInk,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPractice,
                    modifier = Modifier.weight(1f).testTag("textbook_practice_button"),
                    border = BorderStroke(1.dp, ReaderAccent)
                ) {
                    Text(if (practiceComplete) "문제·실습 다시 풀기" else "문제·실습", color = ReaderAccent)
                }
                if (isLastSection) {
                    Button(
                        onClick = onMarkRead,
                        enabled = !readComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                    ) {
                        Text(if (readComplete) "읽기 완료 ✓" else "Chapter 읽기 완료")
                    }
                } else {
                    Button(
                        onClick = { onNextSection?.invoke() },
                        enabled = onNextSection != null,
                        modifier = Modifier.weight(1f).testTag("textbook_next_section"),
                        colors = ButtonDefaults.buttonColors(containerColor = ReaderAccent)
                    ) {
                        Text("다음 Section")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onPreviousSection?.invoke() },
                    enabled = onPreviousSection != null,
                    modifier = Modifier.weight(1f)
                ) { Text("이전 Section") }
                if (isLastSection) {
                    OutlinedButton(
                        onClick = { onNextChapter?.invoke() },
                        enabled = onNextChapter != null,
                        modifier = Modifier.weight(1f).testTag("textbook_next_chapter")
                    ) { Text("다음 Chapter") }
                } else {
                    OutlinedButton(
                        onClick = { onNextSection?.invoke() },
                        enabled = onNextSection != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("계속 읽기") }
                }
            }
            if (isLastSection && onPreviousChapter != null && chapter.number > 1) {
                OutlinedButton(onClick = onPreviousChapter, modifier = Modifier.fillMaxWidth()) {
                    Text("이전 Chapter")
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

        is TextbookBlock.Code -> Surface(
            modifier = modifier,
            color = ReaderCode,
            border = BorderStroke(1.dp, Color(0xFF343A44)),
            shape = RoundedCornerShape(13.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF20242B)).padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (block.language.isBlank()) "CODE" else block.language.uppercase(),
                        color = Color(0xFFB8C7DF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                SelectionContainer {
                    Text(
                        text = block.text,
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
