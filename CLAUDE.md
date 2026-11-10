# StreamCue for Android — project brief

The Android version of **StreamCue**, the TV/film tracker that shows what's
airing, where it streams, and whether it's free. The iPhone app lives in the
private repo `CrankyNerd24/streamcue`; its `CLAUDE.md` is the source of truth
for the product: the tabs, the TMDB/OMDb data model, the colour rules, the
Premium gating, and the review-prompt cadence. Match it, adapted to Android.
Where this file and that one differ, this one wins for Android.

## Locked-in decisions

- **Package / application ID: `com.streamcueapp.streamcue`.** Permanent once
  this app is published to Google Play — never change it after that.
- **Version 1 is local-only.** No household sharing, no cross-device sync.
  iOS's household list rides CloudKit/`CKShare`, which has no drop-in Android
  equivalent; building one is a real backend project, not a port task, so
  it's deferred rather than half-built. Every show/movie lives in this
  device's Room database only. Revisit if iOS's sharing feature turns out to
  matter enough to justify a backend.

## Tech

- Kotlin, Jetpack Compose (Material 3), single `app` module. AGP 9 with
  built-in Kotlin, Gradle wrapper, version catalog in `gradle/libs.versions.toml`.
- `minSdk` 31 (Android 12), `targetSdk` 36, `compileSdk` 37 (the newest Compose
  libraries need it).
- Room (SQLite) is the Android analog to iOS's SwiftData — `StreamCueDatabase`,
  one `@Entity` per tracked title. No `@Attribute(.unique)`-style uniqueness
  at the schema level; dedup happens in the DAO's insert path instead, same
  reasoning as iOS's `Library.addShow`.
- Retrofit + kotlinx.serialization (`retrofit2-kotlinx-serialization-converter`)
  over OkHttp for TMDB and OMDb. **Never use automatic snake_case key
  conversion** — the iOS app shipped a real bug where that lowercased/mangled
  the watch-providers response's country keys. Every model uses explicit
  `@SerialName` per field instead, same as iOS's explicit `CodingKeys`.
- WorkManager is the Android analog to iOS's `BGTaskScheduler` for the nightly
  refresh — `NightlyRefreshWorker` walks every tracked show/movie through
  `data/Refresh.kt` once a day, scheduled from `StreamCueApp.onCreate`.
- DataStore (Preferences) is the Android analog to iOS's `@AppStorage`.
- Coil 3 for poster images (Compose-native `AsyncImage`).
- Local secrets: `local.properties` (gitignored) feeds `TMDB_TOKEN` and
  `OMDB_KEY` into `BuildConfig` fields — see `local.properties.example`,
  mirroring the iOS app's gitignored `Secrets.swift` / `Secrets.example.swift`.
- Google Play Billing (`billing-ktx` 8.3.0) is the analog to StoreKit 2 for the
  Premium one-time unlock — same dependency Nabbit for Android's TipJar
  already proved out. No grandfathering concept carries over: that's an iOS
  pricing-history fix with no Android equivalent to port.
- Google Play In-App Review (`review-ktx` 2.0.2) is the analog to iOS's
  `requestReview`. Reuse Nabbit for Android's exact cadence as the Android
  precedent (not iOS's own numbers): 3rd qualifying occurrence, then every
  30th, at least 120 days apart.
- Android has no API for an app-icon badge detached from a real
  notification, unlike iOS's `UNUserNotificationCenter.setBadgeCount` — a
  badge count here has to ride an actual low-importance notification (see
  `notifications/BadgeNotifier.kt`). Per-show air-date alerts use
  WorkManager one-time requests rather than `AlarmManager`'s exact alarms:
  `SCHEDULE_EXACT_ALARM` is reserved for apps whose core purpose is
  alarms/calendaring, and Play is strict about granting it elsewhere.
  `POST_NOTIFICATIONS` is requested once at launch (`MainActivity`,
  Android 13+ only) — both notifiers silently no-op without it, same as
  iOS "without permission, silently ignores the badge."

## Layout (`app/src/main/java/com/streamcueapp/streamcue`)

- `model/`: `TrackedShow.kt` and `TrackedMovie.kt` — field-for-field Room
  ports of iOS's `TrackedShow.swift` / `TrackedMovie.swift`. `IgnoredTitle.kt`
  is a field-for-field port of iOS's `IgnoredTitle.swift` ("Not interested"
  records) — `kind` is a `DiscoverKind` name string rather than iOS's
  `MediaKind` raw value, and is part of the primary key since TV and movie
  id spaces can collide. `PendingEpisode.kt` is a field-for-field port of
  iOS's `PendingEpisode.swift` — an aired, not-yet-actioned episode; `key`
  ("showId-season-episode") is the primary key directly rather than a
  separate id plus a uniqueness check, since Room enforces it for real and
  there's no CloudKit-style multi-device merge to design around. `Date`
  fields are
  stored as epoch `Long`s (`nextAirDateEpochDay`, `addedAtEpochMillis`,
  `lastRefreshedEpochMillis`, …). Computed-only properties from iOS are
  Kotlin computed properties here, same as on iOS: `effectiveAirDateEpochDay`,
  `scheduleSummary`, and `nextEpisodeSeasonAndNumber` (`TrackedShow`),
  `subtitle` (`TrackedMovie`),
  `headlineRating` and `isStale`/`staleAfterMillis` (both), `label` and
  `airedSummary` (`PendingEpisode`). **Never read
  `nextAirDateEpochDay` directly outside `TrackedShow.kt`** — same rule as
  iOS's "never read `nextAirDate` outside `TrackedShow.swift`"; everything
  else reads `effectiveAirDateEpochDay`, which applies `dayOffset`.
- `data/`: `StreamCueDatabase` (Room singleton, four entities so far),
  `TrackedShowDao` / `TrackedMovieDao` (`observeAll`, `observe(tmdbId)`,
  `find`, `insert` with `OnConflictStrategy.IGNORE`, `update`, `delete`),
  `IgnoredTitleDao` (`observeAll`, `insert` — see `model/IgnoredTitle.kt`
  below), `PendingEpisodeDao` (`observePending` — outstanding only, grouped
  by show; `observeAll` — every record, for the actioned-key check;
  `observeForShow`/`forShow`, `find`, `insert` with
  `OnConflictStrategy.IGNORE`, `update`, `deleteAllForShow`,
  `deleteOlderThan`), `Converters` (List<String>/List<Int> ↔ JSON via kotlinx.serialization),
  `AppPreferences.kt` (the one shared `Context.appDataStore` — Preferences
  DataStore enforces a single instance per file, so `SubscriptionsStore`
  and `SectionCollapseStore` both read this rather than declaring their
  own), `SectionCollapseStore.kt` (Shows/Movies list-section collapse
  state, plus `watchedExpanded`/`readyExpanded` — see the `ui/` entry
  below), `Refresh.kt` (TMDB-only refresh for
  one show/movie, ported from
  `TrackedShow.refresh()`/`TrackedMovie.refresh()` on iOS — shared by the
  detail screens' refresh-if-stale and `NightlyRefreshWorker`; OMDb ratings
  aren't ported, so `imdbScore`/`rottenTomatoesScore`/`metacriticScore` stay
  unset until that lands), `EpisodeSync.kt` (ported from iOS's
  `EpisodeSync` — `sync(episodeDao, showDao, show)` fetches the show's
  current season and records anything aired in the last 30 days as a
  `PendingEpisode`, un-marking the show watched if that leaves something
  outstanding; `prune(episodeDao)` drops anything aired more than 60 days
  ago. Always called right after `Refresh.show` — see the `ui/` entry
  below and `NightlyRefreshWorker` — never on its own). The database uses `fallbackToDestructiveMigration`
  — fine pre-release, revisit once there's an installed base worth
  preserving across a schema change.
- `network/`: `TmdbModels.kt` (`@Serializable` response models, explicit
  `@SerialName` — search, detail, and watch-provider shapes per media type;
  note the detail endpoints return a `genres: [{id,name}]` array, not the
  `genre_ids` the search/discover endpoints use, a real shape difference,
  not just naming), `Availability.kt` (narrows a watch-providers response to
  one region, ported from iOS's `TMDBClient.providers(path:region:)`),
  `TmdbApi.kt` (Retrofit interface), `TmdbClient.kt` (singleton
  OkHttp/Retrofit client, Bearer-token interceptor reading
  `BuildConfig.TMDB_TOKEN`, conditional logging interceptor in debug),
  `TmdbImage.kt` (poster URL helper, same `image.tmdb.org/t/p/wNNN` scheme as
  iOS's `TMDB.swift` image helpers).
- `ui/theme/Theme.kt`: dark-only Compose `MaterialTheme` (matches iOS's
  `.preferredColorScheme(.dark)`) built from named brand-colour constants
  matching the marketing site and iOS app — see `colors.xml`.
- `ui/`: `RootScaffold` — the four-tab shell (Shows / Movies / Services /
  Discover, matching iOS `RootView`'s `TabView`) that `MainActivity` sets as
  its content, built on `navigation-compose`. Shows and Movies are real
  vertical slices: `HomeScreen`/`HomeViewModel` and `MoviesScreen`/
  `MoviesViewModel` group their list into collapsible `SectionHeader`
  groups, the Android analog of iOS's `ContentView`/`WatchlistView`
  section headers — Shows into Airing today (accented) / Ready to watch
  (amber, actionable) / Airing next / No date announced / Finished
  (`groupShows`, mirroring `ContentView`'s
  `dated`/`airingToday`/`upcoming`/`waiting`/`finished`, Ready-to-watch
  exclusion included — a show already fully represented there drops out of
  "dated" so it isn't double-listed); Movies into Free to watch (accented)
  / On a subscription / Rent or buy / Not streaming yet from the unwatched
  list (`groupMovies`, mirroring `WatchlistView`'s `group(_:)` over
  `MovieAvailability.allCases`), then Watched last. Every section defaults
  expanded and persists collapse state by exclusion
  (`SectionCollapseStore.collapsed`), except Watched, which defaults
  collapsed via its own flag (`watchedExpanded`) — iOS uses the same two
  mechanisms (`@AppStorage("collapsedShowSections")` vs.
  `@AppStorage("watchedExpanded")`), not one. Ready to watch has its own
  default-expanded flag too (`readyExpanded`, iOS's
  `@AppStorage("readyExpanded")`). Pulling to refresh on the Shows tab
  (`HomeViewModel.refreshAll`, Material3 `PullToRefreshBox`) re-fetches
  every show and runs `EpisodeSync.sync` on each, the Android analog of
  iOS's `ContentView.refreshAll()`. No Watch Now shortcut on Ready to watch
  cards yet — that rides Premium/`StreamingServices`, not wired here. Each
  row is tappable through to its detail screen, each with its own Add
  flow pushed on top of the tab graph (`AddShowScreen`/`AddShowViewModel`,
  `AddMovieScreen`/`AddMovieViewModel` — debounced TMDB search, same shape
  as iOS's LookupView). Each search result row has two separate affordances,
  same split as Discover's tiles: tapping the row opens `TitlePreviewScreen`
  to see detail before committing, while the "+" icon button on the row
  still adds directly without leaving the search screen. Each tracked title
  also has its own detail screen (`ShowDetailScreen`/
  `ShowDetailViewModel`, `MovieDetailScreen`/`MovieDetailViewModel` —
  poster, schedule/subtitle, rating, a Watched switch, where-to-watch chips
  with a Watch now button that opens the stored JustWatch link, and a
  delete action that also clears the show's recorded episodes; refreshes
  via `data/Refresh.kt` if the row is stale on open, then runs
  `EpisodeSync.sync`). The show detail screen's "Episodes" section is the
  real recorded history — `PendingEpisode` rows for this show, newest
  first, each with a Watched/Undo action (`EpisodeStatusRow`, mirroring
  iOS's own) — not a live TMDB fetch of the whole season, so (like iOS) it
  never previews unaired future episodes. No Not-interested action on
  these screens — "Not interested" only makes sense for a title you
  haven't tracked yet (see `TitlePreviewScreen` below); once it's tracked,
  delete is the equivalent. Trimmed vs. iOS's
  `ShowDetailView`/equivalent: no household
  sharing, no Reminders integration, no cast, no deep-linking into an
  installed streaming app's own UI (iOS's `StreamingServices` scheme
  matching) — Watch now always opens the JustWatch link in a browser.
  Services is a real slice now: `ServicesScreen`/`ServicesViewModel` show
  the picked services from `SubscriptionsStore` (DataStore-backed, the
  Android analog of iOS's `@AppStorage(Subscriptions.key)`), each with a
  "popular on your services" strip per media type — ported from iOS's
  `SubscriptionsView.loadPicks`, narrowed to the top 3 genres across
  tracked shows/movies via `TmdbApi.discoverShows`/`discoverMovies`.
  `ServicePickerScreen`/`ServicePickerViewModel` pick from TMDB's
  `/watch/providers/tv` list. No free-tier cap on selections since
  Premium/Billing doesn't exist on Android at all yet — add one alongside
  that work, not before it. Region is hardcoded to `"US"` throughout
  `TmdbApi` for now; iOS reads `AppSettings.region`, which Android has no
  equivalent setting for yet. `DiscoverScreen`/`DiscoverViewModel` cover
  For you / Trending / Popular (Shows and Movies each) — For you is
  recommendation-based, not genre-based: up to 8 tracked titles as seeds,
  TMDB's own `/recommendations` for each fetched concurrently, tallied by
  how many seeds recommend a title, ported from iOS's
  `DiscoverView.recommendedShows()`/`recommendedMovies()`. Tapping a
  poster never adds anything, same rule as iOS — it opens
  `TitlePreviewScreen`/`TitlePreviewViewModel`, the Android analog of
  iOS's `TitlePreviewSheet`: fetches straight from TMDB (not Room, since
  the title usually isn't tracked yet) and shows an Add button or a
  "Tracked" state, plus Watched and Not interested buttons (hidden once
  tracked, same as iOS's `if !preview.isTracked`) — Watched is one insert
  with `watched = true` rather than add-then-update (iOS's `markWatched()`
  does the same, reusing its own `onAdd`), and Not interested records an
  `IgnoredTitle` via `IgnoredTitleDao.insert`. All three actions navigate
  back afterward, matching iOS dismissing the sheet after any of them. The
  grid tile's own add icon is a separate, explicit affordance that still
  adds directly without opening the preview, same pattern as the Add
  Show/Movie rows; the Services suggestion strips reuse the same preview
  screen. Both already-tracked and "Not interested" titles are filtered
  out of Discover's grid and the Services suggestion strips —
  `DiscoverScreen`'s `visibleItems` and `ServicesScreen`'s per-service
  `shows`/`movies` both subtract `trackedShowIds`/`trackedMovieIds` *and*
  `ignoredShowIds`/`ignoredMovieIds`, same as iOS's `visibleShows`/
  `visibleMovies` filtering out `owned` and `hidden`. An item disappears
  from these screens the moment it's added or marked not interested,
  reactively, not just on next fetch. `ProviderChip.kt` is a small shared
  composable used by both detail screens and the preview screen, rather
  than three copies of the same chip.
  `ShowDetailScreen` also has a day-offset control (stepper + tap-to-edit
  dialog, −7 to 365 days, same range as iOS) and a real, persisted
  "Episodes" list — `PendingEpisode` rows recorded for this show by
  `EpisodeSync`, newest first, each with a Watched/Undo action
  (`EpisodeStatusRow`, mirroring iOS's own). Not a live fetch of the whole
  current season, so — like iOS's actual "Recent episodes" — it never
  previews an episode that hasn't aired yet. Toggling an episode re-derives
  the show's `watched` flag the same way the Shows tab's Ready to watch
  section does: caught up once nothing recorded for the show is still
  outstanding (`ShowDetailViewModel.setEpisodeWatched`, mirroring iOS's
  `ShowDetailView.setWatched`). Both detail screens are laid out as `DetailSection.kt`
  groups — a shared titled/collapsible composable standing in for iOS's
  grouped `List`/`Section` style. Episode synopsis is folded into the
  Schedule section's content rather than getting its own nested disclosure
  (iOS nests a `DisclosureGroup` inside that section; one level of
  collapse reads cleaner on a phone than two). Episodes defaults collapsed,
  matching iOS's "Recent episodes"; Overview/Ratings/Where to watch default
  open, matching iOS always showing them (Android just makes them
  collapsible too, since the screen reads better shorter). `ScoreBadge.kt`
  and `relativeTime()` (`RelativeTime.kt`) are the other two small shared
  pieces between the two detail screens.
- `work/NightlyRefreshWorker.kt`: a `CoroutineWorker` that refreshes every
  tracked show and movie once a day via `data/Refresh.kt`, one `runCatching`
  per title so a single network failure doesn't skip the rest of the batch
  — then runs `EpisodeSync.sync` on each refreshed show and
  `EpisodeSync.prune` once at the end. `work/EpisodeAlertWorker.kt` is the
  one-shot counterpart that fires a single show's air-date alert — see
  `notifications/` below; both are the default WorkManager initializer's
  job to discover, no `Configuration.Provider` needed.
- `notifications/`: the app's notification layer, ported conceptually from
  iOS's `Notifications.swift` (badge + scheduled alerts) but built on
  Android's own primitives throughout — see the Tech section above for
  why. `NotificationChannels.kt` (two channels: low-importance
  `ready_to_watch`, default-importance `episode_alerts`, created once in
  `StreamCueApp.onCreate`). `BadgeNotifier.kt` (the app-icon-badge analog —
  `update(context, outstandingCount)`, posts/updates/cancels one
  notification). `EpisodeAlertNotifier.kt` (`post(context, show)` — one
  visible "airs today" alert, fired by `EpisodeAlertWorker`).
  `NotificationScheduler.kt` (`reschedule(context, shows)` — cancels every
  pending `episode_alert`-tagged work request and re-enqueues one per show
  with a future `effectiveAirDateEpochDay`, fixed at 6pm local; "clear and
  rebuild" rather than diffed, same as iOS). `NotificationPermission.kt`
  (`hasNotificationPermission` — true outright below API 33). Both
  `BadgeNotifier.update` and `NotificationScheduler.reschedule` are driven
  reactively from two long-lived `StreamCueApp`-scoped coroutines watching
  `pendingEpisodeDao().observePending()` and `trackedShowDao().observeAll()`
  — nothing elsewhere in the app calls them directly, so there's no call
  site to remember to add when something new changes either table.
- `billing/`: not yet populated — Play Billing (Premium unlock) lands here
  alongside the screen that needs it.

## Colour rules (same as iOS)

Colour means something — don't repurpose these. Orange (`StreamCueAccent`) is
only ever "airing today". Green (`StreamCueFree`) is only ever "free to
watch"/"watched". Amber (`StreamCueUnwatched`) is only ever "aired, not
actioned" — the Ready to watch section and episode rows. Don't reuse any of
these for anything else.

## Version 1 scope

Done: the four-tab `NavHost` shell is fully real now — Shows and Movies
backed by Room, each grouped into collapsible sections matching iOS's own
grouping, with Add Show / Add Movie TMDB search flows and detail screens
(the Shows one has a day-offset control and a real, persisted episode
history with Watched/Undo), a Ready to watch section on the Shows tab fed
by the ported EpisodeSync/PendingEpisode system, a Services tab that picks
your streaming services and shows per-service suggestion strips, a
Discover tab covering For you / Trending / Popular for both Shows and
Movies, a nightly WorkManager refresh (plus pull-to-refresh on the
Shows tab) that keeps tracked titles' schedule/availability data and
recorded episodes current, and notification parity with iOS's app-icon
badge and per-show air-date alerts (see `notifications/` above — built on
Android's own primitives, not a literal port). Still ahead: Reminders
integration (a real "add to the system Reminders/Calendar app" action, not
just a notification), household
sharing (needs a backend — not built yet), a free-tier cap on Services
selections, OMDb ratings (TMDB
score only for now), cast, a custom Watch-on override, a settings screen
for the notification hour/private-mode toggle, deep-linking Watch now into
an installed streaming app
(always opens the JustWatch link in a browser for now), Play Billing for
the Premium one-time unlock, and Play In-App Review.

App icon: reusing the existing TV-logo PNG (`streamcueapp.com`'s `icon.png`)
as a plain raster mipmap for now rather than a true adaptive icon — that PNG
has its background baked in rather than being a transparent foreground layer.
Redo as a proper adaptive icon (foreground/background split, monochrome layer
for themed icons) before shipping to Play, not required for development.

Later or never: household sharing / cross-device sync (see "Locked-in
decisions" above), any Reminders/EventKit-equivalent integration.

## Building and checks

- `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug`
  before every push. The release build runs R8, which debug doesn't — missing-class
  warnings from libraries go in `app/proguard-rules.pro` as `-dontwarn` (with a
  comment saying why).
- Not yet verified to build in this environment (no Android SDK here) — the
  first person with a working Android toolchain to pull this should run the
  command above and fix anything it finds.

## Changelog

`CHANGELOG.md` follows Semantic Versioning, newest first, with
`## [Unreleased]` at the top. Every commit that changes the app adds one
bullet there: plain words for someone using the app, no trailing period,
wrapped at about 72 characters, a blank line between bullets. Releases bump
`versionCode`/`versionName` in `app/build.gradle.kts` in a commit "Bump
version to X.Y.Z", then "Release X.Y.Z in the changelog".
