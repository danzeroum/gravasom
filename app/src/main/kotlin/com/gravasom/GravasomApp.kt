package com.gravasom

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gravasom.feature.library.LibraryScreen
import com.gravasom.feature.record.RecordScreen
import com.gravasom.feature.settings.NumbersScreen
import com.gravasom.feature.settings.SettingsScreen

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val tag: String)

private val Tabs = listOf(
    Tab(MainActivity.ROUTE_RECORD, R.string.nav_record, Icons.Filled.Mic, "aba_gravar"),
    Tab(MainActivity.ROUTE_LIBRARY, R.string.nav_library, Icons.Filled.LibraryMusic, "aba_gravacoes"),
    Tab(MainActivity.ROUTE_SETTINGS, R.string.nav_settings, Icons.Filled.Settings, "aba_ajustes"),
)

/** Navegacao inferior Gravar / Gravacoes / Ajustes (docs/ux/04-navegacao.md). Abre direto em Gravar. */
@Composable
fun GravasomApp(
    destination: String?,
    onDestinationHandled: () -> Unit,
    autoStart: Boolean,
    onAutoStartHandled: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    LaunchedEffect(destination) {
        if (destination != null) {
            nav.goTo(destination)
            onDestinationHandled()
        }
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tabs.forEach { tab ->
                    val underSettings = tab.route == MainActivity.ROUTE_SETTINGS && current == MainActivity.ROUTE_NUMBERS
                    val selected = current == tab.route || underSettings
                    NavigationBarItem(
                        selected = selected,
                        onClick = { nav.goTo(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        modifier = Modifier.testTag(tab.tag),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = MainActivity.ROUTE_RECORD, modifier = Modifier.padding(padding)) {
            composable(MainActivity.ROUTE_RECORD) { RecordScreen(
                    autoStart = autoStart, onAutoStartHandled = onAutoStartHandled,
                    onOpenLibrary = { nav.goTo(MainActivity.ROUTE_LIBRARY) },
                ) }
            composable(MainActivity.ROUTE_LIBRARY) { LibraryScreen() }
            composable(MainActivity.ROUTE_SETTINGS) { SettingsScreen(onOpenNumbers = { nav.navigate(MainActivity.ROUTE_NUMBERS) }) }
            composable(MainActivity.ROUTE_NUMBERS) { NumbersScreen() }
        }
    }
}

private fun NavHostController.goTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
