package com.reflectednetwork.rfnetapi.async

class AsyncReturnable<T> {
    private lateinit var callback: (it: T) -> Unit

    fun setReturn(what: T) {
        if (this::callback.isInitialized) {
            callback.invoke(what)
        }
    }

    fun then(callback: (it: T) -> Unit) {
        this.callback = callback
    }
}