package com.aimanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aimanager.feature.analytics.AnalyticsScreen
import com.aimanager.feature.canvas.CanvasScreen
import com.aimanager.feature.chat.ChatScreen
import com.aimanager.feature.compare.CompareScreen
import com.aimanager.feature.gems.GemsScreen
import com.aimanager.feature.settings.SettingsScreen
import com.aimanager.feature.skills.SkillsScreen

@Composable
fun AINavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToSkills = { navController.navigate("skills") },
                onNavigateToGems = { navController.navigate("gems") },
                onNavigateToAnalytics = { navController.navigate("analytics") }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("skills") {
            SkillsScreen(onBack = { navController.popBackStack() })
        }
        composable("gems") {
            GemsScreen(onBack = { navController.popBackStack() })
        }
        composable("analytics") {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
        composable("compare") {
            CompareScreen(onBack = { navController.popBackStack() })
        }
        composable("canvas") {
            CanvasScreen(onBack = { navController.popBackStack() })
        }
    }
}
