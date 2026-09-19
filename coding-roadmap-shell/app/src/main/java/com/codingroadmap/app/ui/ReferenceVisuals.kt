package com.codingroadmap.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val EditorialSerif = FontFamily.Serif
val CleanSans = FontFamily.SansSerif

@Composable
fun HeroStillLife(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF3E7D6), Color(0xFFE5D3BA), Color(0xFFC7AC8A))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // soft sunlight
            drawPath(
                Path().apply {
                    moveTo(0f, h * .08f)
                    lineTo(w * .70f, 0f)
                    lineTo(w * .38f, h * .70f)
                    lineTo(0f, h * .85f)
                    close()
                },
                color = Color.White.copy(alpha = .28f)
            )
            drawCircle(Color.White.copy(alpha = .20f), w * .35f, Offset(w * .18f, h * .22f))

            // blurred plant stem
            drawLine(
                color = Color(0xFF657652).copy(alpha = .72f),
                start = Offset(w * .55f, h * .56f),
                end = Offset(w * .56f, h * .20f),
                strokeWidth = w * .012f,
                cap = StrokeCap.Round
            )
            val leaf = Color(0xFF55704C).copy(alpha = .75f)
            listOf(
                Triple(.50f,.28f,-28f), Triple(.60f,.32f,32f),
                Triple(.48f,.38f,-34f), Triple(.62f,.43f,38f),
                Triple(.51f,.49f,-20f)
            ).forEach { (x,y,r) ->
                withTransform({
                    rotate(r, Offset(w*x, h*y))
                }) {
                    drawOval(
                        leaf,
                        topLeft = Offset(w*x-w*.055f, h*y-h*.025f),
                        size = Size(w*.11f, h*.05f)
                    )
                }
            }

            // stacked books
            drawRoundRect(
                Color(0xFFCDB99B),
                topLeft = Offset(w*.35f, h*.64f),
                size = Size(w*.42f, h*.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w*.02f)
            )
            drawRoundRect(
                Color(0xFFDED0BA),
                topLeft = Offset(w*.30f, h*.75f),
                size = Size(w*.47f, h*.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w*.02f)
            )
            drawLine(
                Color(0xFFA58B6C).copy(alpha=.65f),
                Offset(w*.34f,h*.82f), Offset(w*.72f,h*.82f), w*.004f
            )

            // mug
            drawRoundRect(
                Color(0xFFE7D7C1),
                topLeft = Offset(w*.70f, h*.52f),
                size = Size(w*.22f, h*.34f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w*.04f)
            )
            drawArc(
                color = Color(0xFFCEB99D),
                startAngle = -70f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w*.87f, h*.60f),
                size = Size(w*.12f, h*.14f),
                style = Stroke(w*.018f)
            )
        }

        Column(
            Modifier.align(Alignment.TopEnd).padding(top = 25.dp, end = 18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Better", fontFamily = EditorialSerif, color = Color(0xFF594B3A), fontSize = 11.sp)
            Text("Learners", fontFamily = EditorialSerif, color = Color(0xFF594B3A), fontSize = 11.sp)
            Text("Brighter", fontFamily = EditorialSerif, color = Color(0xFF594B3A), fontSize = 11.sp)
            Text("Tomorrows.", fontFamily = EditorialSerif, color = Color(0xFF594B3A), fontSize = 11.sp)
            Text("—", color = Color(0xFF7A6853), fontSize = 11.sp)
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Good", fontFamily = EditorialSerif, color = Color(0xFF5B4D3D), fontSize = 10.sp)
            Text("Code", fontFamily = EditorialSerif, color = Color(0xFF5B4D3D), fontSize = 10.sp)
            Text("A Kinder", fontFamily = EditorialSerif, color = Color(0xFF5B4D3D), fontSize = 10.sp)
            Text("Tomorrow", fontFamily = EditorialSerif, color = Color(0xFF5B4D3D), fontSize = 10.sp)
        }
    }
}

@Composable
fun TrackCoverArt(trackId: Int, modifier: Modifier = Modifier) {
    val bg = when(trackId) {
        1 -> Color(0xFFF0E7D8)
        2 -> Color(0xFF29483B)
        3 -> Color(0xFFD8C9B4)
        else -> Color(0xFFD9D8D4)
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width
            val h=size.height
            when(trackId) {
                1 -> {
                    val sky = Brush.verticalGradient(listOf(Color(0xFFB9D2E4), Color(0xFFF0CDA5)))
                    drawRect(sky, topLeft=Offset(w*.12f,h*.49f), size=Size(w*.76f,h*.20f))
                    val mountain = Path().apply {
                        moveTo(w*.12f,h*.66f); lineTo(w*.32f,h*.58f); lineTo(w*.48f,h*.64f)
                        lineTo(w*.66f,h*.53f); lineTo(w*.88f,h*.62f); lineTo(w*.88f,h*.69f)
                        lineTo(w*.12f,h*.69f); close()
                    }
                    drawPath(mountain, Color(0xFF6E725E))
                    drawCircle(Color(0xFFF3D083), w*.035f, Offset(w*.77f,h*.56f))
                }
                2 -> {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF244A3B), Color(0xFF16352C))),
                        topLeft=Offset(w*.12f,h*.49f), size=Size(w*.76f,h*.20f))
                    val road = Path().apply {
                        moveTo(w*.56f,h*.69f)
                        cubicTo(w*.54f,h*.63f,w*.45f,h*.60f,w*.52f,h*.55f)
                        cubicTo(w*.59f,h*.51f,w*.50f,h*.50f,w*.48f,h*.49f)
                        lineTo(w*.58f,h*.49f)
                        cubicTo(w*.66f,h*.54f,w*.59f,h*.58f,w*.58f,h*.62f)
                        cubicTo(w*.57f,h*.65f,w*.63f,h*.67f,w*.63f,h*.69f)
                        close()
                    }
                    drawPath(road, Color(0xFFD8CBAA))
                }
                3 -> {
                    drawRect(Color(0xFFBFD4E0), topLeft=Offset(w*.12f,h*.49f), size=Size(w*.76f,h*.20f))
                    val stone = Color(0xFFB99F80)
                    repeat(3) { i ->
                        val x=w*(.22f + i*.23f)
                        drawRoundRect(
                            color=stone,
                            topLeft=Offset(x,h*.50f),
                            size=Size(w*.15f,h*.19f),
                            cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.07f)
                        )
                        drawRoundRect(
                            color=Color(0xFFBFD4E0),
                            topLeft=Offset(x+w*.025f,h*.535f),
                            size=Size(w*.10f,h*.155f),
                            cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.05f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeafMark(modifier: Modifier = Modifier, color: Color = GoldSoft) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        drawLine(color, Offset(w*.48f,h*.88f), Offset(w*.52f,h*.12f), w*.06f, StrokeCap.Round)
        listOf(
            Triple(.43f,.28f,-35f), Triple(.58f,.42f,35f), Triple(.42f,.56f,-30f),
            Triple(.57f,.69f,28f)
        ).forEach { (x,y,r) ->
            withTransform({ rotate(r, Offset(w*x,h*y)) }) {
                drawOval(color, Offset(w*x-w*.12f,h*y-h*.06f), Size(w*.24f,h*.12f))
            }
        }
    }
}
