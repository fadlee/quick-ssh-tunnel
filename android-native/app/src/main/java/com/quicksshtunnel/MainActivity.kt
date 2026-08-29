package com.quicksshtunnel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quicksshtunnel.ui.FormScreen
import com.quicksshtunnel.ui.ListScreen
import com.quicksshtunnel.ui.QuickSshTunnelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickSshTunnelTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list",
    ) {
        composable("list") {
            ListScreen(
                onNew = { navController.navigate("form") },
                onEdit = { id -> navController.navigate("form?connectionId=$id") },
            )
        }
        composable(
            route = "form?connectionId={connectionId}",
            arguments = listOf(
                navArgument("connectionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val connectionId = backStackEntry.arguments?.getString("connectionId")
            FormScreen(
                connectionId = connectionId,
                onCancel = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
