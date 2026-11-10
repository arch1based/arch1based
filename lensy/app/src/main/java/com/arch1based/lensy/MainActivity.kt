package com.arch1based.lensy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arch1based.lensy.reminders.Reminders
import com.arch1based.lensy.ui.LensyApp
import com.arch1based.lensy.ui.LensyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Reminders.ensureChannel(this)
        setContent { LensyTheme { LensyApp() } }
    }
}
