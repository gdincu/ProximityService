Brought this app up to date so it builds and runs on modern
Android (up to Android 15 - SDK 35), and fixed the bugs found along the way.

## Fixed
- Settings screen no longer crashes when opening Operational Mode or
  Notification Behavior, and both dialogs show their options again.
- Notification setting no longer always resets to "dismiss".
- Quick Settings tile updates its color when tapped and reflects the
  real service state.
- No more crash when stopping the service on Android 13+ without
  notification permission.
- Service handles missing proximity sensor and unsupported devices
  gracefully.

## Updated
- Now targets Android 15 (was Android 10), with all libraries,
  build tools, and Kotlin brought up to date.
- Settings screen rebuilt with current Android components.
- Removed dead code, old compatibility shims, and the debug-only
  memory-leak tracker.

## Quality
- 22 automated tests, all passing, plus lint checks on every push.
- Every push automatically builds and tests the app.
