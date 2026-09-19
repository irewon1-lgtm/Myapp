package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun SystemBars(dark:Boolean){
    val view=LocalView.current
    SideEffect{
        val window=(view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowInsetsControllerCompat(window,view).apply{
            isAppearanceLightStatusBars=!dark
            isAppearanceLightNavigationBars=!dark
        }
    }
}

@Composable
fun ProgressBar(progress:Float, modifier:Modifier=Modifier, dark:Boolean=false){
    val track=if(dark) Color.White.copy(.12f) else Color(0xFFE7E0D4)
    val fill=if(dark) ReaderGold else Gold
    Box(modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)).background(track)){
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f,1f)).height(7.dp).background(fill))
    }
}

@Composable
fun BottomNav(
    selected:String,
    onHome:()->Unit,
    onLibrary:()->Unit,
    onSaved:()->Unit,
    onSettings:()->Unit
){
    Column(
        Modifier
            .fillMaxWidth()
            .background(IvoryCard)
            .navigationBarsPadding()
    ){
        NavigationBar(
            containerColor=IvoryCard,
            tonalElevation=0.dp,
            windowInsets=WindowInsets(0,0,0,0),
            modifier=Modifier.height(72.dp)
        ){
            val colors=NavigationBarItemDefaults.colors(
                selectedIconColor=Gold,
                selectedTextColor=Gold,
                indicatorColor=Color.Transparent,
                unselectedIconColor=Muted,
                unselectedTextColor=Muted
            )
            NavigationBarItem(selected=selected=="home",onClick=onHome,icon={Icon(Icons.Rounded.Home,null)},label={Text("홈",fontSize=11.sp)},colors=colors)
            NavigationBarItem(selected=selected=="library",onClick=onLibrary,icon={Icon(Icons.Rounded.LibraryBooks,null)},label={Text("서재",fontSize=11.sp)},colors=colors)
            NavigationBarItem(selected=selected=="saved",onClick=onSaved,icon={Icon(Icons.Rounded.BookmarkBorder,null)},label={Text("북마크",fontSize=11.sp)},colors=colors)
            NavigationBarItem(selected=selected=="settings",onClick=onSettings,icon={Icon(Icons.Rounded.Settings,null)},label={Text("설정",fontSize=11.sp)},colors=colors)
        }
    }
}
