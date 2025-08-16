package com.gaurav.shaadisaathi

import android.app.Application
import com.google.firebase.FirebaseApp

class ShaadiSaathiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
