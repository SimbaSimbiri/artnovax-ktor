package com.simbiri.domain.util

sealed interface ResultType<out D, out E: Error>{
    data class Success<out D>(val data: D): ResultType<D, Nothing>
    data class Failure<out E: Error>(val error: E): ResultType<Nothing, E>
}

/**
 * We expose a lambda in each case, where the out param Error or Data will be acted upon appropriately
 * e.g. using presentation.config.utils.respondWithDataError(error) for onFailure.
 *
 * We inline these functions since they will be used repetitively, and to avoid overhead costs.
**/

inline fun <D, E: Error> ResultType<D, E>.onFailure(action: (E) -> Unit): ResultType<D, E> {
    if(this is ResultType.Failure) action(error)
    return this
}

inline fun <D, E: Error> ResultType<D,E>.onSuccess(action: (D) -> Unit): ResultType<D, E> {
    if(this is ResultType.Success) action(data)
    return this
}