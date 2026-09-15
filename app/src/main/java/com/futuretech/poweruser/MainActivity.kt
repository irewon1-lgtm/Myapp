package com.futuretech.poweruser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.futuretech.poweruser.data.AppRepository
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.ui.*
import com.futuretech.poweruser.ui.theme.FutureTechTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FutureTechTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
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

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Box(modifier = Modifier.fillMaxSize()) {
                MainHomeScreen(
                    progressList = progressList,
                    onNavigateToLesson = { lessonId -> navController.navigate("lesson/$lessonId") },
                    onNavigateToReview = { navController.navigate("review") },
                    onNavigateToErrorNotes = { navController.navigate("error_notes") },
                    onNavigateToCustomProject = { navController.navigate("custom_project") },
                    onNavigateToBeginnerProject = { navController.navigate("beginner_project") },
                    onNavigateToIntermediateProject = { navController.navigate("intermediate_project") }
                )
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("textbook_v1") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("textbook_v1_entry"),
                    content = { Text("V1 대학교재 · v1.1.0") }
                )
            }
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
        composable("textbook_v1") {
            V1TextbookScreen(
                practiceCompletedIds = progressList.filter { it.isCompleted }.map { it.lessonId }.toSet(),
                onNavigateBack = { navController.popBackStack() },
                onStartPractice = { lessonId -> navController.navigate("textbook_v1/practice/$lessonId") }
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
