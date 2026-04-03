package com.luigivampa92.ndeftagemulator

import android.app.Application
import com.luigivampa92.ndeftagemulator.data.AppDatabase

class NdefTagEmulatorApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
