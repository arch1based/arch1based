package com.arch1based.lensy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arch1based.lensy.billing.Billing
import com.arch1based.lensy.data.Prefs
import com.arch1based.lensy.data.Store
import com.arch1based.lensy.reminders.Reminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class LensyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    /** null until the first read from disk completes. */
    val prefs: StateFlow<Prefs?> = store.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val billing = Billing(app) { viewModelScope.launch { store.setPurchased(true) } }

    init {
        viewModelScope.launch { store.ensureInstallDate() }
        // Any change (new lenses, new time, purchase) re-arms the alarm.
        viewModelScope.launch { store.prefs.collect { Reminders.schedule(app, it) } }
        billing.connect()
    }

    fun setPlan(start: LocalDate, intervalDays: Int) {
        viewModelScope.launch { store.setPlan(start, intervalDays) }
    }

    fun setRemindAt(time: LocalTime) {
        viewModelScope.launch { store.setRemindAt(time) }
    }
}
