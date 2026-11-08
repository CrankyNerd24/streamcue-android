package com.streamcueapp.streamcue

import android.app.Application
import com.streamcueapp.streamcue.data.StreamCueDatabase
import com.streamcueapp.streamcue.data.SubscriptionsStore
import com.streamcueapp.streamcue.work.NightlyRefreshWorker

class StreamCueApp : Application() {
    val database by lazy { StreamCueDatabase.get(this) }
    val subscriptionsStore by lazy { SubscriptionsStore(this) }

    override fun onCreate() {
        super.onCreate()
        NightlyRefreshWorker.schedule(this)
        // Play Billing/Review wiring lands alongside the screens that need
        // it — see CLAUDE.md's roadmap for what's still ahead.
    }
}
