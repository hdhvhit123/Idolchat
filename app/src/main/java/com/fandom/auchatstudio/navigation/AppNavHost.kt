package com.fandom.auchatstudio.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fandom.auchatstudio.ui.characters.CharactersScreen
import com.fandom.auchatstudio.ui.home.HomeScreen
import com.fandom.auchatstudio.ui.player.PlayerScreen
import com.fandom.auchatstudio.ui.studio.StudioScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object CharactersRoute

@Serializable
data class StudioRoute(val storyId: Long)

@Serializable
data class PlayerRoute(val storyId: Long)

private const val NAV_ANIM_MS = 280

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = HomeRoute,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(NAV_ANIM_MS)) + fadeIn(tween(NAV_ANIM_MS))
        },
        exitTransition = { fadeOut(tween(NAV_ANIM_MS)) },
        popEnterTransition = { fadeIn(tween(NAV_ANIM_MS)) },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(NAV_ANIM_MS)) + fadeOut(tween(NAV_ANIM_MS))
        },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenStory = { nav.navigate(StudioRoute(it)) },
                onPlayStory = { nav.navigate(PlayerRoute(it)) },
                onOpenCharacters = { nav.navigate(CharactersRoute) },
            )
        }
        composable<CharactersRoute> {
            CharactersScreen(onBack = { nav.popBackStack() })
        }
        composable<StudioRoute> { entry ->
            val route = entry.toRoute<StudioRoute>()
            StudioScreen(
                storyId = route.storyId,
                onBack = { nav.popBackStack() },
                onPlay = { nav.navigate(PlayerRoute(route.storyId)) },
                onOpenCharacters = { nav.navigate(CharactersRoute) },
            )
        }
        composable<PlayerRoute> { entry ->
            val route = entry.toRoute<PlayerRoute>()
            PlayerScreen(storyId = route.storyId, onBack = { nav.popBackStack() })
        }
    }
}
