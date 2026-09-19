package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ReaderPrefs
import com.codingroadmap.app.data.TrackMeta

@Composable
fun SavedScreen(track:TrackMeta,prefs:ReaderPrefs,onOpen:(Int)->Unit,onHome:()->Unit,onLibrary:()->Unit,onSettings:()->Unit){
 SystemBars(false)
 var filter by remember{mutableStateOf("전체")}
 val saved=(prefs.bookmarks+prefs.notes.keys).sorted()
 val demo=if(saved.isEmpty()) listOf(0,3,5,7) else saved
 Scaffold(containerColor=Ivory,bottomBar={BottomNav("saved",onHome,onLibrary,{},onSettings)}){pad->
  LazyColumn(Modifier.fillMaxSize().padding(pad),contentPadding=PaddingValues(horizontal=24.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
   item{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
     Column(Modifier.weight(1f)){Text("코딩 로드맵",fontFamily=EditorialSerif,fontSize=38.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text("오늘도, 더 나은 개발자가 되어볼까요?",fontFamily=EditorialSerif,fontSize=15.sp,color=Muted,modifier=Modifier.padding(top=4.dp))}
     Column(horizontalAlignment=Alignment.End){Icon(Icons.Rounded.NotificationsNone,null,tint=Ink,modifier=Modifier.size(25.dp));Text("좋은 코드는\n더 좋은 가능성을\n만듭니다.  —",fontFamily=EditorialSerif,fontSize=10.sp,lineHeight=15.sp,color=Muted,modifier=Modifier.padding(top=10.dp))}
    }
   }
   item{
    Row(verticalAlignment=Alignment.Bottom){Column(Modifier.weight(1f)){Text("북마크",fontFamily=EditorialSerif,fontSize=32.sp,fontWeight=FontWeight.SemiBold,color=Ink);Text("다시 보고 싶은 순간들을 모아두었어요.",fontFamily=EditorialSerif,fontSize=14.sp,color=Muted,modifier=Modifier.padding(top=5.dp))};Text("총 ${demo.size}개",fontFamily=EditorialSerif,fontSize=12.sp,color=Muted)}
   }
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){listOf("전체","북마크","하이라이트","메모").forEach{label->
     val active=filter==label
     Text(label,color=if(active)Color.White else Ink,fontSize=12.sp,modifier=Modifier.clip(RoundedCornerShape(50)).background(if(active)Gold else Color(0xFFF3ECE2)).clickable{filter=label}.padding(horizontal=18.dp,vertical=9.dp))
    }}
   }
   items(demo.size){i->
    val chapter=demo[i].coerceIn(0,track.chapters.lastIndex)
    val quotes=listOf("왜 우리는 코드를 배울까요?","int, float, str, bool","오류를 읽는 힘이 실력을 만든다.","작게 만들고 바로 실행해 보기.")
    val descriptions=listOf("코딩은 단순히 기술이 아니라,\n세상을 이해하고 바꾸는 새로운 언어입니다.","파이썬의 기본 자료형을 이해하면,\n데이터를 다루는 힘이 생깁니다.","에러 메시지는 실패가 아니라,\n더 나은 코드를 위한 힌트입니다.","완벽하게 하려고 하지 말고,\n일단 만들어서 실행해보자!")
    Row(Modifier.fillMaxWidth().shadow(1.dp,RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(IvoryCard).clickable{onOpen(chapter)}.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
     Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF4EAD8)),contentAlignment=Alignment.Center){Icon(if(i==3)Icons.Rounded.EditNote else Icons.Rounded.Bookmark,null,tint=Gold,modifier=Modifier.size(20.dp))}
     Column(Modifier.padding(start=13.dp).weight(1f)){
      Row{Text("Chapter ${chapter+1} · ${track.chapters[chapter]}",fontFamily=EditorialSerif,fontSize=15.sp,color=Ink,modifier=Modifier.weight(1f));Text("p.${listOf(12,41,63,88)[i.coerceAtMost(3)]}",fontFamily=EditorialSerif,fontSize=11.sp,color=Muted)}
      Text("“${quotes[i.coerceAtMost(3)]}”",fontFamily=EditorialSerif,fontSize=18.sp,lineHeight=23.sp,color=Ink,modifier=Modifier.padding(top=7.dp))
      Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5DED3)).padding(top=7.dp))
      Text(descriptions[i.coerceAtMost(3)],fontFamily=EditorialSerif,fontSize=12.sp,lineHeight=18.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
     }
     Box(Modifier.padding(start=12.dp).size(88.dp).clip(RoundedCornerShape(14.dp)).background(if(i%2==0)Color(0xFFF0E5D6) else Color(0xFFE4D4BF)),contentAlignment=Alignment.Center){
      if(i==0) TrackCoverArt(1,Modifier.fillMaxSize())
      else if(i==1) HeroStillLife(Modifier.fillMaxSize())
      else LeafMark(Modifier.size(48.dp),if(i==2)Gold else Forest)
     }
    }
   }
  }
 }
}
