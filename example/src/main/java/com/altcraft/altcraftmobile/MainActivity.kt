package com.altcraft.altcraftmobile

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.altcraft.altcraftmobile.deeplink.DeepLink.resolveStartDestination
import com.altcraft.altcraftmobile.view.config.ConfigScreen
import com.altcraft.altcraftmobile.view.home.HomeScreen
import com.altcraft.altcraftmobile.view.logs.LogScreen.LogScreen
import com.altcraft.altcraftmobile.view.menu.UIMenu
import com.altcraft.altcraftmobile.ui.theme.AltcraftMobileTheme
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.altcraftmobile.view.example.ExampleScreen
import com.altcraft.sdk.AltcraftSDK
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        WindowCompat.setDecorFitsSystemWindows(window, false)
        resources.configuration.fontScale = 1f
        AltcraftSDK.requestNotificationPermission(this, this)

        setContent {
            AltcraftMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    navController = rememberNavController()
                    MainScreen(
                        viewModel = viewModel,
                        navController = navController,
                        startDestination = resolveStartDestination(intent)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val newDestination = resolveStartDestination(intent)
        if (::navController.isInitialized) {
            navController.navigate(newDestination) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavHostController,
    startDestination: String
) {
    val systemUiController = rememberSystemUiController()

    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AppNavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                viewModel = viewModel,
                startDestination = startDestination
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White,
                            Color.White
                        )
                    )
                )
        ) {
            UIMenu.BottomNavigationBar(navController = navController)
        }
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier,
    navController: NavHostController,
    viewModel: MainViewModel,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") { HomeScreen(viewModel) }
        composable("example") { ExampleScreen(viewModel) }
        composable("logs") { LogScreen(viewModel) }
        composable("config") { ConfigScreen(viewModel) }
    }
}




