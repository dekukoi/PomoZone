# PomoZone

A calm, offline Android Pomodoro timer with an honest statistics dashboard. No accounts, no network, no points or gamification.

## Features

- **25 / 5 / 15 Pomodoro cycle:** 25-minute Focus blocks, 5-minute Short breaks, and a 15-minute Long break after every fourth completed Focus session.
- **Session controls:** Start, pause, resume, reset, and skip. Only Focus blocks that run to zero are saved.
- **Statistics dashboard:** Today, 7-day, and all-time focus totals, daily goal ring (4 sessions/day), 7-day bar chart, current streak, and a 12-week activity heatmap—all derived from the stored list of completed sessions.
- **System theming:** Follows the device light and dark theme using Material 3.

## Architecture

- **Pure Kotlin domain layer (`domain/`):** Owns the Pomodoro state machine (`TimerEngine`) and statistics projection (`StatsProjection`) with zero Android imports, driven by an injected `java.time.Clock` and `ZoneId` for deterministic JUnit 4 testing.
- **Persistence (`data/`):** Local Room database with a single `sessions` table storing completed session start instants and planned durations.
- **UI (`ui/`):** Jetpack Compose + Material 3 with custom `Canvas`-drawn progress ring, goal ring, weekly bars, and 12-week heatmap. Manual application-level wiring (`AppContainer`) without DI or navigation frameworks.

## Accurate Timer Approach

Instead of counting periodic ticks (which drift when updates are delayed or the app is backgrounded), the timer records an `endInstant = now + remaining` whenever a block starts or resumes. Remaining time is always recomputed as `endInstant - now` from the injected `Clock`. Pausing stores the remaining duration and discards `endInstant`; resuming computes a fresh `endInstant`.

## Known Limitations

- **Process suspension:** A block that finishes while the app process is suspended is completed exactly once when the user returns to the app, without firing a notification at the exact moment it ended.

## What I'd Add Next

- A foreground service (or scheduled notification alarm) to alert at the exact moment a block completes while the app is suspended
- Custom durations and daily goal preferences (via DataStore)
- Optional keep-screen-on and Do Not Disturb integration
