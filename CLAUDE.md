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

## Layout (`app/src/main/java/com/streamcueapp/streamcue`)

- `model/`: `TrackedShow.kt` and `TrackedMovie.kt` — field-for-field Room
  ports of iOS's `TrackedShow.swift` / `TrackedMovie.swift`. `IgnoredTitle.kt`
  is a field-for-field port of iOS's `IgnoredTitle.swift` ("Not interested"
  records) — `kind` is a `DiscoverKind` name string rather than iOS's
  `MediaKind` raw value, and is part of the primary key since TV and movie
  id spaces can collide. `Date` fields are
  stored as epoch `Long`s (`nextAirDateEpochDay`, `addedAtEpochMillis`,
  `lastRefreshedEpochMillis`, …). Computed-only properties from iOS are
  Kotlin computed properties here, same as on iOS: `effectiveAirDateEpochDay`
  and `scheduleSummary` (`TrackedShow`), `subtitle` (`TrackedMovie`),
  `headlineRating` and `isStale`/`staleAfterMillis` (both). **Never read
  `nextAirDateEpochDay` directly outside `TrackedShow.kt`** — same rule as
  iOS's "never read `nextAirDate` outside `TrackedShow.swift`"; everything
  else reads `effectiveAirDateEpochDay`, which applies `dayOffset`.
- `data/`: `StreamCueDatabase` (Room singleton, three entities so far),
  `TrackedShowDao` / `TrackedMovieDao` (`observeAll`, `observe(tmdbId)`,
  `find`, `insert` with `OnConflictStrategy.IGNORE`, `update`, `delete`),
  `IgnoredTitleDao` (`observeAll`, `insert` — see `model/IgnoredTitle.kt`
  below), `Converters` (List<String>/List<Int> ↔ JSON via kotlinx.serialization),
  `AppPreferences.kt` (the one shared `Context.appDataStore` — Preferences
  DataStore enforces a single instance per file, so `SubscriptionsStore`
  and `SectionCollapseStore` both read this rather than declaring their
  own), `SectionCollapseStore.kt` (Shows/Movies list-section collapse
  state — see the `ui/` entry below), `Refresh.kt` (TMDB-only refresh for
  one show/movie, ported from
  `TrackedShow.refresh()`/`TrackedMovie.refresh()` on iOS — shared by the
  detail screens' refresh-if-stale and `NightlyRefreshWorker`; OMDb ratings
  aren't ported, so `imdbScore`/`rottenTomatoesScore`/`metacriticScore` stay
  unset until that lands). The database uses `fallbackToDestructiveMigration`
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
  section headers — Shows into Airing today (accented) / Airing next / No
  date announced / Finished (`groupShows`, mirroring `ContentView`'s
  `dated`/`airingToday`/`upcoming`/`waiting`/`finished`, minus the Ready-
  to-watch exclusion, since no such section exists here); Movies into Free
  to watch (accented) / On a subscription / Rent or buy / Not streaming
  yet from the unwatched list (`groupMovies`, mirroring `WatchlistView`'s
  `group(_:)` over `MovieAvailability.allCases`), then Watched last. Every
  section defaults expanded and persists collapse state by exclusion
  (`SectionCollapseStore.collapsed`), except Watched, which defaults
  collapsed via its own flag (`watchedExpanded`) — iOS uses the same two
  mechanisms (`@AppStorage("collapsedShowSections")` vs.
  `@AppStorage("watchedExpanded")`), not one. No Ready-to-watch section
  and no Watch Now shortcut yet — both ride iOS's `PendingEpisode`/
  `EpisodeSync`, not ported (see below). Each row is tappable through to
  its detail screen, each with its own Add
  flow pushed on top of the tab graph (`AddShowScreen`/`AddShowViewModel`,
  `AddMovieScreen`/`AddMovieViewModel` — debounced TMDB search, same shape
  as iOS's LookupView) and its own detail screen (`ShowDetailScreen`/
  `ShowDetailViewModel`, `MovieDetailScreen`/`MovieDetailViewModel` —
  poster, schedule/subtitle, rating, a Watched switch, where-to-watch chips
  with a Watch now button that opens the stored JustWatch link, and a
  delete action; refreshes via `data/Refresh.kt` if the row is stale on
  open). No Not-interested action on these screens — "Not interested" only
  makes sense for a title you haven't tracked yet (see `TitlePreviewScreen`
  below); once it's tracked, delete is the equivalent. Trimmed vs. iOS's
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
  dialog, −7 to 365 days, same range as iOS) and a read-only "Episodes"
  list for the show's current season — fetched live from TMDB's
  `/tv/{id}/season/{n}`, not stored. This is a deliberately trimmed analog
  of iOS's "Recent episodes": no per-episode watched/dismissed state, no
  undo, no cross-session history. iOS's real episode tracking is
  `EpisodeSync` + `PendingEpisode`, a whole separate sync subsystem that
  writes individual episode records as they air and feeds the Ready-to-
  watch section — that hasn't been ported at all (StreamCue Android has no
  Ready-to-watch section yet either). Build that properly, as its own
  entity + sync logic, rather than bolting watched-state onto this
  read-only list. Both detail screens are laid out as `DetailSection.kt`
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
  per title so a single network failure doesn't skip the rest of the batch.
- `billing/`: not yet populated — Play Billing (Premium unlock) lands here
  alongside the screen that needs it.

## Colour rules (same as iOS)

Colour means something — don't repurpose these. Orange (`StreamCueAccent`) is
only ever "airing today". Green (`StreamCueFree`) is only ever "free to
watch". Keep any future "aired, not actioned" amber state as its own named
colour rather than reusing either of these.

## Version 1 scope

Done: the four-tab `NavHost` shell is fully real now — Shows and Movies
backed by Room, each grouped into collapsible sections matching iOS's own
grouping, with Add Show / Add Movie TMDB search flows and detail screens
(the Shows one has a day-offset control and a read-only current-season
episode list), a Services tab that picks your streaming services and shows
per-service suggestion strips, a Discover tab covering For you / Trending /
Popular for both Shows and Movies, and a nightly WorkManager refresh that
keeps tracked titles' schedule/availability data current. Still ahead: real
per-episode watched/dismissed tracking (iOS's EpisodeSync/PendingEpisode —
a whole separate sync subsystem, not just a read-only list) and a
Ready-to-watch section built on it, a free-tier cap on Services selections,
OMDb ratings (TMDB score only for now), air-date notifications,
deep-linking Watch now into an installed streaming app (always opens the
JustWatch link in a browser for now), Play Billing for the Premium
one-time unlock, and Play In-App Review.

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
