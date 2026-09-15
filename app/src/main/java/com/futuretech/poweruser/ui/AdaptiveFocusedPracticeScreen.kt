package com.futuretech.poweruser.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.education.LearningSessionMode
import com.futuretech.poweruser.education.LearningUi21To25Policy
import com.futuretech.poweruser.education.MasteryEvidence

/**
 * Items 21-25 adaptive shell.
 *
 * Phone: keep the practice flow vertical (problem -> code -> result).
 * Tablet landscape: reserve ~40% for the problem/learning frame and ~60% for the existing
 * deterministic Run/Submit workspace. No server, paid API, or remote layout dependency is used.
 */
@Composable
fun AdaptiveFocusedPracticeScreen(
    lessonId: String,
    mode: LearningSessionMode = LearningSessionMode.PRACTICE,
    onNavigateBack: () -> Unit,
    onSwitchMode: () -> Unit,
    onReviewLecture: (Int) -> Unit,
    onMasteryCompleted: (String, String, Int, MasteryEvidence) -> Unit,
    onRecordErrorNote: (String, String, String, String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val split = LearningUi21To25Policy.practiceSplit(
        smallestScreenWidthDp = configuration.smallestScreenWidthDp,
        isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    )

    if (split == null) {
        Box(
            Modifier
                .fillMaxSize()
                .testTag("practice_phone_vertical")
        ) {
            FocusedPracticeScreen(
                lessonId = lessonId,
                mode = mode,
                onNavigateBack = onNavigateBack,
                onSwitchMode = onSwitchMode,
                onReviewLecture = onReviewLecture,
                onMasteryCompleted = onMasteryCompleted,
                onRecordErrorNote = onRecordErrorNote
            )
        }
        return
    }

    val lesson = remember(lessonId) {
        CurriculumDataRepository.lessonById(lessonId) ?: CurriculumDataRepository.beginnerLessons.first()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("practice_tablet_landscape_split")
    ) {
        Surface(
            modifier = Modifier
                .weight(split.problemWeight)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (mode == LearningSessionMode.PRACTICE) "문제 · 학습 가이드" else "문제 · 독립 평가",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lesson.title,
                    fontSize = 24.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = lesson.expectedOutcome,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("난이도는 이렇게 올라갑니다", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LearningUi21To25Policy.guidanceOrder.forEach { phase ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = phase.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = phase.description,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("힌트 원칙", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (mode == LearningSessionMode.PRACTICE) {
                                "필요할 때만 1 → 2 → 3 순서로 엽니다. 힌트를 써도 벌점은 없고 숙련 증거의 강도만 낮아집니다."
                            } else {
                                "Chapter Challenge에서는 힌트와 AI 도움을 잠그고 혼자 풀 수 있는지 확인합니다."
                            },
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "오른쪽 작업공간에서 코드를 수정하고 ▶ 실행으로 자유 실험한 뒤 ✓ 제출하세요. 실행 결과와 제출 결과는 코드 아래에서 확인합니다.",
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline)
        )

        Box(
            Modifier
                .weight(split.workspaceWeight)
                .fillMaxHeight()
                .testTag("practice_tablet_workspace")
        ) {
            FocusedPracticeScreen(
                lessonId = lessonId,
                mode = mode,
                onNavigateBack = onNavigateBack,
                onSwitchMode = onSwitchMode,
                onReviewLecture = onReviewLecture,
                onMasteryCompleted = onMasteryCompleted,
                onRecordErrorNote = onRecordErrorNote
            )
        }
    }
}
