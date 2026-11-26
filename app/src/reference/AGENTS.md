# PomoZone: agent rules

Android Pomodoro timer with a stats dashboard. Kotlin, Jetpack Compose, Material 3, Room.
Package: `com.dekukoi.pomozone`. Scope and rules live in the spec and tickets, not here.

## Read, in this order
1. The ticket you were given: `.scratch/pomozone/issues/NN-*.md`
2. Only the parts of `.scratch/pomozone/spec.md` the ticket needs (Implementation Decisions, Testing Decisions)
3. UI tickets only: `DESIGN.md`. Open `docs/design-preview.html` only if DESIGN.md is unclear.

Do not read or change files unrelated to the ticket.

## Workflow
- One ticket at a time. Work only on the ticket you were given.
- Write the tests the ticket names, then the code.
- Done means every acceptance box is true and both pass: `./gradlew test` and `./gradlew assembleDebug`.
- When done: tick the ticket's boxes, set its Status to `done`, then commit with `git add` and `git commit -m "NN: <ticket title>"`. Never push.
- Never edit, delete or weaken an earlier test without the owner's approval. If an earlier test fails, fix your code, not the test.

## Stop rule
After 2 failed attempts at a ticket, stop. Write `NN-failure-note.md` beside the ticket with: what you tried, the last error (exact text), and the files you touched. Do not start fixing unrelated things.

## You may run without asking
`./gradlew test`, `./gradlew assembleDebug`, `git status`, `git diff`, `git add`, `git commit`.
Ask first for everything else. Never run `git push`.

## Owner-controlled (never edit)
Version catalog, every `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, the Gradle wrapper.
If a ticket needs a new dependency or a Gradle change, stop and ask. Do not add or upgrade any dependency.
The manifest is yours to edit (orientation, permissions, application class).

## Stack
Kotlin, Compose, Material 3, Room with KSP, DataStore (not needed yet), lifecycle view-model support for Compose, JUnit 4, kotlinx-coroutines-test, minSdk 26.
Not allowed: Hilt or any DI framework, navigation-compose, chart libraries, mocking libraries, Robolectric, XML layouts.

## Layout
```
app/src/main/java/com/dekukoi/pomozone/
  domain/   pure Kotlin, NO android.* imports: timer engine, session model, stats projection, constants
  data/     Room entity, DAO, database, session store implementations
  ui/       timer/, stats/, shell; ui/theme/ already has Color.kt, Type.kt, Theme.kt: use them, do not redo them
  notify/   completion notifier
  debug/    demo data
  AppContainer.kt  MainActivity.kt  PomoZoneApp.kt
app/src/test/java/com/dekukoi/pomozone/   tests, mirroring the package layout
```

## Domain rules
- Time comes only from an injected `java.time.Clock`. Never call `Instant.now()`, `LocalDate.now()` or `System.currentTimeMillis()` directly.
- Stats functions take the current instant (or clock) and a `ZoneId` as parameters, so tests can use non-UTC zones.
- Remaining time is always `endInstant - now`. Never count ticks.
- Durations, the daily goal (4) and heatmap bounds are named constants in one place in `domain/`.
- A Day is a `LocalDate` in the given zone, taken from the session's start instant.

## Test rules
- JUnit 4 and kotlinx-coroutines-test, under `app/src/test`. No Android types, no mocks.
- Use hand-written fakes: a mutable fake clock and an in-memory session store.
- Test through public functions only. Never assert on private state or call order.
- Test names are plain sentences in backticks, for example `` `fourth focus block leads to a long break` ``.

## UI rules
- State flows down, events flow up. ViewModels expose `StateFlow`; screens use `collectAsState()`.
- Use `viewModelScope`, never `GlobalScope`. Cancel any tick loop when the ViewModel clears. The loop only refreshes the display; the engine derives the time from the clock.
- Colors come only from `MaterialTheme.colorScheme` or `PomoTheme.colors`. Never write `Color(0x...)` in a screen.
- Sizes, spacing, type and copy come from `DESIGN.md`.
- Charts and rings are drawn with Compose `Canvas`.
- The debug-build check is `ApplicationInfo.FLAG_DEBUGGABLE`, not `BuildConfig`.
