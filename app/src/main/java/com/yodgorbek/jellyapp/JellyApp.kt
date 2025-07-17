package com.yodgorbek.jellyapp

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.yodgorbek.jellyapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class JellyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        FirebaseAuth.getInstance() // Initialize Authentication
        // Initialize Koin
        startKoin {
            androidContext(this@JellyApp)
            modules(appModule)
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

    }
}