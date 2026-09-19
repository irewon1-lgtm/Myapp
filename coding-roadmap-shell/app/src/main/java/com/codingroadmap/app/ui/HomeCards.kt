package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.TrackMeta

@Composable
fun ContinueCard(track:TrackMeta, chapter:Int, progress:Float, onClick:()->Unit){
    val title=track.chapters.getOrNull(chapter)?:"학습 시작"
    Box(
        Modifier.fillMaxWidth().height(286.dp)
            .shadow(3.dp,RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick=onClick)
    ){
        Row(Modifier.fillMaxSize().background(Color(0xFFF7EEDF))){
            Column(
                Modifier.weight(1.06f).fillMaxHeight().padding(22.dp),
                verticalArrangement=Arrangement.Center
            ){
                Text("이어 학습하기",fontFamily=EditorialSerif,fontSize=29.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                Text(
                    "지금의 한 걸음이,\n더 넓은 세상을 만듭니다.",
                    fontFamily=EditorialSerif,
                    fontSize=15.sp,
                    lineHeight=22.sp,
                    color=Muted,
                    modifier=Modifier.padding(top=10.dp)
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD9CCB8)))
                Text(
                    "TRACK ${track.id.toString().padStart(2,'0')} · ${track.title}",
                    fontSize=11.sp,
                    color=Muted,
                    modifier=Modifier.padding(top=12.dp)
                )
                Text(
                    "Chapter ${chapter+1} · $title",
                    fontFamily=EditorialSerif,
                    fontSize=21.sp,
                    color=Ink,
                    modifier=Modifier.padding(top=3.dp)
                )
                Row(Modifier.padding(top=14.dp),verticalAlignment=Alignment.CenterVertically){
                    ProgressBar(progress,Modifier.weight(1f))
                    Text("${(progress*100).toInt()}%",fontSize=13.sp,color=Ink,modifier=Modifier.padding(start=10.dp))
                }
                Box(
                    Modifier.padding(top=15.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Gold)
                        .padding(horizontal=22.dp,vertical=11.dp)
                ){
                    Text("이어보기  →",color=Color.White,fontSize=16.sp)
                }
            }
            HeroStillLife(Modifier.weight(.94f).fillMaxHeight())
        }
    }
}

@Composable
fun BookCard(track:TrackMeta, progress:Float, onClick:()->Unit){
    val palette=listOf(
        Color(0xFFF3EADF),
        Color(0xFF29483B),
        Color(0xFFDCCDBA),
        Color(0xFFD9E1E6),
        Color(0xFFDDD2E7),
        Color(0xFFE6D8C6),
        Color(0xFFD7E3D8),
        Color(0xFFE3D6D2),
        Color(0xFFD5DCE8),
        Color(0xFFE5D8C9),
        Color(0xFFD9D9D2)
    )
    val bg=palette[(track.id-1).coerceIn(0,palette.lastIndex)]
    val darkCover=track.id==2
    val fg=if(darkCover) Color(0xFFF3EEE7) else Ink
    val titleSize=when{
        track.title.length>=11 -> 14.sp
        track.title.length>=8 -> 15.sp
        else -> 18.sp
    }

    Column(
        Modifier.width(152.dp)
            .clickable(enabled=track.available,onClick=onClick)
    ){
        Box(
            Modifier.fillMaxWidth().height(214.dp)
                .shadow(5.dp,RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .padding(14.dp)
        ){
            Column{
                Text(
                    "TRACK ${track.id.toString().padStart(2,'0')}",
                    fontSize=9.sp,
                    color=fg.copy(.75f)
                )
                Text(
                    track.title,
                    fontFamily=EditorialSerif,
                    fontSize=titleSize,
                    lineHeight=(titleSize.value+3).sp,
                    fontWeight=FontWeight.SemiBold,
                    color=fg,
                    modifier=Modifier.padding(top=8.dp),
                    maxLines=2
                )
                Text(
                    track.subtitle,
                    fontSize=9.sp,
                    lineHeight=13.sp,
                    color=fg.copy(.72f),
                    modifier=Modifier.padding(top=7.dp),
                    maxLines=3
                )
                Spacer(Modifier.height(8.dp))
                TrackCoverArt(track.id,Modifier.fillMaxWidth().height(58.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    when(track.id){
                        1 -> "A NEW\nJOURNEY BEGINS"
                        2 -> "SIMPLE\nPOWERFUL\nTOGETHER"
                        3 -> "THINK\nBEFORE CODE"
                        4 -> "WEB\nCONNECTED"
                        5 -> "TYPE\nBUILD\nMOVE"
                        6 -> "DATA\nQUERY\nSTORE"
                        7 -> "API\nSERVER\nBACKEND"
                        8 -> "BUILD\nON GALAXY"
                        9 -> "SAFE\nCHANGE\nTEST"
                        10 -> "AI\nIN YOUR APP"
                        else -> "SYSTEM\nDEEP DIVE"
                    },
                    fontFamily=EditorialSerif,
                    fontSize=8.sp,
                    lineHeight=10.sp,
                    color=fg.copy(.65f)
                )
            }
        }

        Text(
            "TRACK ${track.id.toString().padStart(2,'0')}",
            fontSize=10.sp,
            color=Muted,
            modifier=Modifier.padding(top=9.dp)
        )
        Text(
            track.title,
            fontFamily=EditorialSerif,
            fontSize=15.sp,
            color=Ink,
            maxLines=1
        )
        Row(Modifier.padding(top=7.dp),verticalAlignment=Alignment.CenterVertically){
            ProgressBar(progress,Modifier.weight(1f))
            Text(
                if(track.available)"${(progress*100).toInt()}%" else "준비 중",
                fontSize=11.sp,
                color=Muted,
                modifier=Modifier.padding(start=8.dp)
            )
        }
    }
}
