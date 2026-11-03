package dev.scaffold.kmp.android.feature.reference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.scaffold.kmp.feature.reference.ReferenceEffect

private const val ROUTE_LIST = "reference/list"
private const val ROUTE_DETAIL = "reference/detail/{itemId}"
private const val ARG_ITEM_ID = "itemId"

@Composable
fun ReferenceNavHost(
    modifier: Modifier = Modifier,
    viewModel: ReferenceViewModel = viewModel(factory = ReferenceViewModel.Factory),
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReferenceEffect.NavigateToDetail -> {
                    navController.navigate("reference/detail/${effect.id.value}")
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST,
        modifier = modifier,
    ) {
        composable(ROUTE_LIST) {
            ReferenceScreen(
                state = state,
                onAction = viewModel::onAction,
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_ITEM_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString(ARG_ITEM_ID).orEmpty()
            ReferenceDetailScreen(
                itemId = itemId,
                state = state,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
