package com.mouradsoft.translator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mouradsoft.translator.session.Screen
import com.mouradsoft.translator.session.SessionViewModel

@Composable
fun TranslatorApp(viewModel: SessionViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigation = rememberNavController()
    // The session owns navigation. The host never keeps a historical result destination.
    // Restored framework destinations cannot render content unless they match live memory state.
    LaunchedEffect(state.screen) {
        navigation.navigate(state.screen.name) {
            popUpTo(navigation.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    BackHandler(state.screen != Screen.Welcome) { viewModel.back() }
    MouradsoftTheme {
        NavHost(navigation, startDestination = Screen.Welcome.name,
            enterTransition = { fadeIn(tween(160)) }, exitTransition = { fadeOut(tween(100)) }) {
            composable(Screen.Welcome.name) { if (state.screen == Screen.Welcome) WelcomeScreen(viewModel::start) }
            composable(Screen.Choose.name) { if (state.screen == Screen.Choose) ChooseScreen(state, viewModel::choose, viewModel::continueToInput, viewModel::back) }
            composable(Screen.Input.name) { if (state.screen == Screen.Input) InputScreen(state, viewModel) }
            composable(Screen.Completion.name) { if (state.screen == Screen.Completion) CompletionScreen(viewModel.isDemo, viewModel::finish, viewModel::back) }
            composable(Screen.Result.name) { if (state.screen == Screen.Result) ResultScreen(state, viewModel.isDemo, viewModel::redo, viewModel::back) }
        }
    }
}
