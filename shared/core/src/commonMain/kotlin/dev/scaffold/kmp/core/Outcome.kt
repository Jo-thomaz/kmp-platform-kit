package dev.scaffold.kmp.core

sealed class Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>()
    data class Failure(val error: DomainError) : Outcome<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun errorOrNull(): DomainError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    inline fun <R> map(transform: (T) -> R): Outcome<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    inline fun <R> flatMap(transform: (T) -> Outcome<R>): Outcome<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (DomainError) -> R,
    ): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }

    companion object {
        fun <T> success(value: T): Outcome<T> = Success(value)

        fun failure(error: DomainError): Outcome<Nothing> = Failure(error)
    }
}
