package com.futuretech.poweruser.ui

import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.V1TextbookCatalog
import kotlinx.coroutines.launch

/**
 * Item 23: reading stays a single centered book column on phone/tablet.
 * Chapter navigation is hidden in a temporary drawer instead of permanently consuming width.
 */
@Composable
fun AdaptiveTextbookScreen(
    practiceCompletedIds: Set<String>,
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    initialChapterId: String = "V1-C01"
) {
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
                        .padding(vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "1권 목차",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "읽는 동안에는 닫혀 있고, 필요할 때만 엽니다.",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    V1TextbookCatalog.chapters.forEach { chapter ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = "${chapter.number}. ${chapter.title}",
                                    maxLines = 2
                                )
                            },
                            selected = chapter.id == selectedChapterId,
                            onClick = {
                                selectedChapterId = chapter.id
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
            Modifier
                .fillMaxSize()
                .testTag("reader_single_column")
        ) {
            key(selectedChapterId) {
                V1TextbookScreen(
                    practiceCompletedIds = practiceCompletedIds,
                    onNavigateBack = onNavigateBack,
                    onStartPractice = onStartPractice,
                    initialChapterId = selectedChapterId
                )
            }

            FloatingActionButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("reader_toc_button"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("목차", fontWeight = FontWeight.Bold)
            }
        }
    }
}
