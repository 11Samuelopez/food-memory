# Food Memory engineering conventions

Keep future changes consistent with the app's small, reviewable architecture.

## Architecture

- Keep the Android app in its current single Gradle module, organized by `app`, `feature`, `domain`, `data`, and `ui` packages.
- Use MVVM: Compose screens render state and emit events; ViewModels own screen state and coroutine work; domain use cases express application actions; repositories hide persistence and network details.
- Keep Android, Room, location, and HTTP implementation details out of domain models and use cases where practical.
- Wire implementations and factories in `AppContainer`, the composition root. Do not make screens construct data sources.
- Use `StateFlow` for observable state and `viewModelScope` for cancellable asynchronous work. Rethrow `CancellationException` instead of treating cancellation as an error.

## Text, locale, and design values

- Add user-facing copy to both `translations/en.json` and `translations/es.json`; keep keys and format placeholders aligned.
- Generate Android resources with `python3 tools/generate_android_locales.py`. Spanish is the active app locale in `MainActivity`; keep English ready in `values-en` and add a matching catalog/resource set before enabling another language.
- Use `ui/theme/Color.kt` and `ui/theme/DesignTokens.kt` for shared colors, spacing, shapes, and sizes. Preserve the existing design unless the user asks for a visual change.
- Avoid user-facing hardcoded text in Kotlin. Icons/glyphs and internal identifiers are not translations.

## Persistence, permissions, and asynchronous data

- Persist structured experience data through Room repositories; write related records in a Room transaction and keep schema migrations explicit.
- Store captured photos in app-private files. Write through a temporary file and expose the destination only after a complete copy succeeds.
- Store the demo session in private preferences. Do not send local profile/session data to external services unless the relevant feature requires it.
- Request location only after the user signs in or opens the nearby feature. Never add background-location access for this foreground-only map.
- With permission granted and a signed-in session, prepare the map and nearby-place results while the user is on Home; reuse the same ViewModel and map composition when navigating to Nearby. Stop location updates on sign-out.
- Keep map-tile prewarming behind the current screen at full viewport size with an invisible TextureView render surface (`textureMode(true)`); never shrink/expand the MapView or let its GLSurfaceView overlay foreground screens.
- Sort place names with Spanish locale collation so accented vowels sort with their base letters; preserve the selected ascending/descending direction.
- Reset nearby list scroll position when sorting, sort direction, filtering, or search changes so the first result is shown.
- For CameraX capture, prefer low-latency capture, lock out duplicate taps while a photo is being written/normalized, update target rotation at capture time, and normalize EXIF before presenting/saving the image.

## Changes and verification

- Prefer focused edits with clear names and comments only where they explain lifecycle, architecture, or non-obvious behavior.
- Do not alter unrelated screens or styling as part of code cleanup.
- Compile the debug app after implementation changes. Add or run tests when the user requests verification or when the change introduces critical persistence/business behavior that needs a regression check.
