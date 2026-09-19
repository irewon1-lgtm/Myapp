package com.codingroadmap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ReaderPrefs
import com.codingroadmap.app.data.TrackMeta

@Composable
fun LibraryScreen(
 track:TrackMeta,prefs:ReaderPrefs,onBack:()->Unit,onOpen:(Int)->Unit,
 onHome:()->Unit,onSaved:()->Unit,onSettings:()->Unit
){
 SystemBars(false)
 val demo=prefs.visitedChapters.isEmpty()
 val current=if(demo)3 else prefs.currentChapter
 val progress=if(demo).34f else prefs.visitedChapters.size/track.chapters.size.toFloat()
 Scaffold(containerColor=Ivory,bottomBar={BottomNav("library",onHome,{},onSaved,onSettings)}){pad->
  LazyColumn(
   Modifier.fillMaxSize().padding(pad),
   contentPadding=PaddingValues(horizontal=22.dp,vertical=14.dp),
   verticalArrangement=Arrangement.spacedBy(10.dp)
  ){
   item{
    Row(verticalAlignment=Alignment.CenterVertically){
     IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"뒤로",tint=Ink)}
     Text("TRACK 01 · ${track.title}",fontFamily=EditorialSerif,fontSize=25.sp,fontWeight=FontWeight.SemiBold,color=Ink)
    }
   }
   item{TrackSummary(track,progress){onOpen(current)}}
   item{
    Row(Modifier.fillMaxWidth().padding(top=10.dp,bottom=4.dp),verticalAlignment=Alignment.Bottom){
     Text("목차",fontFamily=EditorialSerif,fontSize=27.sp,fontWeight=FontWeight.SemiBold,color=Ink,modifier=Modifier.weight(1f))
     Text("작은 배움이 모여,\n큰 변화를 만듭니다.",fontFamily=EditorialSerif,fontSize=11.sp,lineHeight=15.sp,color=Muted)
    }
   }
   items(track.chapters.size){index->
    ChapterRow(index,track.chapters[index],visited=demo&&index<3 || index in prefs.visitedChapters,current=index==current,onClick={onOpen(index)})
   }
  }
 }
}
