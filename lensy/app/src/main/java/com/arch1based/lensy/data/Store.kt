package com.arch1based.lensy.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arch1based.lensy.core.LensPlan
import com.arch1based.lensy.core.trialDaysLeft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

private val Context.dataStore by preferencesDataStore("lensy")

data class Prefs(
    val plan: LensPlan?,
    val remindAt: LocalTime,
    val install: LocalDate,
    val purchased: Boolean,
) {
    fun trialLeft(today: LocalDate): Long = trialDaysLeft(install, today)

    /** Reminders work while the trial runs or after the one-time purchase. */
    fun unlocked(today: LocalDate): Boolean = purchased || trialLeft(today) > 0
}

class Store(context: Context) {
    private val ds = context.applicationContext.dataStore

    val prefs: Flow<Prefs> = ds.data.map { it.toPrefs() }

    suspend fun ensureInstallDate() {
        ds.edit { if (it[INSTALL] == null) it[INSTALL] = LocalDate.now().toEpochDay() }
    }

    suspend fun setPlan(start: LocalDate, intervalDays: Int) {
        ds.edit {
            it[START] = start.toEpochDay()
            it[INTERVAL] = intervalDays
        }
    }

    suspend fun setRemindAt(time: LocalTime) {
        ds.edit { it[REMIND_MINUTES] = time.hour * 60 + time.minute }
    }

    suspend fun setPurchased(value: Boolean) {
        ds.edit { it[PURCHASED] = value }
    }

    private fun Preferences.toPrefs(): Prefs {
        val start = this[START]
        val interval = this[INTERVAL]
        val minutes = this[REMIND_MINUTES] ?: (9 * 60)
        return Prefs(
            plan = if (start != null && interval != null) LensPlan(LocalDate.ofEpochDay(start), interval) else null,
            remindAt = LocalTime.of(minutes / 60, minutes % 60),
            install = this[INSTALL]?.let(LocalDate::ofEpochDay) ?: LocalDate.now(),
            purchased = this[PURCHASED] ?: false,
        )
    }

    private companion object {
        val START = longPreferencesKey("start_epoch_day")
        val INTERVAL = intPreferencesKey("interval_days")
        val REMIND_MINUTES = intPreferencesKey("remind_minutes")
        val INSTALL = longPreferencesKey("install_epoch_day")
        val PURCHASED = booleanPreferencesKey("purchased")
    }
}
