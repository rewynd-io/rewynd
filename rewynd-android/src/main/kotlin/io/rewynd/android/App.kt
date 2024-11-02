package io.rewynd.android

import android.app.Application
import android.content.Context
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        initAcra {
            reportFormat = StringFormat.JSON

            dialog {
                text = getString(R.string.acra_notification_text)
            }

            mailSender {
                mailTo = "reports@rewynd.io"
                reportFileName = "Rewynd-Android-Crash.json"
                reportAsFile = false
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        sApplication = this
    }

    companion object {
        private lateinit var sApplication: Application
        val application: Application
            get() = sApplication
        val context: Context
            get() = application.applicationContext
    }
}
