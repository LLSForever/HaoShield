package com.haoshield.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.ui.guide.MakeShieldGuideScreen
import com.haoshield.ui.home.HomeScreen
import com.haoshield.ui.intro.IntroScreen
import com.haoshield.ui.journal.JournalScreen
import com.haoshield.ui.journal.UnblockAppScreen
import com.haoshield.ui.permissions.PermissionsScreen
import com.haoshield.ui.protectedscreen.ProtectedScreen
import com.haoshield.ui.reflection.ReflectionScreen
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
        // Screen changes are quiet crossfades — turning a page, not pushing a card.
        enterTransition = { fadeIn(animationSpec = tween(HaoMotion.STANDARD)) },
        exitTransition = { fadeOut(animationSpec = tween(HaoMotion.STANDARD)) },
        popEnterTransition = { fadeIn(animationSpec = tween(HaoMotion.STANDARD)) },
        popExitTransition = { fadeOut(animationSpec = tween(HaoMotion.STANDARD)) },
    ) {
        composable(Route.Intro.path) {
            IntroScreen(
                onFinished = {
                    if (navController.previousBackStackEntry != null) {
                        // Replayed from Settings — just return.
                        navController.popBackStack()
                    } else {
                        // First run — replace the intro with Home.
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Intro.path) { inclusive = true }
                        }
                    }
                },
            )
        }
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
                onNavigateToIntro = { navController.navigate(Route.Intro.path) },
            )
        }
        composable(Route.Permissions.path) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Route.Protected.path,
            // Entering and leaving a session is the app's most meaningful transition — slower.
            enterTransition = { fadeIn(animationSpec = tween(HaoMotion.SLOW)) },
            exitTransition = { fadeOut(animationSpec = tween(HaoMotion.SLOW)) },
            popEnterTransition = { fadeIn(animationSpec = tween(HaoMotion.SLOW)) },
            popExitTransition = { fadeOut(animationSpec = tween(HaoMotion.SLOW)) },
        ) {
            ProtectedScreen(
                onNavigateHome = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Home.path) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToReflection = {
                    navController.navigate(Route.Reflection.path) {
                        popUpTo(Route.Home.path) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToScanner = {
                    navController.navigate(Route.QrScanner.createRoute(ScanMode.SESSION))
                },
            )
        }
        composable(
            route = Route.Reflection.path,
            enterTransition = { fadeIn(animationSpec = tween(HaoMotion.SLOW)) },
            exitTransition = { fadeOut(animationSpec = tween(HaoMotion.SLOW)) },
            popEnterTransition = { fadeIn(animationSpec = tween(HaoMotion.SLOW)) },
            popExitTransition = { fadeOut(animationSpec = tween(HaoMotion.SLOW)) },
        ) {
            ReflectionScreen(
                onDone = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Home.path) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Route.Journal.path) {
            JournalScreen(
                onNavigateBack = { navController.popBackStack() },
            )
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