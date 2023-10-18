package kr.co.makeitall.arduino

object Log {
    private const val TAG = "Arduino"
    private var on = true
    private val info: String
        get() {
            val ste = Throwable().stackTrace
            val realMethod = ste[2]
            return "[" + realMethod.fileName + ":" + realMethod.lineNumber + ":" + realMethod.methodName + "()] "
        }

    @JvmStatic
    fun on() {
        on = true
    }

    @JvmStatic
    fun off() {
        on = false
    }

    @JvmStatic
    fun v(msg: String) {
        if (on) {
            android.util.Log.v(TAG, info + msg)
        }
    }

    @JvmStatic
    fun d(msg: String) {
        if (on) {
            android.util.Log.d(TAG, info + msg)
        }
    }

    @JvmStatic
    fun i(msg: String) {
        if (on) {
            android.util.Log.i(TAG, info + msg)
        }
    }

    @JvmStatic
    fun w(msg: String) {
        if (on) {
            android.util.Log.w(TAG, info + msg)
        }
    }

    @JvmStatic
    fun e(msg: String) {
        if (on) {
            android.util.Log.e(TAG, info + msg)
        }
    }

    @JvmStatic
    fun v(tag: String?, msg: String) {
        if (on) {
            android.util.Log.v(tag, info + msg)
        }
    }

    @JvmStatic
    fun d(tag: String?, msg: String) {
        if (on) {
            android.util.Log.d(tag, info + msg)
        }
    }

    @JvmStatic
    fun i(tag: String?, msg: String) {
        if (on) {
            android.util.Log.i(tag, info + msg)
        }
    }

    @JvmStatic
    fun w(tag: String?, msg: String) {
        if (on) {
            android.util.Log.w(tag, info + msg)
        }
    }

    @JvmStatic
    fun e(tag: String?, msg: String) {
        if (on) {
            android.util.Log.e(tag, info + msg)
        }
    }
}
