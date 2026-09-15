package com.futuretech.poweruser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.futuretech.poweruser.data.AppRepository
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.ui.BeginnerStockScoreProjectScreen
import com.futuretech.poweruser.ui.CurriculumOverviewScreen
import com.futuretech.poweruser.ui.CustomProjectBuilderScreen
import com.futuretech.poweruser.ui.FeedbackPracticeScreen
import com.futuretech.poweruser.ui.IntermediateStockResearchProjectScreen
import com.futuretech.poweruser.ui.MainHomeScreen
import com.futuretech.poweruser.ui.PersonalErrorNotesScreen
import com.futuretech.poweruser.ui.SelfUpdateBanner
import com.futuretech.poweruser.ui.SpacedRepetitionReviewScreen
import com.futuretech.poweruser.ui.TeachingLessonScreen
import com.futuretech.poweruser.ui.V1TextbookScreen
import com.futuretech.poweruser.ui.theme.FutureTechTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FutureTechTheme(darkTheme = true) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SelfUpdateBanner()
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    val scope = rememberCoroutineScope()
    val progressList by repository.getAllProgress().collectAsState(initial = emptyList())
    val errorNotesList by repository.getAllErrorNotes().collectAsState(initial = emptyList())

    fun recordCompletion(id: String, cType: String, unitNum: Int) {
        scope.launch {
            val lesson = CurriculumDataRepository.lessonById(id)
            repository.updateProgress(
                lessonId = id,
                curriculumType = cType,
                unitNumber = unitNum,
                title = lesson?.title ?: id,
                status = "VERIFIABLE",
                completionPercentage = 100,
                isCompleted = true
            )
            if (lesson != null) {
                repository.addSpacedRepetitionItem(
                    conceptId = lesson.lessonId,
                    conceptTitle = lesson.title,
                    category = lesson.curriculumType,
                    definition = lesson.explanation,
                    analogy = lesson.expectedOutcome,
                    example = lesson.codeSample,
                    comparison = lesson.aiHallucinationQuestion
                )
            }
        }
    }

    NavHost(navController = navController, startDestination = "curriculum") {
        composable("curriculum") {
            CurriculumOverviewScreen(
                onOpenChapter = { chapterId -> navController.navigate("textbook_v1/$chapterId") }
            )
        }
        composable("textbook_v1") {
            V1TextbookScreen(
                practiceCompletedIds = progressList.filter { it.isCompleted }.map { it.lessonId }.toSet(),
                onNavigateBack = { navController.navigate("curriculum") },
                onStartPractice = { lessonId -> navController.navigate("textbook_v1/practice/$lessonId") }
            )
        }
        composable("textbook_v1/{chapterId}") { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: "V1-C01"
            V1TextbookScreen(
                practiceCompletedIds = progressList.filter { it.isCompleted }.map { it.lessonId }.toSet(),
                onNavigateBack = { navController.popBackStack() },
                onStartPractice = { lessonId -> navController.navigate("textbook_v1/practice/$lessonId") },
                initialChapterId = chapterId
            )
        }
        composable("home") {
            MainHomeScreen(
                progressList = progressList,
                onNavigateToLesson = { lessonId -> navController.navigate("lesson/$lessonId") },
                onNavigateToReview = { navController.navigate("review") },
                onNavigateToErrorNotes = { navController.navigate("error_notes") },
                onNavigateToCustomProject = { navController.navigate("custom_project") },
                onNavigateToBeginnerProject = { navController.navigate("beginner_project") },
                onNavigateToIntermediateProject = { navController.navigate("intermediate_project") }
            )
        }
        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: "B01-01"
            TeachingLessonScreen(
                lessonId = lessonId,
                onNavigateBack = { navController.popBackStack() },
                onStepCompleted = ::recordCompletion,
                onRecordErrorNote = { type, code, msg, guide ->
                    scope.launch { repository.recordErrorNote(type, code, msg, guide) }
                }
            )
        }
        composable("textbook_v1/practice/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: "TB1-C01"
            FeedbackPracticeScreen(
                lessonId = lessonId,
                onNavigateBack = { navController.popBackStack() },
                onReviewLecture = { navController.popBackStack() },
                onStepCompleted = ::recordCompletion,
                onRecordErrorNote = { type, code, msg, guide ->
                    scope.launch { repository.recordErrorNote(type, code, msg, guide) }
                }
            )
        }
        composable("review") { SpacedRepetitionReviewScreen(repository = repository, onNavigateBack = { navController.popBackStack() }) }
        composable("error_notes") { PersonalErrorNotesScreen(errorNotesList = errorNotesList, onNavigateBack = { navController.popBackStack() }) }
        composable("beginner_project") { BeginnerStockScoreProjectScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("intermediate_project") { IntermediateStockResearchProjectScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("custom_project") { CustomProjectBuilderScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}
