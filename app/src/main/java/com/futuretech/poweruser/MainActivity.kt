package com.futuretech.poweruser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.futuretech.poweruser.ui.*
import com.futuretech.poweruser.ui.theme.FutureTechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FutureTechTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainHomeScreen(
                onNavigateToLesson = { lessonId -> navController.navigate("lesson/$lessonId") },
                onNavigateToReview = { navController.navigate("review") },
                onNavigateToErrorNotes = { navController.navigate("error_notes") },
                onNavigateToCustomProject = { navController.navigate("custom_project") },
                onNavigateToBeginnerProject = { navController.navigate("beginner_project") },
                onNavigateToIntermediateProject = { navController.navigate("intermediate_project") }
            )
        }
        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: "B01"
            LessonDetailScreen(
                lessonId = lessonId,
                onNavigateBack = { navController.popBackStack() },
                onRecordErrorNote = { type, code, msg, guide -> }
            )
        }
        composable("review") {
            SpacedRepetitionReviewScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("error_notes") {
            PersonalErrorNotesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("beginner_project") {
            BeginnerStockScoreProjectScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("intermediate_project") {
            IntermediateStockResearchProjectScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("custom_project") {
            CustomProjectBuilderScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
