package com.futuretech.poweruser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.V1TextbookCatalog
import kotlinx.coroutines.launch

/**
 * Focused e-book shell. The reader consumes the whole screen and the TRACK table of contents is
 * opened only from the compact top-bar button, so it never competes with page-turn tap zones.
 */
@Composable
fun AdaptiveTextbookScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    initialChapterId: String = "V1-C01"
) {
    val context = LocalContext.current
    val progressStore = remember { TextbookProgressStore(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedChapterId by rememberSaveable(initialChapterId) { mutableStateOf(initialChapterId) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        modifier = Modifier.testTag("reader_toc_drawer_shell"),
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 360.dp)
                        .padding(vertical = 18.dp)
                ) {
                    Text(
                        text = "책 목차",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TRACK을 고르면 해당 책의 첫 화면으로 이동합니다.",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    V1TextbookCatalog.chapters.forEach { track ->
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(
                                        text = "TRACK ${track.number.toString().padStart(2, '0')}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = track.title,
                                        maxLines = 2,
                                        fontWeight = if (track.id == selectedChapterId) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            },
                            selected = track.id == selectedChapterId,
                            onClick = {
                                progressStore.saveSelectedChapter(track.id)
                                selectedChapterId = track.id
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
    ) {
        key(selectedChapterId) {
            V1TextbookScreen(
                practiceCompletedIds = practiceCompletedIds,
                onNavigateBack = onNavigateBack,
                onStartPractice = onStartPractice,
                initialChapterId = selectedChapterId,
                onOpenToc = { scope.launch { drawerState.open() } }
            )
        }
    }
}
