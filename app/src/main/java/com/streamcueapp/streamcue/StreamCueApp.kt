package com.streamcueapp.streamcue

import android.app.Application
import com.streamcueapp.streamcue.data.StreamCueDatabase

class StreamCueApp : Application() {
    val database by lazy { StreamCueDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        // Nightly background refresh (WorkManager) and Play Billing/Review
        // wiring land alongside the screens that need them — see
        // CLAUDE.md's roadmap for what's still ahead.
    }
}
