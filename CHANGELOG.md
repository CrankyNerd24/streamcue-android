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
