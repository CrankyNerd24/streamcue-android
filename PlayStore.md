# Google Play release notes

What to fill in on the Play Console for StreamCue for Android.

## Before the first upload

- [ ] Play Console developer account ($25 one-time)
- [ ] New personal accounts: a **closed test with at least 12 testers for 14 days in a row** before production
- [ ] App created with package `com.streamcueapp.streamcue` (permanent)
- [ ] Upload key and Play App Signing set up; release builds signed (not set up in the repo yet)

## Premium (in-app product)

One **non-consumable** (managed) product, the direct analog of the iOS app's
one-time StoreKit 2 unlock. No grandfathering concept carries over to
Android — that's a fix for an iOS pricing-history problem with no equivalent
here, so every Android purchaser pays the current price.

| Product ID | Name | Price |
|---|---|---|
| `streamcue_premium` | StreamCue Premium | $3.99 |

Gates household sharing (once built — see `CLAUDE.md`'s locked-in decisions)
and the free-tier caps (shows/services tracked), same as iOS. Never gates the
underlying TMDB/OMDb availability info itself.

- [ ] Payments profile set up (Console → Settings → Payments profile)
- [ ] `streamcue_premium` created and active (Monetize → In-app products)
- [ ] Testers added as license testers (Settings → License testing) to buy without being charged

## Permission declarations

- **Internet** (`INTERNET`): required for TMDB/OMDb lookups — there's no offline
  mode, the app is a thin client over those APIs plus the local Room database.
- **Notifications** (`POST_NOTIFICATIONS`): air-date alerts for tracked shows,
  the Android analog of iOS's local notifications.
- **Billing** (`com.android.vending.BILLING`): required by Google Play Billing
  for the Premium purchase.

## Data safety

- No account, no sign-in. Tracked shows/movies and settings stay on the
  device (Room + DataStore) — v1 has no backend, no sync, no analytics.
- Purchases (Premium) are handled by Google Play.
- Third-party content: show/movie metadata, posters, and streaming
  availability come from TMDB; ratings optionally from OMDb. Both are
  outbound API calls only — no personal data is sent to either beyond the
  title being looked up.

## Content rating

Answer the questionnaire as a general-audience reference app: no user-generated
content, no chat, no in-app purchases beyond the single non-consumable unlock,
and media metadata/synopses sourced from TMDB/OMDb (which can include mature
themes in a show's own description, same exposure as any TV/film database app).
