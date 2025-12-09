package com.simbiri.domain.util

sealed interface DataError : Error {
    data object NotFound : DataError
    data object DatabaseError : DataError
    data object ValidationError : DataError
    data object UnknownError : DataError
}