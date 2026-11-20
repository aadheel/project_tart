package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.editor.AlarmEditorScreen
import com.example.ui.editor.AlarmEditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.TaktTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_EDITOR_BASE = "editor"
private const val ARG_ALARM_ID = "alarmId"
private const val ROUTE_EDITOR = "$ROUTE_EDITOR_BASE/{$ARG_ALARM_ID}"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as TaktApplication).container

        setContent {
            val isDarkMode by container.themePreferences.isDarkMode
                .collectAsStateWithLifecycle(initialValue = true)

            LaunchedEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            TaktTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME
                ) {
                    composable(ROUTE_HOME) {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.factory(
                                alarmRepository = container.alarmRepository,
                                themePreferences = container.themePreferences
                            )
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            onAddAlarm = {
                                navController.navigate("$ROUTE_EDITOR_BASE/0")
                            },
                            onEditAlarm = { alarmId ->
                                navController.navigate("$ROUTE_EDITOR_BASE/$alarmId")
                            }
                        )
                    }

                    composable(
                        route = ROUTE_EDITOR,
                        arguments = listOf(
                            navArgument(ARG_ALARM_ID) {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        ),
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up,
                                animationSpec = tween(260)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Down,
                                animationSpec = tween(220)
                            )
                        }
                    ) { backStackEntry ->
                        val alarmId = backStackEntry.arguments?.getLong(ARG_ALARM_ID) ?: 0L
                        val editorViewModel: AlarmEditorViewModel = viewModel(
                            factory = AlarmEditorViewModel.factory(
                                alarmId = alarmId.takeIf { it > 0L },
                                repository = container.alarmRepository
                            )
                        )
                        AlarmEditorScreen(
                            viewModel = editorViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
