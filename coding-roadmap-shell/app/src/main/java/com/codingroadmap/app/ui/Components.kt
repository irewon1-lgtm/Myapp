package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, dark: Boolean = false) {
    val track = if (dark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .08f)
    val fill = if (dark) ReaderGold else Gold
    Box(modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(7.dp).background(fill))
    }
}

@Composable
fun BottomNav(
    selected: String,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onSaved: () -> Unit,
    onSettings: () -> Unit
) {
    NavigationBar(containerColor = IvoryCard) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Gold,
            selectedTextColor = Gold,
            indicatorColor = GoldSoft.copy(alpha = .5f),
            unselectedIconColor = Muted,
            unselectedTextColor = Muted
        )
        NavigationBarItem(selected == "home", onHome, { Icon(Icons.Rounded.Home, null) }, label = { Text("홈") }, colors = colors)
        NavigationBarItem(selected == "library", onLibrary, { Icon(Icons.Rounded.LibraryBooks, null) }, label = { Text("서재") }, colors = colors)
        NavigationBarItem(selected == "saved", onSaved, { Icon(Icons.Rounded.Bookmark, null) }, label = { Text("북마크") }, colors = colors)
        NavigationBarItem(selected == "settings", onSettings, { Icon(Icons.Rounded.Settings, null) }, label = { Text("설정") }, colors = colors)
    }
}
