package fr.outadoc.homeslide.app.onboarding.model

sealed class CallStatus<out T> {
    class Done<T>(val value: Result<T>) : CallStatus<T>()
    object Loading : CallStatus<Nothing>()
}
