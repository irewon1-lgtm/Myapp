package com.codingroadmap.app.ui

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

    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 27.dp, vertical = 18.dp)
        ) {
            Text(
                pageData.eyebrow,
                fontFamily = EditorialSerif,
                color = ReaderGold,
                fontSize = (13 * scale).sp
            )
            Text(
                pageData.title,
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (31 * scale).sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (40 * scale).sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "—",
                fontFamily = EditorialSerif,
                color = ReaderGold,
                fontSize = (21 * scale).sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (pageData.keywords.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    pageData.keywords.forEach { keyword ->
                        Text(
                            keyword,
                            color = ReaderText,
                            fontSize = (11 * scale).sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2A2722))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            pageData.paragraphs.forEachIndexed { index, paragraph ->
                Text(
                    paragraph,
                    fontFamily = EditorialSerif,
                    color = ReaderText,
                    fontSize = (16 * scale).sp,
                    lineHeight = (28 * scale).sp,
                    modifier = Modifier.padding(top = if (index == 0) 16.dp else 18.dp)
                )
            }

            pageData.visual?.let {
                ConceptDiagram(it, scale, Modifier.padding(top = 22.dp))
            }

            if (pageData.bullets.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ReaderSurface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pageData.bullets.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = ReaderGold, fontSize = (17 * scale).sp, modifier = Modifier.width(20.dp))
                            Text(
                                item,
                                fontFamily = EditorialSerif,
                                color = ReaderText,
                                fontSize = (14 * scale).sp,
                                lineHeight = (22 * scale).sp,
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
                    scale = scale,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            pageData.code?.let { code ->
                CodeCard(
                    code = code,
                    note = pageData.codeNote,
                    scale = scale,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            pageData.practicePrompt?.let { prompt ->
                InfoCard(
                    label = "직접 해보기",
                    text = prompt,
                    accent = ReaderGold,
                    scale = scale,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            pageData.question?.let { question ->
                InfoCard(
                    label = "문제",
                    text = question,
                    accent = Color(0xFF93B7D7),
                    scale = scale,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            if (pageData.answer != null) {
                TextButton(
                    onClick = { showAnswer = !showAnswer },
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        if (showAnswer) "정답·해설 접기  ▲" else "정답·해설 보기  ▼",
                        color = ReaderGold,
                        fontFamily = EditorialSerif,
                        fontSize = (14 * scale).sp
                    )
                }

                if (showAnswer) {
                    InfoCard(
                        label = "정답·해설",
                        text = pageData.answer,
                        accent = Color(0xFF9BB58B),
                        scale = scale,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    pageData.mistake?.let { mistake ->
                        InfoCard(
                            label = "자주 틀리는 이유",
                            text = mistake,
                            accent = Color(0xFFD19B76),
                            scale = scale,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }

        if (!focusMode) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.08f)))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
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
