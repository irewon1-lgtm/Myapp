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
 val title=track.chapters.getOrNull(chapter)?:"타입"
 Box(Modifier.fillMaxWidth().height(286.dp).shadow(3.dp,RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).clickable(onClick=onClick)){
  Row(Modifier.fillMaxSize().background(Color(0xFFF7EEDF))){
   Column(Modifier.weight(1.06f).fillMaxHeight().padding(22.dp), verticalArrangement=Arrangement.Center){
    Text("이어 학습하기",fontFamily=EditorialSerif,fontSize=29.sp,fontWeight=FontWeight.SemiBold,color=Ink)
    Text("지금의 한 걸음이,\n더 넓은 세상을 만듭니다.",fontFamily=EditorialSerif,fontSize=15.sp,lineHeight=22.sp,color=Muted,modifier=Modifier.padding(top=10.dp))
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD9CCB8)))
    Text("TRACK 01 · 코딩의 시작",fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=12.dp))
    Text("Chapter ${chapter+1} · $title",fontFamily=EditorialSerif,fontSize=21.sp,color=Ink,modifier=Modifier.padding(top=3.dp))
    Row(Modifier.padding(top=14.dp),verticalAlignment=Alignment.CenterVertically){
      ProgressBar(progress,Modifier.weight(1f))
      Text("${(progress*100).toInt()}%",fontSize=13.sp,color=Ink,modifier=Modifier.padding(start=10.dp))
    }
    Box(Modifier.padding(top=15.dp).clip(RoundedCornerShape(50)).background(Gold).padding(horizontal=22.dp,vertical=11.dp)){
      Text("이어보기  →",color=Color.White,fontSize=16.sp)
    }
   }
   HeroStillLife(Modifier.weight(.94f).fillMaxHeight())
  }
 }
}

@Composable
fun BookCard(track:TrackMeta, progress:Float, onClick:()->Unit){
 val bg=when(track.id){1->Color(0xFFF3EADF);2->Forest;3->Color(0xFFDCCDBA);else->Color(0xFFD7DCE0)}
 val fg=if(track.id==2) Color(0xFFF3EEE7) else Ink
 Column(Modifier.width(152.dp).clickable(enabled=track.available,onClick=onClick)){
  Box(Modifier.fillMaxWidth().height(214.dp).shadow(5.dp,RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(bg).padding(14.dp)){
   Column{
    Text("TRACK ${track.id.toString().padStart(2,'0')}",fontSize=9.sp,color=fg.copy(.75f))
    Text(track.title,fontFamily=EditorialSerif,fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=fg,modifier=Modifier.padding(top=8.dp))
    Text(track.subtitle,fontSize=10.sp,lineHeight=15.sp,color=fg.copy(.72f),modifier=Modifier.padding(top=7.dp))
    Spacer(Modifier.height(10.dp))
    TrackCoverArt(track.id,Modifier.fillMaxWidth().height(66.dp))
    Spacer(Modifier.weight(1f))
    Text(when(track.id){1->"A NEW\nJOURNEY BEGINS";2->"SIMPLE\nPOWERFUL\nTOGETHER";3->"DATA\nOPENS\nA WIDER WORLD";else->"NEXT\nEDITION"},fontFamily=EditorialSerif,fontSize=8.sp,lineHeight=10.sp,color=fg.copy(.65f))
   }
  }
  Text("TRACK ${track.id.toString().padStart(2,'0')}",fontSize=10.sp,color=Muted,modifier=Modifier.padding(top=9.dp))
  Text(track.title,fontFamily=EditorialSerif,fontSize=16.sp,color=Ink)
  Row(Modifier.padding(top=7.dp),verticalAlignment=Alignment.CenterVertically){
    ProgressBar(progress,Modifier.weight(1f))
    Text(if(track.available)"${(progress*100).toInt()}%" else "시작 전",fontSize=11.sp,color=Muted,modifier=Modifier.padding(start=8.dp))
  }
 }
}
