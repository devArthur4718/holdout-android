# Play Console answers

What to enter in each Play Console form for **Holdout** (`com.devarthur.holdout`). If the app changes (a network library, analytics, ads, a new permission), update this file and the forms together.

## App details

| Field | Value |
|---|---|
| App name | Holdout: Drop bad habits |
| Default language | English (United States) — en-US |
| App or game | App |
| Free or paid | Free (can't be changed to paid later) |
| Category | Health & Fitness |
| Tags | Habit tracker, Quit smoking, Self-improvement (pick the closest offered) |
| Privacy policy | https://devarthur4718.github.io/holdout-android/privacy/ |
| Contact email | (your developer email, required) |

Listing text and screenshots: `fastlane/metadata/android/<locale>/` (en-US, pt-BR, es-ES).

## App content (Policy › App content)

**Privacy policy:** the URL above.

**Ads:** No, the app does not contain ads.

**App access:** All functionality is available without special access (no login).

**Content rating (IARC questionnaire):** Category *All other app types*. Answer honestly:
- Violence, sexuality, language: No.
- Controlled substances, alcohol, tobacco: the app *references* them (habit names like Cigarettes, Alcohol) but does not depict, promote or sell them. Answer yes to "references" if asked, no to "promotes use".
- Gambling: No simulated or real gambling. (Gambling is only named as a habit to quit.)
- User-generated content shared with others: No. Interaction between users: No. Shares location: No. Digital purchases: No.

**Target audience and content:** 18 and over. Not designed for children. No appeal to children.

**News app:** No.

**COVID-19 contact tracing / status:** No.

**Data safety:**
- Does your app collect or share any of the required user data types? **No.**
  (Data processed only on the device and never sent off it doesn't count as "collected". The app has no INTERNET permission.)
- Is all user data encrypted in transit? Not applicable (nothing is transmitted).
- Do you provide a way for users to request that their data be deleted? Not required when nothing is collected; the app has Settings › Delete all data anyway.

**Health apps declaration:** select the features that fit, likely *Addiction / substance use* or *Wellness / habit tracking*, under health & fitness. The app gives no medical advice, diagnosis or treatment. The listing says it isn't a medical service.

**Financial features:** None.

**Government apps:** No.

**Permissions declarations:** none needed. `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` are normal permissions. No exact-alarm, location, SMS or call-log permissions.

## Release

- Upload `app/build/outputs/bundle/release/app-release.aab` (`./gradlew :app:bundleRelease`, signed with the upload key from `keystore.properties`).
- Use Play App Signing (default for new apps).
- Upload the R8 mapping file if the Console doesn't take it from the bundle automatically: `app/build/outputs/mapping/release/mapping.txt`.
- Release notes: `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
- Each new upload needs a higher `versionCode` in `app/build.gradle.kts`.

## Graphics

- App icon (512 × 512, 32-bit PNG): `fastlane/metadata/android/en-US/images/icon.png`
- Feature graphic (1024 × 500, no transparency), one per language: `fastlane/metadata/android/<locale>/images/featureGraphic.png`
- Phone screenshots (1080 × 2084, English UI): `fastlane/metadata/android/en-US/images/phoneScreenshots/`. Play shows them for every language unless a language has its own.
