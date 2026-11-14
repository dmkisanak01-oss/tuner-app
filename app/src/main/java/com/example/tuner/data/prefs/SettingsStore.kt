package com.example.tuner.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PrivilegeMode { NONE, SHIZUKU, ROOT }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val keyMode = stringPreferencesKey("privilege_mode")

    val privilegeMode: Flow<PrivilegeMode> = context.dataStore.data.map { prefs ->
        prefs[keyMode]?.let { runCatching { PrivilegeMode.valueOf(it) }.getOrNull() } ?: PrivilegeMode.NONE
    }

    suspend fun setPrivilegeMode(mode: PrivilegeMode) {
        context.dataStore.edit { it[keyMode] = mode.name }
    }
}
