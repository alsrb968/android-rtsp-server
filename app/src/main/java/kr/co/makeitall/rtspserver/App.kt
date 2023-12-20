package kr.co.makeitall.rtspserver

import android.app.Application
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberTree(getString(R.string.app_name)))
        }
    }

    private inner class TimberTree(private val tag: String) : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "[${element.fileName}:${element.lineNumber}#${element.methodName}()]"
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            super.log(priority, this.tag, "$tag $message", t)
        }
    }
}