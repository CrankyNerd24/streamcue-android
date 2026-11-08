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
  refresh (not yet wired up).
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
  ports of iOS's `TrackedShow.swift` / `TrackedMovie.swift`. `Date` fields are
  stored as epoch `Long`s (`nextAirDateEpochDay`, `addedAtEpochMillis`,
  `lastRefreshedEpochMillis`, …). Computed-only properties on iOS
  (`effectiveAirDate`, `scheduleSummary`, `isFreeSomewhere`, `subtitle`,
  etc.) are mostly not stored fields — recompute them the same way iOS does,
  from the stored data, wherever they're needed (`TrackedMovie.subtitle` is
  the one already ported, as a Kotlin computed property). **Never read a raw
  air-date field directly outside `TrackedShow.kt`** — same rule as iOS's
  "never read `nextAirDate` outside `TrackedShow.swift`"; add the Android
  equivalent of `effectiveAirDate` here if/when something needs it, don't
  inline the arithmetic at the call site.
- `data/`: `StreamCueDatabase` (Room singleton, two entities so far),
  `TrackedShowDao` / `TrackedMovieDao` (`observeAll`, `find`, `insert` with
  `OnConflictStrategy.IGNORE`, `update`, `delete`), `Converters`
  (List<String>/List<Int> ↔ JSON via kotlinx.serialization). The database
  uses `fallbackToDestructiveMigration` — fine pre-release, revisit once
  there's an installed base worth preserving across a schema change.
- `network/`: `TmdbModels.kt` (`@Serializable` response models, explicit
  `@SerialName`, one pair of search/detail models per media type),
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
  `MoviesViewModel` (empty state vs. a live list from Room), each with its
  own Add flow pushed on top of the tab graph (`AddShowScreen`/
  `AddShowViewModel`, `AddMovieScreen`/`AddMovieViewModel` — debounced TMDB
  search, same shape as iOS's LookupView). Services is a real but trimmed
  slice: `ServicesScreen`/`ServicesViewModel` show the picked services from
  `SubscriptionsStore` (DataStore-backed, the Android analog of iOS's
  `@AppStorage(Subscriptions.key)`), and `ServicePickerScreen`/
  `ServicePickerViewModel` pick from TMDB's `/watch/providers/tv` list —
  but the per-service "popular on your services" suggestion strips from
  iOS's `SubscriptionsView` (genre aggregation from tracked shows/movies
  plus a `/discover` query per service) aren't ported yet, and there's no
  free-tier cap on selections since Premium/Billing doesn't exist on
  Android at all yet — add one alongside that work, not before it. Region
  is hardcoded to `"US"` in `TmdbApi.watchProviders` for now; iOS reads
  `AppSettings.region`, which Android has no equivalent setting for yet.
  `DiscoverScreen` is still a placeholder.
- `billing/`, `work/`: not yet populated — Play Billing (Premium unlock) and
  WorkManager (nightly refresh) land here when those features are built.

## Colour rules (same as iOS)

Colour means something — don't repurpose these. Orange (`StreamCueAccent`) is
only ever "airing today". Green (`StreamCueFree`) is only ever "free to
watch". Keep any future "aired, not actioned" amber state as its own named
colour rather than reusing either of these.

## Version 1 scope

Done: the four-tab `NavHost` shell; Shows and Movies tabs backed by Room
with Add Show / Add Movie TMDB search flows; a Services tab that picks and
stores your streaming services (no per-service suggestions yet, no
free-tier cap). Still ahead: the Discover tab (currently a placeholder),
per-service suggestion strips on Services, show/movie detail screens,
where-to-watch data surfaced per title, day-offset scheduling logic ported
from iOS, air-date notifications, nightly background refresh (WorkManager),
Play Billing for the Premium one-time unlock, and Play In-App Review.

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
