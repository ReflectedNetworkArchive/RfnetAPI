package network.reflected.rfnetapi.async

class AsyncReturnable<T> {
    private lateinit var callback: (it: T) -> Unit

    fun setReturn(what: T) {
        callback.invoke(what)
    }

    fun then(callback: (it: T) -> Unit) {
        this.callback = callback
    }
}