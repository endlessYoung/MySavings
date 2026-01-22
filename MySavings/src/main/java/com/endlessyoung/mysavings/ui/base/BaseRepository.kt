package com.endlessyoung.mysavings.ui.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseRepository<T, E> {
    protected suspend fun <R> withIO(block: suspend () -> R): R {
        return withContext(Dispatchers.IO) {
            block()
        }
    }


}