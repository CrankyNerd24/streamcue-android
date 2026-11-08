# Changelog

All notable changes to this project are documented here. Format follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

- Scaffold the Android project: Gradle wrapper, version catalog, app module
  wired for Compose, Room, WorkManager, DataStore, Retrofit, Coil, Play
  Billing, and Play In-App Review

- Add the `TrackedShow` Room entity, DAO, and database, ported field for
  field from the iOS SwiftData model

- Add a TMDB Retrofit client with explicit per-field serialization, avoiding
  the snake_case auto-conversion bug the iOS app hit

- Add the first vertical slice: a Shows screen backed by Room, showing an
  empty state or a live list of tracked shows

- Add an Add Show flow: a debounced TMDB search screen reachable from the
  Shows tab, with poster thumbnails and dedup against already-tracked shows

- Add the four-tab bottom nav shell (Shows, Movies, Services, Discover)
  matching the iOS app's tab bar, with Movies/Services/Discover as
  placeholders until their real data lands

- Add the `TrackedMovie` Room entity and DAO, ported field for field from
  the iOS SwiftData model, and bump the database to fall back to a
  destructive migration on a schema change (fine pre-release)

- Turn the Movies tab into a real vertical slice backed by Room, with its
  own Add Movie flow searching TMDB's movie endpoint

- Add a Services tab: pick your streaming services from TMDB's watch
  providers list and store the selection with DataStore; per-service
  suggestions and a free-tier cap are still ahead

- Add a Discover tab: browse Trending/Popular Shows and Movies from TMDB
  and add straight from the grid; the genre-based "for you" feed isn't
  ported yet

- Show a poster on each Shows-tab row — it was rendering title-only text,
  unlike the Movies tab, since the row was never given an AsyncImage

- Fix the TMDB detail endpoints reading a `genre_ids` field that doesn't
  exist on them (only search/discover responses have it); genres were
  silently always empty on refresh until this was caught

- Add show and movie detail screens: poster, schedule or year/runtime,
  TMDB rating, a Watched switch, where-to-watch chips, a Watch now button
  that opens the stored JustWatch link, and a delete action

- Add a shared TMDB-only refresh path (data/Refresh.kt) used by the detail
  screens when a title's data is stale, and by a new nightly WorkManager
  job that keeps every tracked show and movie current in the background

- Fix Discover tiles not opening anything on tap — only the small add icon
  did anything. Tapping the poster now opens a title preview screen
  (fetched straight from TMDB, since the title usually isn't tracked yet)
  with its own Add button, the Android analog of iOS's TitlePreviewSheet

- Add a real "For you" feed to Discover: up to 8 tracked titles as seeds,
  TMDB's own recommendations for each, tallied by how many seeds recommend
  a title — the same approach as iOS, not genre matching

- Add a day-offset control and a read-only current-season episode list to
  the show detail screen; the episode list is TMDB-only for now, with no
  per-episode watched state (that's iOS's EpisodeSync, a separate sync
  subsystem, not ported yet)

- Add a next-episode synopsis disclosure, a last-aired row, and an
  "Updated X ago" footer to the show detail screen

- Add "popular on your services" suggestion strips to the Services tab,
  one per service for Shows and for Movies, narrowed to the genres already
  tracked — reuses the title preview screen on tap

- Fix Discover and the Services suggestion strips showing already-tracked
  titles — they now filter them out reactively, the same as iOS's
  visibleShows/visibleMovies, so a tile disappears the moment it's added

- Rework both detail screens into titled, collapsible sections (a shared
  DetailSection composable) — Overview, Ratings, Schedule, Episodes, and
  Where to watch, standing in for iOS's grouped List/Section layout.
  Episodes defaults collapsed, matching iOS's "Recent episodes"

- Group the Shows and Movies list screens into collapsible sections,
  matching iOS's ContentView/WatchlistView: Shows into Airing today / Airing
  next / No date announced / Finished; Movies into Free to watch / On a
  subscription / Rent or buy / Not streaming yet, then Watched (collapsed
  by default, same as iOS). Collapse state persists across restarts

- Add Watched and Not interested to the title preview screen (reached from
  Discover and the Services suggestion strips), matching iOS's
  TitlePreviewSheet. Not interested records an IgnoredTitle, which now
  also filters Discover and the Services suggestion strips going forward,
  the same as iOS's visibleShows/visibleMovies hiding ignored titles
