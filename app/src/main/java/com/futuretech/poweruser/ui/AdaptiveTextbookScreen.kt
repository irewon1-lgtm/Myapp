package com.futuretech.poweruser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.futuretech.poweruser.textbook.V5BookAssetRepository
import kotlinx.coroutines.launch

/**
 * Focused e-book shell.
 *
 * A TRACK with a validated V5 manifest is routed to the part-lazy, measured V5 reader. Tracks which
 * have not yet been rewritten keep using the verified V4/V3 compatibility reader. Remote V5 content
 * is refreshed when this screen opens so a newly published TRACK can appear without rebuilding the
 * APK again after the hybrid reader itself has been installed once.
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
    val v5Repository = remember { V5BookAssetRepository(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedChapterId by rememberSaveable(initialChapterId) { mutableStateOf(initialChapterId) }
    var remoteGeneration by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(v5Repository) {
        runCatching { v5Repository.refreshRemoteContent() }
        remoteGeneration += 1
    }

    val selectedChapter = V1TextbookCatalog.chapterById(selectedChapterId)
        ?: V1TextbookCatalog.chapters.first()
    val hasV5Book = remember(selectedChapter.id, remoteGeneration) {
        runCatching { v5Repository.loadManifest(selectedChapter.number) }.isSuccess
    }

    fun selectTrack(id: String) {
        val target = V1TextbookCatalog.chapterById(id) ?: return
        progressStore.saveSelectedChapter(target.id)
        selectedChapterId = target.id
    }

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
                        text = "TRACK을 고르면 해당 책의 읽던 위치로 이동합니다.",
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
                                selectTrack(track.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 780.dp)
                    .fillMaxWidth()
                    .testTag("reader_single_column")
            ) {
                key(selectedChapterId, hasV5Book, remoteGeneration) {
                    if (hasV5Book) {
                        V5TrackBookScreen(
                            trackNumber = selectedChapter.number,
                            onNavigateBack = onNavigateBack,
                            onOpenToc = { scope.launch { drawerState.open() } },
                            onStartPractice = onStartPractice,
                            onPreviousTrack = V1TextbookCatalog.chapters
                                .getOrNull(selectedChapter.number - 2)
                                ?.let { previous -> { selectTrack(previous.id) } },
                            onNextTrack = V1TextbookCatalog.chapters
                                .getOrNull(selectedChapter.number)
                                ?.let { next -> { selectTrack(next.id) } }
                        )
                    } else {
                        V4PagedBookScreen(
                            practiceCompletedIds = practiceCompletedIds,
                            onNavigateBack = onNavigateBack,
                            onStartPractice = onStartPractice,
                            initialChapterId = selectedChapterId,
                            onOpenToc = { scope.launch { drawerState.open() } },
                            onChapterSelected = { selectedChapterId = it }
                        )
                    }
                }
            }
        }
    }
}
