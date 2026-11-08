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
