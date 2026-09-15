package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.education.LearningSessionMode
import com.futuretech.poweruser.education.LectureContentRepository
import com.futuretech.poweruser.education.LectureProgressStore
import com.futuretech.poweruser.education.LectureSection
import com.futuretech.poweruser.education.LearningImportance
import com.futuretech.poweruser.education.MasteryEvidence
import com.futuretech.poweruser.ui.theme.CodeTypography

@Composable
fun TeachingLessonScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onStepCompleted: (String, String, Int) -> Unit,
    onRecordErrorNote: (String, String, String, String) -> Unit,
    onMasteryCompleted: ((String, String, Int, MasteryEvidence) -> Unit)? = null,
    onStartChallenge: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lesson = remember(lessonId) {
        CurriculumDataRepository.lessonById(lessonId) ?: CurriculumDataRepository.beginnerLessons.first()
    }
    val lecture = remember(lesson.lessonId) { LectureContentRepository.forLesson(lesson) }
    val progressStore = remember { LectureProgressStore(context) }

    var lectureCompleted by rememberSaveable(lesson.lessonId) {
        mutableStateOf(progressStore.isLectureCompleted(lesson.lessonId))
    }
    var showPractice by rememberSaveable(lesson.lessonId) { mutableStateOf(false) }
    var sectionIndex by rememberSaveable(lesson.lessonId) {
        mutableStateOf(progressStore.sectionIndex(lesson.lessonId).coerceIn(0, lecture.sections.lastIndex))
    }

    if (showPractice) {
        FocusedPracticeScreen(
            lessonId = lesson.lessonId,
            mode = LearningSessionMode.PRACTICE,
            onNavigateBack = onNavigateBack,
            onSwitchMode = onStartChallenge ?: {},
            onReviewLecture = { targetIndex ->
                sectionIndex = targetIndex.coerceIn(0, lecture.sections.lastIndex)
                progressStore.saveSectionIndex(lesson.lessonId, sectionIndex)
                showPractice = false
            },
            onMasteryCompleted = { id, type, unit, evidence ->
                if (onMasteryCompleted != null) {
                    onMasteryCompleted(id, type, unit, evidence)
                } else {
                    onStepCompleted(id, type, unit)
                }
            },
            onRecordErrorNote = onRecordErrorNote
        )
        return
    }

    LecturePhaseScreen(
        lessonTitle = lesson.title,
        moduleTitle = lesson.moduleTitle,
        levelNumber = lesson.stepNumber,
        estimatedMinutes = lecture.estimatedLectureMinutes,
        beginnerAssumption = lecture.beginnerAssumption,
        section = lecture.sections[sectionIndex],
        sectionIndex = sectionIndex,
        totalSections = lecture.sections.size,
        practiceUnlocked = lectureCompleted,
        onBack = onNavigateBack,
        onPrevious = {
            if (sectionIndex > 0) {
                sectionIndex--
                progressStore.saveSectionIndex(lesson.lessonId, sectionIndex)
            }
        },
        onNext = {
            if (sectionIndex < lecture.sections.lastIndex) {
                sectionIndex++
                progressStore.saveSectionIndex(lesson.lessonId, sectionIndex)
            }
        },
        onStartPractice = { showPractice = true },
        onFinishLecture = {
            progressStore.markLectureCompleted(lesson.lessonId)
            lectureCompleted = true
            showPractice = true
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LecturePhaseScreen(
    lessonTitle: String,
    moduleTitle: String,
    levelNumber: Int,
    estimatedMinutes: Int,
    beginnerAssumption: String,
    section: LectureSection,
    sectionIndex: Int,
    totalSections: Int,
    practiceUnlocked: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStartPractice: () -> Unit,
    onFinishLecture: () -> Unit
) {
    val isLast = sectionIndex == totalSections - 1

    Scaffold(
        modifier = Modifier.testTag("lecture_root"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(moduleTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Level $levelNumber · $lessonTitle", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 목록") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("강의 먼저 · 문제는 나중에", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(beginnerAssumption, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("예상 강의 ${estimatedMinutes}분 · 강의 ${sectionIndex + 1}/$totalSections", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (sectionIndex + 1).toFloat() / totalSections.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (practiceUnlocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("practice_unlocked_label"),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "이 강의는 완료했습니다. 학습 내용은 언제든 다시 볼 수 있습니다.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = onStartPractice,
                            modifier = Modifier.fillMaxWidth().testTag("lecture_start_practice")
                        ) {
                            Text("실습으로 이동 →")
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("practice_locked_label"),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "문제·코딩 실습은 이 강의를 모두 본 뒤 열립니다.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(section.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        ImportanceBadge(section.importance)
                    }

                    Text(section.body, fontSize = 15.sp, lineHeight = 24.sp)

                    if (section.code.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                section.code,
                                modifier = Modifier.padding(14.dp),
                                style = CodeTypography,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (section.takeaway.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("이 페이지에서 딱 하나", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(section.takeaway, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = sectionIndex > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("← 이전 강의") }

                if (!isLast) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f).testTag("lecture_next")
                    ) { Text("다음 강의 →") }
                } else if (practiceUnlocked) {
                    Button(
                        onClick = onStartPractice,
                        modifier = Modifier.weight(1f).testTag("lecture_start_practice_bottom")
                    ) { Text("실습 시작 →") }
                } else {
                    Button(
                        onClick = onFinishLecture,
                        modifier = Modifier.weight(1f).testTag("lecture_finish")
                    ) { Text("강의 완료 · 실습 시작") }
                }
            }

            Text(
                "페이지를 넘길 때마다 위치가 자동 저장됩니다. 앱을 닫았다가 다시 열어도 같은 강의에서 이어집니다.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ImportanceBadge(importance: LearningImportance) {
    val color = when (importance) {
        LearningImportance.MUST_UNDERSTAND -> MaterialTheme.colorScheme.primary
        LearningImportance.MUST_PRACTICE -> MaterialTheme.colorScheme.secondary
        LearningImportance.AI_CAN_HELP -> MaterialTheme.colorScheme.tertiary
        LearningImportance.REFERENCE -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(
            importance.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
