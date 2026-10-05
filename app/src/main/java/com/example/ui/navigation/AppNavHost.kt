package com.example.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.ArdenViewModel

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomNavItem(NavRoutes.HOME, "Home", Icons.Default.Home)
    data object Solve : BottomNavItem(NavRoutes.SOLVE_METHOD, "Solve", Icons.Default.Calculate)
    data object Examples : BottomNavItem(NavRoutes.EXAMPLES, "Examples", Icons.Default.MenuBook)
    data object Learn : BottomNavItem(NavRoutes.LEARN, "Learn", Icons.Default.Lightbulb)
}

@Composable
fun AppNavHost(
    viewModel: ArdenViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Solve,
        BottomNavItem.Examples,
        BottomNavItem.Learn
    )

    val showBottomBar = currentRoute in listOf(
        NavRoutes.HOME,
        NavRoutes.SOLVE_METHOD,
        NavRoutes.EXAMPLES,
        NavRoutes.LEARN
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2563EB)
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2563EB),
                                selectedTextColor = Color(0xFF2563EB),
                                indicatorColor = Color(0xFFEFF6FF),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            )
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(NavRoutes.SOLVE_METHOD) {
                SolveMethodScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.INPUT_MANUAL) {
                ManualInputScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.INPUT_COMPACT) {
                CompactInputScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.INPUT_IMAGE_OCR) {
                ImageOcrScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.DFA_VIEW) {
                DfaVisualizerScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.STATE_EQUATIONS) {
                StateLanguageEquationsScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.STEP_SOLVER) {
                StepSolverScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.FINAL_RESULT) {
                FinalResultScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.VERIFICATION) {
                VerificationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.SIMULATION) {
                SimulationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.EXAMPLES) {
                ExamplesScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.LEARN) {
                LearnScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.HOW_IT_WORKS) {
                HowItWorksScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
