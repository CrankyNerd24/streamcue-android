package com.streamcueapp.streamcue

import android.app.Application
import com.streamcueapp.streamcue.data.StreamCueDatabase
import com.streamcueapp.streamcue.data.SubscriptionsStore

class StreamCueApp : Application() {
    val database by lazy { StreamCueDatabase.get(this) }
    val subscriptionsStore by lazy { SubscriptionsStore(this) }

    override fun onCreate() {
        super.onCreate()
        // Nightly background refresh (WorkManager) and Play Billing/Review
        // wiring land alongside the screens that need them — see
        // CLAUDE.md's roadmap for what's still ahead.
    }
}
