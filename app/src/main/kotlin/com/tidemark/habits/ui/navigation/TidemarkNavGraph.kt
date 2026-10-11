package com.tidemark.habits.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tidemark.habits.AppContainer
import com.tidemark.habits.R
import com.tidemark.habits.ui.detail.HabitDetailScreen
import com.tidemark.habits.ui.detail.HabitDetailViewModel
import com.tidemark.habits.ui.editor.HabitEditorScreen
import com.tidemark.habits.ui.editor.HabitEditorViewModel
import com.tidemark.habits.ui.habits.HabitsScreen
import com.tidemark.habits.ui.habits.HabitsViewModel
import com.tidemark.habits.ui.settings.SettingsScreen
import com.tidemark.habits.ui.settings.SettingsViewModel
import com.tidemark.habits.ui.today.TodayScreen
import com.tidemark.habits.ui.today.TodayViewModel

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Habits : Screen("habits")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object Editor : Screen("editor?habitId={habitId}&prefill={prefill}") {
        fun createRoute(habitId: Long? = null, prefill: String? = null): String {
            val idParam = habitId?.toString() ?: ""
            val prefillParam = prefill ?: ""
            return "editor?habitId=$idParam&prefill=$prefillParam"
        }
    }
    data object Detail : Screen("detail/{habitId}") {
        fun createRoute(habitId: Long): String = "detail/$habitId"
    }
}

data class BottomNavTab(
    val screen: Screen,
    val title: String,
    val iconRes: Int
)

@Composable
fun TidemarkNavGraph(
    container: AppContainer,
    onDateChangedTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomTabs = listOf(
        BottomNavTab(Screen.Today, "Today", R.drawable.ic_check),
        BottomNavTab(Screen.Habits, "Habits", R.drawable.ic_wave),
        BottomNavTab(Screen.Stats, "Stats", R.drawable.ic_chart),
        BottomNavTab(Screen.Settings, "Settings", R.drawable.ic_settings)
    )

    val isTopLevel = bottomTabs.any { it.screen.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != tab.screen.route) {
                                    navController.navigate(tab.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            // TODAY
            composable(Screen.Today.route) {
                val viewModel = viewModel<TodayViewModel> {
                    TodayViewModel(
                        repository = container.habitRepository,
                        settingsRepository = container.settingsRepository,
                        timeProvider = container.timeProvider
                    )
                }
                TodayScreen(
                    viewModel = viewModel,
                    onNavigateToHabitDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                    onNavigateToCreateHabit = { prefill -> navController.navigate(Screen.Editor.createRoute(prefill = prefill)) },
                    onNavigateToEditHabit = { id -> navController.navigate(Screen.Editor.createRoute(habitId = id)) }
                )
            }

            // HABITS
            composable(Screen.Habits.route) {
                val viewModel = viewModel<HabitsViewModel> {
                    HabitsViewModel(
                        repository = container.habitRepository,
                        onCancelReminder = { id -> container.reminderScheduler.cancelReminder(id) },
                        timeProvider = container.timeProvider
                    )
                }
                HabitsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                    onNavigateToEdit = { id -> navController.navigate(Screen.Editor.createRoute(habitId = id)) },
                    onNavigateToCreate = { navController.navigate(Screen.Editor.createRoute()) }
                )
            }

            // STATS
            composable(Screen.Stats.route) {
                val viewModel = viewModel<StatsViewModel> {
                    StatsViewModel(
                        repository = container.habitRepository,
                        timeProvider = container.timeProvider
                    )
                }
                com.tidemark.habits.ui.stats.StatsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }

            // SETTINGS
            composable(Screen.Settings.route) {
                val viewModel = viewModel<SettingsViewModel> {
                    SettingsViewModel(
                        repository = container.habitRepository,
                        settingsRepository = container.settingsRepository,
                        timeProvider = container.timeProvider,
                        onScheduleTestReminder = { secs -> container.reminderScheduler.scheduleTestReminder(secs) },
                        onRescheduleAll = {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                val habits = container.habitRepository.observeHabits()
                                    .let { kotlinx.coroutines.flow.first(it) }
                                container.reminderScheduler.rescheduleAll(habits)
                            }
                        }
                    )
                }
                SettingsScreen(viewModel = viewModel)
            }

            // EDITOR (Pushed route with slide animation)
            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("habitId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefill") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) }
            ) { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("habitId")
                val habitId = idStr?.toLongOrNull()
                val prefill = backStackEntry.arguments?.getString("prefill")?.ifEmpty { null }

                val viewModel = viewModel<HabitEditorViewModel>(key = "editor_${habitId}_$prefill") {
                    HabitEditorViewModel(
                        habitId = habitId,
                        prefillName = prefill,
                        repository = container.habitRepository,
                        timeProvider = container.timeProvider,
                        onScheduleReminder = { habit -> container.reminderScheduler.scheduleReminder(habit) },
                        onCancelReminder = { id -> container.reminderScheduler.cancelReminder(id) }
                    )
                }

                HabitEditorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            // DETAIL (Pushed route with slide animation)
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("habitId") { type = NavType.LongType }
                ),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) }
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L

                val viewModel = viewModel<HabitDetailViewModel>(key = "detail_$habitId") {
                    HabitDetailViewModel(
                        habitId = habitId,
                        repository = container.habitRepository,
                        timeProvider = container.timeProvider,
                        onCancelReminder = { id -> container.reminderScheduler.cancelReminder(id) }
                    )
                }

                HabitDetailScreen(
                    viewModel = viewModel,
                    today = container.timeProvider.today(),
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.Editor.createRoute(habitId = id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}
