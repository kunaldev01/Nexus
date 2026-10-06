package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.DashboardScreen
import com.example.ui.FitnessScreen
import com.example.ui.GoalScreen
import com.example.ui.MealScreen
import com.example.ui.AiCoachScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LifeOsViewModel
import com.example.state.LifeOsStateProvider

enum class Screen {
    DASHBOARD,
    FITNESS,
    MEAL,
    AI_PLANNER,
    AI_COACH
}

class MainActivity : ComponentActivity() {

    private val viewModel: LifeOsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appThemeStr by viewModel.appThemeStr.collectAsState()
            LifeOsStateProvider(viewModel = viewModel) {
                MyApplicationTheme(themeName = appThemeStr) {
                    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsState()

                    if (!isGoogleSignedIn) {
                        com.example.ui.GoogleLoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = { /* Login screen state flow handles state changes triggering composition */ }
                        )
                    } else {
                        var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                        Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.DASHBOARD,
                                    onClick = { currentScreen = Screen.DASHBOARD },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                    label = { Text("Command") },
                                    modifier = Modifier.testTag("nav_item_dashboard")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.FITNESS,
                                    onClick = { currentScreen = Screen.FITNESS },
                                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Fitness Progress") },
                                    label = { Text("Fitness") },
                                    modifier = Modifier.testTag("nav_item_fitness")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.MEAL,
                                    onClick = { currentScreen = Screen.MEAL },
                                    icon = { Icon(Icons.Default.Star, contentDescription = "Kitchen Nutrition") },
                                    label = { Text("Kitchen") },
                                    modifier = Modifier.testTag("nav_item_meal")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.AI_PLANNER,
                                    onClick = { currentScreen = Screen.AI_PLANNER },
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "AI Goal Planner") },
                                    label = { Text("Decompose") },
                                    modifier = Modifier.testTag("nav_item_goals")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.AI_COACH,
                                    onClick = { currentScreen = Screen.AI_COACH },
                                    icon = { Icon(Icons.Default.Info, contentDescription = "AI Advisor Guidance") },
                                    label = { Text("AI Coach") },
                                    modifier = Modifier.testTag("nav_item_ai_coach")
                                )
                            }
                        }
                    ) { innerPadding ->
                        val contentModifier = Modifier
                            .padding(innerPadding)
                            .systemBarsPadding() // Ensures safe area / notch avoidance

                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                                (slideInHorizontally { width -> direction * width / 2 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -direction * width / 2 } + fadeOut())
                            },
                            label = "ScreenTransition"
                        ) { targetScreen ->
                            when (targetScreen) {
                                Screen.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    modifier = contentModifier,
                                    onNavigateToFitness = { currentScreen = Screen.FITNESS },
                                    onNavigateToMeals = { currentScreen = Screen.MEAL }
                                )
                                Screen.FITNESS -> FitnessScreen(
                                    viewModel = viewModel,
                                    modifier = contentModifier
                                )
                                Screen.MEAL -> MealScreen(
                                    viewModel = viewModel,
                                    modifier = contentModifier
                                )
                                Screen.AI_PLANNER -> GoalScreen(
                                    viewModel = viewModel,
                                    modifier = contentModifier
                                )
                                Screen.AI_COACH -> AiCoachScreen(
                                    viewModel = viewModel,
                                    modifier = contentModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
