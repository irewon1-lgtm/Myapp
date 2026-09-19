package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ReaderPrefs
import com.codingroadmap.app.data.TrackMeta

@Composable
fun ReaderShellScreen(
 track:TrackMeta,chapter:Int,prefs:ReaderPrefs,onBack:()->Unit,onChapter:(Int)->Unit,onVisit:(Int)->Unit,
 onBookmark:(Int)->Unit,onSaveNote:(Int,String)->Unit,onTextScale:(Float)->Unit
){
 SystemBars(true)
 val title=track.chapters[chapter]
 var showDisplay by remember{mutableStateOf(false)}
 var showNote by remember{mutableStateOf(false)}
 var note by remember(chapter,prefs.notes[chapter]){mutableStateOf(prefs.notes[chapter].orEmpty())}
 var drag by remember(chapter){mutableFloatStateOf(0f)}
 LaunchedEffect(chapter){onVisit(chapter)}
 Column(Modifier.fillMaxSize().background(ReaderBg).windowInsetsPadding(WindowInsets.safeDrawing)){
  Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){
   IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"목차",tint=ReaderText)}
   Text("Chapter ${chapter+1} · $title",fontFamily=EditorialSerif,color=ReaderText,fontSize=18.sp,modifier=Modifier.weight(1f),maxLines=1)
   IconButton(onClick={onBookmark(chapter)}){Icon(if(chapter in prefs.bookmarks)Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,"북마크",tint=if(chapter in prefs.bookmarks)ReaderGold else ReaderText)}
   IconButton(onClick={showNote=true}){Icon(Icons.Rounded.EditNote,"메모",tint=if(prefs.notes[chapter].isNullOrBlank())ReaderText else ReaderGold)}
   TextButton(onClick={showDisplay=true}){Text("Aa",fontFamily=EditorialSerif,color=ReaderText,fontSize=18.sp)}
  }
  ReaderBody(title,chapter,track.chapters.size,prefs.textScale,
   modifier=Modifier.weight(1f).pointerInput(chapter){
    detectHorizontalDragGestures(onDragStart={drag=0f},onDragEnd={
     if(drag>120f&&chapter>0)onChapter(chapter-1)
     if(drag< -120f&&chapter<track.chapters.lastIndex)onChapter(chapter+1)
     drag=0f
    },onHorizontalDrag={_,amount->drag+=amount})
   },
   onPrev={if(chapter>0)onChapter(chapter-1)},onNext={if(chapter<track.chapters.lastIndex)onChapter(chapter+1)}
  )
 }
 ReaderDialogs(showDisplay,showNote,prefs.textScale,note,{note=it},{showDisplay=false},{showNote=false},onTextScale,{onSaveNote(chapter,note);showNote=false})
}
