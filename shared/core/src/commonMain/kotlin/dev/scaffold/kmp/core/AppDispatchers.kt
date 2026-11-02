package dev.scaffold.kmp.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface AppDispatchers {
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultAppDispatchers(
    override val default: CoroutineDispatcher = Dispatchers.Default,
    override val main: CoroutineDispatcher = Dispatchers.Main,
) : AppDispatchers
