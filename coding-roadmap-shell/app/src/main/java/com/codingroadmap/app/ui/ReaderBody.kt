package com.codingroadmap.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ContentPage

@Composable
fun ReaderBody(
    chapter: Int,
    chapterCount: Int,
    page: Int,
    pageCount: Int,
    pageData: ContentPage,
    scale: Float,
    canPrev: Boolean,
    canNext: Boolean,
    focusMode: Boolean,
    onToggleFocus: () -> Unit,
    modifier: Modifier = Modifier,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    var showAnswer by remember(chapter, page) { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val tabS10FePortraitProfile =
        configuration.smallestScreenWidthDp >= 600 &&
        configuration.screenHeightDp >= 900 &&
        configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Galaxy Tab S10 FE 10.9" portrait: keep the same readable size,
    // but tighten line/section rhythm enough that a normal page fits without scrolling.
    val readerScale = scale * if (tabS10FePortraitProfile) 0.94f else 1f
    val horizontalPadding = if (tabS10FePortraitProfile) 22.dp else 27.dp
    val verticalPadding = if (tabS10FePortraitProfile) 10.dp else 18.dp
    val firstParagraphGap = if (tabS10FePortraitProfile) 12.dp else 16.dp
    val paragraphGap = if (tabS10FePortraitProfile) 12.dp else 18.dp
    val sectionGap = if (tabS10FePortraitProfile) 14.dp else 20.dp
    val bottomGap = if (tabS10FePortraitProfile) 18.dp else 28.dp

    val scrollState = rememberScrollState()

    LaunchedEffect(chapter, page) {
        scrollState.scrollTo(0)
    }

    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            Text(
                pageData.eyebrow,
                fontFamily = EditorialSerif,
                color = ReaderGold,
                fontSize = (13 * readerScale).sp
            )
            Text(
                pageData.title,
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (31 * readerScale).sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (40 * readerScale).sp,
                modifier = Modifier.padding(top = if (tabS10FePortraitProfile) 7.dp else 10.dp)
            )
            Text(
                "—",
                fontFamily = EditorialSerif,
                color = ReaderGold,
                fontSize = (21 * readerScale).sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (pageData.keywords.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (tabS10FePortraitProfile) 4.dp else 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    pageData.keywords.forEach { keyword ->
                        Text(
                            keyword,
                            color = ReaderText,
                            fontSize = (11 * readerScale).sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2A2722))
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = if (tabS10FePortraitProfile) 5.dp else 6.dp
                                )
                        )
                    }
                }
            }

            pageData.paragraphs.forEachIndexed { index, paragraph ->
                Text(
                    paragraph,
                    fontFamily = EditorialSerif,
                    color = ReaderText,
                    fontSize = (16 * readerScale).sp,
                    lineHeight = (28 * readerScale).sp,
                    modifier = Modifier.padding(
                        top = if (index == 0) firstParagraphGap else paragraphGap
                    )
                )
            }

            if (pageData.glossary.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(top = sectionGap),
                    verticalArrangement = Arrangement.spacedBy(
                        if (tabS10FePortraitProfile) 9.dp else 12.dp
                    )
                ) {
                    pageData.glossary.forEach { entry ->
                        GlossaryCard(
                            term = entry.term,
                            description = entry.description,
                            usage = entry.usage,
                            example = entry.example,
                            scale = readerScale
                        )
                    }
                }
            }

            pageData.visual?.let {
                ConceptDiagram(
                    it,
                    readerScale,
                    Modifier.padding(top = sectionGap)
                )
            }

            if (pageData.bullets.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = sectionGap)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ReaderSurface)
                        .padding(if (tabS10FePortraitProfile) 14.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        if (tabS10FePortraitProfile) 7.dp else 10.dp
                    )
                ) {
                    pageData.bullets.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "•",
                                color = ReaderGold,
                                fontSize = (17 * readerScale).sp,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                item,
                                fontFamily = EditorialSerif,
                                color = ReaderText,
                                fontSize = (14 * readerScale).sp,
                                lineHeight = (22 * readerScale).sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            pageData.calloutTitle?.let { title ->
                CalloutCard(
                    title = title,
                    body = pageData.calloutBody.orEmpty(),
                    scale = readerScale,
                    modifier = Modifier.padding(top = sectionGap)
                )
            }

            pageData.code?.let { code ->
                CodeCard(
                    code = code,
                    note = pageData.codeNote,
                    scale = readerScale,
                    modifier = Modifier.padding(top = sectionGap)
                )
            }

            pageData.practicePrompt?.let { prompt ->
                InfoCard(
                    label = "직접 해보기",
                    text = prompt,
                    accent = ReaderGold,
                    scale = readerScale,
                    modifier = Modifier.padding(top = sectionGap)
                )
            }

            pageData.question?.let { question ->
                InfoCard(
                    label = "문제",
                    text = question,
                    accent = Color(0xFF93B7D7),
                    scale = readerScale,
                    modifier = Modifier.padding(top = sectionGap)
                )
            }

            if (pageData.answer != null) {
                TextButton(
                    onClick = { showAnswer = !showAnswer },
                    modifier = Modifier.padding(top = if (tabS10FePortraitProfile) 6.dp else 10.dp)
                ) {
                    Text(
                        if (showAnswer) "정답·해설 접기  ▲" else "정답·해설 보기  ▼",
                        color = ReaderGold,
                        fontFamily = EditorialSerif,
                        fontSize = (14 * readerScale).sp
                    )
                }

                if (showAnswer) {
                    InfoCard(
                        label = "정답·해설",
                        text = pageData.answer,
                        accent = Color(0xFF9BB58B),
                        scale = readerScale,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    pageData.mistake?.let { mistake ->
                        InfoCard(
                            label = "자주 틀리는 이유",
                            text = mistake,
                            accent = Color(0xFFD19B76),
                            scale = readerScale,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }

            if (pageData.extras.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(top = sectionGap),
                    verticalArrangement = Arrangement.spacedBy(
                        if (tabS10FePortraitProfile) 9.dp else 12.dp
                    )
                ) {
                    pageData.extras.forEach { section ->
                        InfoCard(
                            label = section.title,
                            text = section.body,
                            accent = ReaderGold,
                            scale = readerScale
                        )
                    }
                }
            }

            pageData.closingPrompt?.let { prompt ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (tabS10FePortraitProfile) 8.dp else 12.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF1B1814))
                        .padding(
                            horizontal = 14.dp,
                            vertical = if (tabS10FePortraitProfile) 9.dp else 11.dp
                        ),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "✓",
                        color = ReaderGold,
                        fontSize = (13 * readerScale).sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        prompt,
                        fontFamily = EditorialSerif,
                        color = ReaderMuted,
                        fontSize = (12 * readerScale).sp,
                        lineHeight = (18 * readerScale).sp
                    )
                }
            }

            Spacer(Modifier.height(bottomGap))
        }

        if (!focusMode) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.08f)))
            Row(
                Modifier.fillMaxWidth().padding(
                    horizontal = 14.dp,
                    vertical = if (tabS10FePortraitProfile) 7.dp else 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(enabled = canPrev, onClick = onPrev) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            "이전",
                            tint = if (canPrev) ReaderText else ReaderMuted.copy(.3f)
                        )
                    }
                    Text("이전", fontFamily = EditorialSerif, color = ReaderMuted, fontSize = 11.sp)
                }

                Column(
                    Modifier.weight(1f).padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Chapter ${chapter + 1}/$chapterCount · Page ${page + 1}/$pageCount",
                        fontFamily = EditorialSerif,
                        color = ReaderMuted,
                        fontSize = 11.sp
                    )
                    ProgressBar(
                        (page + 1) / pageCount.toFloat(),
                        Modifier.padding(top = 8.dp),
                        dark = true
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(enabled = canNext, onClick = onNext) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            "다음",
                            tint = if (canNext) ReaderText else ReaderMuted.copy(.3f)
                        )
                    }
                    Text("다음", fontFamily = EditorialSerif, color = ReaderMuted, fontSize = 11.sp)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(start = 7.dp)
                        .clickable(onClick = onToggleFocus)
                ) {
                    Icon(Icons.Rounded.MenuBook, null, tint = ReaderGold, modifier = Modifier.size(25.dp))
                    Text(
                        "집중모드",
                        fontFamily = EditorialSerif,
                        color = ReaderMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }

}

@Composable
private fun GlossaryCard(
    term: String,
    description: String,
    usage: String,
    example: String,
    scale: Float
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF211D17))
            .padding(16.dp)
    ) {
        Text(
            term,
            fontFamily = EditorialSerif,
            color = ReaderGold,
            fontWeight = FontWeight.SemiBold,
            fontSize = (17 * scale).sp
        )
        Text(
            description,
            fontFamily = EditorialSerif,
            color = ReaderText,
            fontSize = (14 * scale).sp,
            lineHeight = (22 * scale).sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "어디서 쓰나요",
            color = Color(0xFF9BB58B),
            fontSize = (11 * scale).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            usage,
            fontFamily = EditorialSerif,
            color = ReaderText,
            fontSize = (13 * scale).sp,
            lineHeight = (20 * scale).sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "간단한 예시",
            color = Color(0xFF93B7D7),
            fontSize = (11 * scale).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 9.dp)
        )
        Text(
            example,
            fontFamily = FontFamily.Monospace,
            color = ReaderText,
            fontSize = (12 * scale).sp,
            lineHeight = (19 * scale).sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CodeCard(
    code: String,
    note: String?,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF191817))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF24211D))
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Python",
                fontFamily = EditorialSerif,
                color = ReaderMuted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(code)) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Rounded.ContentCopy, "코드 복사", tint = ReaderText, modifier = Modifier.size(17.dp))
            }
            Text("복사", color = ReaderText, fontSize = 11.sp)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(15.dp)
        ) {
            Text(
                code,
                fontFamily = FontFamily.Monospace,
                color = ReaderText,
                fontSize = (14 * scale).sp,
                lineHeight = (22 * scale).sp
            )
        }

        note?.let {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.07f)))
            Text(
                it,
                fontFamily = EditorialSerif,
                color = ReaderMuted,
                fontSize = (12 * scale).sp,
                lineHeight = (19 * scale).sp,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun CalloutCard(
    title: String,
    body: String,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF30291F))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeafMark(Modifier.size(48.dp), ReaderGold)
        Box(
            Modifier
                .padding(horizontal = 15.dp)
                .width(1.dp)
                .height(68.dp)
                .background(Color(0xFF6D5B40))
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = EditorialSerif,
                fontSize = (15 * scale).sp,
                color = ReaderGold,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                body,
                fontFamily = EditorialSerif,
                fontSize = (14 * scale).sp,
                lineHeight = (22 * scale).sp,
                color = ReaderText,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun InfoCard(
    label: String,
    text: String,
    accent: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ReaderSurface2)
            .padding(16.dp)
    ) {
        Text(
            label,
            color = accent,
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text,
            fontFamily = EditorialSerif,
            color = ReaderText,
            fontSize = (14 * scale).sp,
            lineHeight = (23 * scale).sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ConceptDiagram(
    visual: String,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF211D17))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        when (visual) {
            "CODE_TO_OUTPUT" -> FlowNodes(listOf("사람이 코드 작성", "Python 실행", "결과 출력"), scale)
            "TOP_TO_BOTTOM" -> FlowNodes(listOf("1번 줄", "2번 줄", "3번 줄", "결과"), scale)
            "VARIABLE_BINDING" -> {
                PairNode("name", "민수", scale)
                PairNode("age", "37", scale)
                PairNode("age 재대입", "38", scale)
            }
            "TYPE_TABLE" -> {
                PairNode("10", "int · 정수", scale)
                PairNode("10.0", "float · 실수", scale)
                PairNode("\"10\"", "str · 문자열", scale)
                PairNode("True", "bool · 참/거짓", scale)
            }
            "INPUT_CONVERSION" -> FlowNodes(listOf("input()", "str", "int / float", "계산", "출력"), scale)
            "ERROR_MAP" -> {
                PairNode("SyntaxError", "문법", scale)
                PairNode("NameError", "이름·순서", scale)
                PairNode("TypeError", "타입", scale)
                PairNode("ValueError", "값 형식", scale)
            }
            "CALC_STEPS" -> FlowNodes(listOf("price × count", "subtotal", "+ shipping", "total"), scale)
            "IPO_PROJECT" -> FlowNodes(listOf("INPUT", "PROCESS", "OUTPUT", "VERIFY"), scale)
        }
    }
}

@Composable
private fun FlowNodes(nodes: List<String>, scale: Float) {
    nodes.forEachIndexed { index, node ->
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF2B2720))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                node,
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (13 * scale).sp
            )
        }
        if (index != nodes.lastIndex) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "↓",
                    color = ReaderGold,
                    fontSize = (17 * scale).sp
                )
            }
        }
    }
}

@Composable
private fun PairNode(left: String, right: String, scale: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF2B2720))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            left,
            fontFamily = FontFamily.Monospace,
            color = ReaderGold,
            fontSize = (13 * scale).sp,
            modifier = Modifier.weight(.8f)
        )
        Text("→", color = ReaderMuted)
        Text(
            right,
            fontFamily = EditorialSerif,
            color = ReaderText,
            fontSize = (13 * scale).sp,
            modifier = Modifier.weight(1.2f).padding(start = 10.dp)
        )
    }
}
