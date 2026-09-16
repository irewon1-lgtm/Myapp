package com.futuretech.poweruser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.V1TextbookCatalog
import kotlinx.coroutines.launch

/** Ebook shell: the actual reading surface is a fixed page, while the book table of contents stays hidden. */
@Composable
fun AdaptiveTextbookScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    initialChapterId: String? = null
) {
    val context = LocalContext.current
    val progressStore = remember { TextbookProgressStore(context) }
    val restoredChapterId = initialChapterId?.takeIf { V1TextbookCatalog.chapterById(it) != null }
        ?: progressStore.selectedChapterId()?.takeIf { V1TextbookCatalog.chapterById(it) != null }
        ?: "V1-C01"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedChapterId by rememberSaveable { mutableStateOf(restoredChapterId) }

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
                        text = "책 차례",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "읽는 화면은 한 페이지씩 넘기고, 여기서는 원하는 TRACK으로 바로 이동합니다.",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    V1TextbookCatalog.chapters.forEach { track ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = "${track.number}장 · ${track.title}",
                                    maxLines = 2
                                )
                            },
                            selected = track.id == selectedChapterId,
                            onClick = {
                                selectedChapterId = track.id
                                progressStore.saveSelectedChapter(track.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().testTag("reader_single_page_shell")
        ) {
            key(selectedChapterId) {
                EbookTextbookScreen(
                    practiceCompletedIds = practiceCompletedIds,
                    onNavigateBack = onNavigateBack,
                    onStartPractice = onStartPractice,
                    initialChapterId = selectedChapterId,
                    onChapterChanged = { selectedChapterId = it }
                )
            }

            FloatingActionButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 72.dp)
                    .testTag("reader_toc_button"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("목차", fontWeight = FontWeight.Bold)
            }
        }
    }
}
