package com.haoshield.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.ui.guide.MakeShieldGuideScreen
import com.haoshield.ui.home.HomeScreen
import com.haoshield.ui.journal.JournalScreen
import com.haoshield.ui.journal.UnblockAppScreen
import com.haoshield.ui.permissions.PermissionsScreen
import com.haoshield.ui.protectedscreen.ProtectedScreen
import com.haoshield.ui.scanner.QrScannerScreen
import com.haoshield.ui.settings.SettingsScreen
import com.haoshield.ui.setup.SetupScreen

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
        composable(Route.Setup.path) {
            SetupScreen(
                onNavigateToProtected = {
                    navController.navigate(Route.Protected.path) {
                        popUpTo(Route.Home.path) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Route.Settings.path) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGuide = { navController.navigate(Route.Guide.path) },
                onNavigateToPermissions = { navController.navigate(Route.Permissions.path) },
            )
        }
        composable(Route.Permissions.path) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Route.Protected.path) {
            ProtectedScreen(
                onNavigateHome = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Home.path) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToScanner = {
                    navController.navigate(Route.QrScanner.createRoute(ScanMode.SESSION))
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
                onNavigateToScanner = { mode ->
                    navController.navigate(Route.QrScanner.createRoute(mode))
                },
            )
        }
        composable(
            route = Route.QrScanner.path,
            arguments = listOf(
                navArgument(Route.QrScanner.ARG_SCAN_MODE) { type = NavType.StringType },
            ),
        ) {
            QrScannerScreen(
                onFinished = { result ->
                    // Session start/end navigation is owned by MainActivity's global collector
                    // (it rebuilds the back stack via popUpTo(Home)); popping here too would race
                    // with it. For registration and errors, just return to the caller.
                    when (result) {
                        is ShieldScanResult.SessionStarted,
                        is ShieldScanResult.SessionEnded -> Unit
                        else -> navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}