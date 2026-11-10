package com.streamcueapp.streamcue

import android.app.Application
import com.streamcueapp.streamcue.data.SectionCollapseStore
import com.streamcueapp.streamcue.data.StreamCueDatabase
import com.streamcueapp.streamcue.data.SubscriptionsStore
import com.streamcueapp.streamcue.notifications.BadgeNotifier
import com.streamcueapp.streamcue.notifications.NotificationChannels
import com.streamcueapp.streamcue.notifications.NotificationScheduler
import com.streamcueapp.streamcue.work.NightlyRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StreamCueApp : Application() {
    val database by lazy { StreamCueDatabase.get(this) }
    val subscriptionsStore by lazy { SubscriptionsStore(this) }
    val sectionCollapseStore by lazy { SectionCollapseStore(this) }

    // Outlives any single screen — these two reactive jobs own the app's
    // notifications for as long as the process is alive, including when a
    // background Worker (not an Activity) is what woke it up.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        NightlyRefreshWorker.schedule(this)

        // The Android analog of iOS's app-icon badge — see BadgeNotifier.
        // Re-posted on every change to the outstanding-episode count,
        // whatever caused it (a sync, a mark-watched, an undo).
        appScope.launch {
            database.pendingEpisodeDao().observePending().collect { pending ->
                BadgeNotifier.update(this@StreamCueApp, pending.size)
            }
        }
        // Per-show "airs today" alerts — see NotificationScheduler. Rebuilt
        // from scratch whenever a tracked show changes (added, removed,
        // refreshed onto a new date), same "clear and rebuild" strategy as
        // iOS's own Notifications.reschedule.
        appScope.launch {
            database.trackedShowDao().observeAll().collect { shows ->
                NotificationScheduler.reschedule(this@StreamCueApp, shows)
            }
        }
        // Play Billing/Review wiring lands alongside the screens that need
        // it — see CLAUDE.md's roadmap for what's still ahead.
    }
}
