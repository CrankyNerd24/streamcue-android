package com.streamcueapp.streamcue.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single shared Preferences DataStore file. DataStore enforces one
 * instance per file, so every preferences-backed store in the app reads
 * this instead of declaring its own `preferencesDataStore` delegate. */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
