package com.haoshield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.haoshield.ui.guide.MakeShieldGuideScreen
import com.haoshield.ui.home.HomeScreen
import com.haoshield.ui.journal.JournalScreen
import com.haoshield.ui.journal.UnblockAppScreen
import com.haoshield.ui.protectedscreen.ProtectedScreen

@Composable
fun HaoShieldNavHost(
    navController: NavHostController,
    startDestination: String = Route.Home.path,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Route.Home.path) {
            HomeScreen(
                onNavigate = navController::navigate,
                onNavigateToProtected = {
                    navController.navigate(Route.Protected.path) {
                        popUpTo(Route.Home.path) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Route.ShieldMode.path) {
            // Shield mode screen — Phase 1 UI
        }
        composable(Route.Protected.path) {
            ProtectedScreen(
                onNavigateHome = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Home.path) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Route.Journal.path) {
            JournalScreen()
        }
        composable(
            route = Route.Unblock.path,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
            ),
        ) {
            UnblockAppScreen(
                onComplete = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Route.Guide.path) {
            MakeShieldGuideScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Route.Letters.path) {
            // Hǎo Letters opt-in — Phase 1 UI
        }
    }
}