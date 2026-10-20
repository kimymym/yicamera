package com.github.kimymym.yicamera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.kimymym.yicamera.ui.connect.ConnectRoute
import com.github.kimymym.yicamera.ui.main.MainRoute

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Connect.route,
        modifier = modifier
    ) {
        composable(Screen.Connect.route) {
            ConnectRoute(
                onConnected = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Connect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainRoute(
                onNavigateBackToConnect = {
                    navController.navigate(Screen.Connect.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
