package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderBody(
    trackId: Int,
    title: String,
    chapter: Int,
    count: Int,
    scale: Float,
    canPrev: Boolean,
    canNext: Boolean,
    focusMode: Boolean,
    onToggleFocus: () -> Unit,
    modifier: Modifier = Modifier,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val mainTitle = if (trackId == 1 && chapter == 3) "타입은 값의 종류입니다" else title

    Column(modifier) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 27.dp, vertical = 18.dp)
        ) {
            Text("코딩의 기초", fontFamily = EditorialSerif, color = ReaderGold, fontSize = (13 * scale).sp)
            Text(
                mainTitle,
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (34 * scale).sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (43 * scale).sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text("—", fontFamily = EditorialSerif, color = ReaderGold, fontSize = (22 * scale).sp, modifier = Modifier.padding(top = 4.dp))
            Text(
                "데이터에는 저마다의 형태가 있고,\n파이썬은 그 형태를 ‘타입’으로 구분합니다.",
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (17 * scale).sp,
                lineHeight = (28 * scale).sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                "우리가 사용하는 모든 값은 타입을 가집니다.\n타입은 값이 어떤 종류의 데이터인지 알려주는 이름표와 같습니다. 같은 37이라도, 숫자 37과 문자열 “37”은 전혀 다른 타입입니다.",
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (16 * scale).sp,
                lineHeight = (28 * scale).sp,
                modifier = Modifier.padding(top = 22.dp)
            )
            Text(
                "파이썬에서 자주 사용하는 기본 타입은 다음과 같습니다.",
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (16 * scale).sp,
                lineHeight = (27 * scale).sp,
                modifier = Modifier.padding(top = 20.dp)
            )
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                TypeRow("int", "정수 (예: 1, 0, -5)", scale)
                TypeRow("float", "실수 (예: 3.14, -0.5)", scale)
                TypeRow("str", "문자열 (예: “안녕하세요”)", scale)
                TypeRow("bool", "참과 거짓 (예: True, False)", scale)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 22.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF30291F))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LeafMark(Modifier.size(50.dp), ReaderGold)
                Box(Modifier.padding(horizontal = 16.dp).width(1.dp).height(72.dp).background(Color(0xFF6D5B40)))
                Column(Modifier.weight(1f)) {
                    Text("아주 쉽게", fontFamily = EditorialSerif, fontSize = (16 * scale).sp, color = ReaderGold, fontWeight = FontWeight.SemiBold)
                    Text(
                        "타입은 값의 ‘종류’를 말합니다.\n숫자, 문자, 참/거짓처럼 데이터가 어떤 형태인지 구분하는 기준이에요.",
                        fontFamily = EditorialSerif,
                        fontSize = (14 * scale).sp,
                        lineHeight = (22 * scale).sp,
                        color = ReaderText,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Text(
                "아래 예제를 실행해 보면, 서로 다른 타입의 값을 변수에 저장하는 것을 확인할 수 있습니다.",
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (15 * scale).sp,
                lineHeight = (25 * scale).sp,
                modifier = Modifier.padding(top = 20.dp)
            )
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF191817))
            ) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF24211D)).padding(horizontal = 15.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Python", fontFamily = EditorialSerif, color = ReaderMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ContentCopy, null, tint = ReaderText, modifier = Modifier.size(17.dp))
                    Text("  복사하기", color = ReaderText, fontSize = 11.sp)
                }
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CodeLine("1", "age = ", "37", Color(0xFF76A9E7))
                    CodeLine("2", "name = ", "“승원”", Color(0xFFE4B94C))
                    CodeLine("3", "is_ready = ", "True", Color(0xFFC58AEF))
                }
            }
            Text(
                "각각의 변수는 서로 다른 타입의 값을 가지고 있습니다.",
                fontFamily = EditorialSerif,
                color = ReaderText,
                fontSize = (14 * scale).sp,
                modifier = Modifier.padding(top = 15.dp, bottom = 25.dp)
            )
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

                Column(Modifier.weight(1f).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${chapter + 1} / $count", fontFamily = EditorialSerif, color = ReaderMuted, fontSize = 12.sp)
                    ProgressBar((chapter + 1) / count.toFloat(), Modifier.padding(top = 8.dp), dark = true)
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
                    modifier = Modifier.padding(start = 7.dp).clickable(onClick = onToggleFocus)
                ) {
                    Icon(Icons.Rounded.MenuBook, null, tint = ReaderGold, modifier = Modifier.size(25.dp))
                    Text("집중모드", fontFamily = EditorialSerif, color = ReaderMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun TypeRow(type:String,desc:String,scale:Float){
    Row(verticalAlignment=Alignment.CenterVertically){
        Text("•",color=ReaderGold,fontSize=(17*scale).sp,modifier=Modifier.width(22.dp))
        Text(type,fontFamily=FontFamily.Monospace,color=ReaderText,fontSize=(14*scale).sp,modifier=Modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFF292724)).padding(horizontal=9.dp,vertical=4.dp))
        Text(desc,fontFamily=EditorialSerif,color=ReaderText,fontSize=(14*scale).sp,modifier=Modifier.padding(start=12.dp))
    }
}

@Composable
private fun CodeLine(no:String,prefix:String,value:String,valueColor:Color){
    Row{
        Text(no,fontFamily=FontFamily.Monospace,color=ReaderMuted,fontSize=13.sp,modifier=Modifier.width(30.dp))
        Text(prefix,fontFamily=FontFamily.Monospace,color=ReaderText,fontSize=14.sp)
        Text(value,fontFamily=FontFamily.Monospace,color=valueColor,fontSize=14.sp)
    }
}
