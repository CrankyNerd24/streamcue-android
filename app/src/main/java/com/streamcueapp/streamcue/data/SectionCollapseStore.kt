package com.streamcueapp.streamcue.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Collapse state for the Shows and Movies tabs' list sections — two
 * mechanisms, matching iOS exactly:
 * - Titles of collapsed sections, stored by exclusion so every section
 *   starts out expanded — the analog of iOS's
 *   @AppStorage("collapsedShowSections").
 * - A separate default-collapsed flag for Movies' Watched section — the
 *   analog of iOS's @AppStorage("watchedExpanded"), which defaults false
 *   rather than being stored by exclusion.
 * - A separate default-expanded flag for Shows' Ready to watch section —
 *   the analog of iOS's @AppStorage("readyExpanded"), which defaults true.
 */
class SectionCollapseStore(private val context: Context) {
    private val collapsedKey = stringSetPreferencesKey("collapsed_sections")
    private val watchedExpandedKey = booleanPreferencesKey("movies_watched_expanded")
    private val readyExpandedKey = booleanPreferencesKey("shows_ready_expanded")

    val collapsed: Flow<Set<String>> = context.appDataStore.data
        .map { prefs -> prefs[collapsedKey] ?: emptySet() }

    suspend fun setCollapsed(title: String, collapsed: Boolean) {
        context.appDataStore.edit { prefs ->
            val current = prefs[collapsedKey] ?: emptySet()
            prefs[collapsedKey] = if (collapsed) current + title else current - title
        }
    }

    val watchedExpanded: Flow<Boolean> = context.appDataStore.data
        .map { prefs -> prefs[watchedExpandedKey] ?: false }

    suspend fun setWatchedExpanded(expanded: Boolean) {
        context.appDataStore.edit { prefs -> prefs[watchedExpandedKey] = expanded }
    }

    val readyExpanded: Flow<Boolean> = context.appDataStore.data
        .map { prefs -> prefs[readyExpandedKey] ?: true }

    suspend fun setReadyExpanded(expanded: Boolean) {
        context.appDataStore.edit { prefs -> prefs[readyExpandedKey] = expanded }
    }
}
