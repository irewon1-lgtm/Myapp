package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
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
fun TrackSummary(track:TrackMeta,progress:Float,onContinue:()->Unit){
 Row(Modifier.fillMaxWidth().shadow(2.dp,RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF7EEDF)).padding(16.dp),verticalAlignment=Alignment.CenterVertically){
  Box(Modifier.width(112.dp).height(160.dp).shadow(4.dp,RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF2E9DB)).padding(12.dp)){
   Column{
    Text("TRACK 01",fontSize=9.sp,color=Muted)
    Text(track.title,fontFamily=EditorialSerif,fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=Ink,modifier=Modifier.padding(top=8.dp))
    Text("코딩하는\n새로운 나를 만나는 시간",fontSize=9.sp,lineHeight=13.sp,color=Muted,modifier=Modifier.padding(top=6.dp))
    TrackCoverArt(1,Modifier.fillMaxWidth().height(52.dp).padding(top=8.dp))
    Spacer(Modifier.weight(1f))
    Text("A NEW\nJOURNEY BEGINS",fontFamily=EditorialSerif,fontSize=7.sp,lineHeight=9.sp,color=Muted)
   }
  }
  Column(Modifier.padding(start=16.dp).weight(1f)){
   Text("지금의 한 걸음이,\n더 넓은 세상을 만듭니다.",fontFamily=EditorialSerif,fontSize=18.sp,lineHeight=24.sp,color=Ink)
   Text("코딩을 처음 시작하는 당신을 위해,\n기초부터 차근차근 안내합니다.",fontFamily=EditorialSerif,fontSize=12.sp,lineHeight=18.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
   Row(Modifier.padding(top=12.dp),verticalAlignment=Alignment.CenterVertically){
    ProgressBar(progress,Modifier.weight(1f))
    Text("${(progress*100).toInt()}%",fontSize=12.sp,color=Muted,modifier=Modifier.padding(start=8.dp))
   }
   Row(Modifier.padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Rounded.MenuBook,null,tint=Muted,modifier=Modifier.size(16.dp));Text("8개 챕터",fontSize=10.sp,color=Muted)
    Icon(Icons.Rounded.Schedule,null,tint=Muted,modifier=Modifier.size(16.dp));Text("약 2시간 30분",fontSize=10.sp,color=Muted)
   }
   Text("이어보기  →",color=Color.White,fontSize=14.sp,modifier=Modifier.padding(top=11.dp).clip(RoundedCornerShape(50)).background(Gold).clickable(onClick=onContinue).padding(horizontal=18.dp,vertical=9.dp))
  }
 }
}

@Composable
fun ChapterRow(index:Int,title:String,visited:Boolean,current:Boolean,onClick:()->Unit){
 val bg=if(current)Color(0xFFF1E2C9) else IvoryCard
 val minutes=listOf(12,14,16,18,20,22,18,28)[index]
 val videos=listOf(9,9,9,9,8,10,9,12)[index]
 Row(Modifier.fillMaxWidth().height(74.dp).shadow(1.dp,RoundedCornerShape(17.dp)).clip(RoundedCornerShape(17.dp)).background(bg).clickable(onClick=onClick).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
  Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFF2E5D2)),contentAlignment=Alignment.Center){Text("${index+1}",fontFamily=EditorialSerif,fontSize=18.sp,color=Ink)}
  Column(Modifier.padding(start=12.dp).weight(1f)){
   Text("Chapter ${index+1} · $title",fontFamily=EditorialSerif,fontSize=16.sp,color=Ink,maxLines=1)
   Row(Modifier.padding(top=4.dp),verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Rounded.MenuBook,null,tint=Muted,modifier=Modifier.size(14.dp));Text(" $minutes분  |  ",fontSize=10.sp,color=Muted)
    Icon(Icons.Rounded.Schedule,null,tint=Muted,modifier=Modifier.size(14.dp));Text(" $videos개 영상",fontSize=10.sp,color=Muted)
   }
  }
  if(index<3){
   Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE8ECE0)).padding(horizontal=9.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Rounded.CheckCircle,null,tint=Color(0xFF718466),modifier=Modifier.size(15.dp));Text(" 완료",fontSize=11.sp,color=Color(0xFF607356))
   }
  } else if(current || index==3){
   Text("⌁  읽는 중",color=Gold,fontSize=11.sp,modifier=Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(.72f)).padding(horizontal=9.dp,vertical=5.dp))
  } else if(index==4){
   Text("다음",color=Muted,fontSize=11.sp,modifier=Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF0ECE5)).padding(horizontal=10.dp,vertical=5.dp))
  }
  Spacer(Modifier.width(4.dp));Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight,null,tint=Muted)
 }
}
