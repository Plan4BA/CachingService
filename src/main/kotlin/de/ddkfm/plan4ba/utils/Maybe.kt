package de.ddkfm.plan4ba.utils

import de.ddkfm.plan4ba.models.HttpStatus

data class Maybe<T>(
    val maybe : T?,
    val error : HttpStatus?
) {
    companion object {
        fun <T> of(maybe : T) : Maybe<T> {
            return Maybe(maybe, null)
        }
        fun <T> ofError(error : HttpStatus) : Maybe<T> {
            return Maybe(null, error)
        }
    }
    fun getOrThrow() : T {
        return maybe ?: throw (error?.asException() ?: NullPointerException())
    }
}