package com.chy.radiofrance.core.model

/**
 * The outcome of a data call. Like `kotlin.Result`, but the failure is a typed
 * [DataError] instead of a Throwable, so `when` is exhaustive with no cast.
 */
sealed interface DataResult<out T> {
    data class Success<out T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}
