# Tidemark — Offline-First Android Habit Tracker

> An offline-first, privacy-focused Android habit tracker engineered for high-precision streak mathematics, reboot-resilient reminders, and deliberate Material 3 ergonomics.

**Author**: Chaitanya Selote — GDG on Campus RBU, Recruitment Drive 2026-27 (Position: Android)  
**Package**: `com.tidemark.habits`  
**Application ID**: `com.tidemark.habits`  

---

## 📸 Screenshots

| Today Screen | Habits Overview | Habit Detail & Calendar | 12-Week Progress |
| :---: | :---: | :---: | :---: |
| ![Today](docs/screenshots/01_today_screen.png) | ![Habits](docs/screenshots/03_habits_list.png) | ![Detail](docs/screenshots/05_habit_detail_calendar.png) | ![Chart](docs/screenshots/06_habit_detail_chart.png) |

| Habit Editor | Statistics Dashboard | Night Sea (Dark) | Reminder Notification |
| :---: | :---: | :---: | :---: |
| ![Editor](docs/screenshots/04_habit_editor.png) | ![Stats](docs/screenshots/07_stats_overview.png) | ![Dark](docs/screenshots/08_dark_theme.png) | ![Notification](docs/screenshots/09_reminder_notification.png) |

*(Note: See [docs/screenshots/README.md](docs/screenshots/README.md) for individual image captures and instructions).*

---

## ✨ Features

### Core Offline Functionality
- **Habit Types**:
  - **Yes/No**: Binary completion toggle with animated spring ring and light haptic feedback.
  - **Countable**: Incremental progress with customizable units (pages, glasses, km, reps) and one-tap `+step` quick add.
  - **Timed**: Stopwatch timer that survives process death, screen rotation, and backgrounding via DataStore timestamp persistence (with +5 min manual quick credit).
- **Flexible Schedules**:
  - **Daily**: Track habits every single calendar day.
  - **Weekdays**: Specific days of the week (e.g. Mon · Wed · Fri). Unscheduled days are transparent and never break a streak.
  - **N Times Per Week**: Target completion count per ISO week (e.g. 3× per week). First partial week is fairly pro-rated.
- **Precision Streaks & Statistics**:
  - Exact current and longest streak calculations respecting scheduled days, pending today, and retroactive edits.
  - 7-day, 30-day, and all-time completion rate statistics (never divides by zero).
  - Custom Canvas-drawn month calendar with 8 distinct `DayState` visuals (Complete, Partial, Missed, Pending, Unscheduled, Neutral, Before-Start, Future).
  - 12-week animated progress bar chart drawn on Canvas.
- **Reliable Local Reminders**:
  - Exact `AlarmManager` one-shot alarms (`setExactAndAllowWhileIdle`).
  - Progress-aware notification messages (e.g. *"12 / 20 pages logged today"*).
  - Re-registered automatically upon phone reboot (`BOOT_COMPLETED`), app update, or timezone change.
- **Zero Cloud / Complete Privacy**:
  - Strictly **NO `android.permission.INTERNET`** declared in the manifest.
  - Zero analytics, zero third-party cloud SDKs, zero user accounts.

### 🌟 Bonus Features
- **[B1] Light / Dark / System Themes**: Custom "Beach Green" palette (sand mist, sea glass, lagoon green) and "Night Sea" dark mode, instant switching backed by DataStore.
- **[B2] "Mark Done" Notification Action**: Complete a habit directly from the system reminder drawer without opening the app (credits the exact date the reminder was triggered for).
- **[B3] Complete Data Backup (Export/Import)**: Offline JSON backup via Android Storage Access Framework (SAF) with strict referential integrity validation.
- **[B4] Reminder Health Diagnostics**: In-app diagnostic card verifying notification permission, exact alarm permission, OEM battery guidance, and a **"Send Test Reminder (10 s)"** button for grader verification.

---

## 🏛️ Architecture & Tech Stack

```
                                  ┌────────────────────────┐
                                  │   Compose UI Screens   │
                                  │ (Today, Detail, Stats) │
                                  └───────────┬────────────┘
                                              │ StateFlow / Events
                                  ┌───────────▼────────────┐
                                  │       ViewModels       │
                                  └───────────┬────────────┘
                         ┌────────────────────┴────────────────────┐
                         ▼                                         ▼
            ┌─────────────────────────┐               ┌────────────────────────┐
            │   :domain (Pure JVM)    │               │    :app Data Layer     │
            │  StreakCalculator       │               │ Room DB (SQLite)       │
            │  StatsCalculator        │◄──────────────┤ DataStore Preferences  │
            │  ReminderPlanner        │   Entities    │ AlarmManager Scheduler │
            │  HabitValidator         │   to Domain   └────────────────────────┘
            └─────────────────────────┘
```

### Data Flow 1: Logging a Habit
```
[User taps Card / Stepper] 
       │
       ▼
TodayViewModel.setLogValue() 
       │
       ▼
LogEditor.evaluate() (Pure Domain Rule Check)
       │
       ▼
RoomHabitRepository.setLog() (Atomic SQLite UPSERT)
       │
       ▼
Room emits new List<HabitLog> via cold Flow
       │
       ▼
TodayViewModel recalculates StreakCalculator.calculateStreak()
       │
       ▼
TodayUiState updates ──► Compose UI recomposes in 16ms
```

### Data Flow 2: Reminder Scheduling & Execution
```
Habit Created / Edited with Reminder
       │
       ▼
ReminderPlanner.nextTrigger() (Pure Domain Planning)
       │
       ▼
AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP)
       │
       ▼ [Alarm Fires]
ReminderReceiver.onReceive() (goAsync + Coroutine)
       │
       ├──► ReminderPlanner.shouldNotify() ──► If true: NotificationHelper.post()
       │
       └──► ALWAYS arms next trigger via ReminderPlanner.nextTrigger()
```

---

## 🚀 Setup & Build Instructions

### Prerequisites
- **JDK**: Java 17 or higher.
- **Android Studio**: Android Studio Ladybug (2024.2+) or Jellyfish with Android SDK Platform 35.
- **Target Device / Emulator**: Android 8.0 (API 26) through Android 15 (API 35).

### Command Line Build

#### macOS / Linux
```bash
# Build Debug APK
./gradlew assembleDebug --console=plain

# Run Pure Domain Unit Tests (50 tests)
./gradlew :domain:test --console=plain

# Run App Unit Tests
./gradlew testDebugUnitTest --console=plain

# Run Android Lint
./gradlew lintDebug --console=plain

# Install on connected device/emulator
./gradlew installDebug
```

#### Windows (PowerShell / Command Prompt)
```powershell
# Build Debug APK
.\gradlew.bat assembleDebug --console=plain

# Run Pure Domain Unit Tests (50 tests)
.\gradlew.bat :domain:test --console=plain

# Run App Unit Tests
.\gradlew.bat testDebugUnitTest --console=plain

# Run Android Lint
.\gradlew.bat lintDebug --console=plain

# Install on connected device/emulator
.\gradlew.bat installDebug
```

### Permissions Granted
- `POST_NOTIFICATIONS` (Android 13+ / API 33+): Requested in context when enabling a habit reminder.
- `SCHEDULE_EXACT_ALARM` (Android 12+ / API 31+): Used for precise alarm dispatch.
- `RECEIVE_BOOT_COMPLETED`: Reschedules all alarms upon phone reboot.
- **Zero Network**: The app contains no `INTERNET` permission.

---

## 📋 The Five Technical Decisions

### (a) Why this tech stack and architecture?
We chose native Kotlin, Jetpack Compose, Material 3, Room, and a decoupled `:domain` module because it aligns directly with Android's official modern guidelines and the core grading rubric. Pure Kotlin Compose allows declarative state-driven rendering with zero XML bloat and smooth canvas drawing. Isolating all algorithmic rules into a pure Kotlin/JVM `:domain` module ensures that our streak and statistics logic can be verified with host unit tests in seconds without Android runtime overhead. We chose manual dependency injection (`AppContainer`) to avoid the reflection, annotation overhead, and black-box build complexity of Hilt/Dagger for an offline-first app.

### (b) How are streaks computed (especially for "N times per week")?
For **Daily** and **Weekdays**, the horizon `[startDate, today]` is mapped into scheduled days. Each day is classified as `COMPLETE`, `MISSED`, or `PENDING` (today is pending if not yet completed). Trailing pending days are dropped so mid-day logging doesn't break streaks. The current streak is the trailing run of `COMPLETE` days. For **N times per week**, the streak unit is **weeks** (ISO Monday–Sunday). A week is `MET` if distinct completed days `≥ required`. The habit's starting week is pro-rated (`required = min(N, daysRemainingInWeek)`) so starting on a Saturday does not penalize the user. An in-progress week that is not yet met is treated as `PENDING` until Sunday at 23:59:59.

### (c) How do reminders work and what happens after a phone restart?
Reminders utilize Android's `AlarmManager` with one-shot `setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` triggers pointing to `ReminderReceiver`. We never use `setRepeating`. When an alarm fires, `ReminderReceiver` checks `ReminderPlanner.shouldNotify()` (suppressing notifications if the habit is already completed today), displays a progress-aware notification, and immediately schedules the *next* occurrence. Because the OS clears all alarms on power down, a `BootReceiver` listens for `ACTION_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, and `TIMEZONE_CHANGED`, queries active reminders from Room via `goAsync()`, and reschedules every habit alarm.

### (d) How are dates and time zones handled?
All calendar days are represented strictly by `java.time.LocalDate` in the device's current time zone, and logs are keyed by ISO-8601 date strings (`YYYY-MM-DD`). We avoid `java.util.Date` and timestamp math (e.g. never adding 86,400 seconds to an `Instant` to find tomorrow, which fails across 23/25-hour daylight saving shifts). When crossing time zones, stored local log dates are preserved. Reminders are constructed as `ZonedDateTime.of(date, time, zone)`, allowing `java.time` to natively resolve DST spring gaps (forward shift) and autumn overlaps.

### (e) What would you improve with more time?
With more time, we would implement:
1. **Schedule Versioning**: Archiving past schedule configurations so changing from Daily to Weekdays preserves past completion rates without re-evaluating historical dates.
2. **Decimal & Floating Targets**: Supporting fractional units (e.g. 2.5 liters of water or 5.2 km running).
3. **Glance Home-Screen Widget**: An interactive Jetpack Glance widget displaying today's progress ring and quick-complete buttons.
4. **Peer-to-Peer Offline Sync**: Encrypted local Wi-Fi or Bluetooth backup sync between phone and tablet without a central cloud server.

---

## 🧪 Automated Testing Suite

Tidemark features **50 pure domain unit tests** running on the JVM, plus ViewModel tests and serialization validation.

```bash
./gradlew :domain:test --console=plain
```

### Coverage by Functional Area
| Test Suite Class | Tests | Focus Area | Status |
| :--- | :---: | :--- | :---: |
| `TaskExampleTest.kt` | 3 | **T1–T3**: Normative GDG example (Read 20 pgs, Gym Mon/Wed/Fri, past edit 2 → 3) | ✅ GREEN |
| `DailyStreakTest.kt` | 8 | **T4–T11**: Missed day breaks, pending today, year & leap-day boundaries | ✅ GREEN |
| `WeekdayStreakTest.kt` | 6 | **T12–T17**: Unscheduled days transparent, Friday pending, Sunday-only habits | ✅ GREEN |
| `TimesPerWeekStreakTest.kt` | 9 | **T18–T26**: ISO week boundaries, pro-rated start weeks, N-per-week runs | ✅ GREEN |
| `StatisticsTest.kt` | 8 | **T27–T34**: 7d/30d rates, DayState mapping, multi-habit overview | ✅ GREEN |
| `EditingPastDaysTest.kt` | 5 | **T35–T39**: Past day corrections, retroactive breaks, gap merging | ✅ GREEN |
| `MidnightTimezoneDstTest.kt` | 4 | **T40–T43**: 23:59:59 vs 00:00:00 rollover, timezone stability, US DST | ✅ GREEN |
| `ReminderPlannerTest.kt` | 4 | **T44–T47**: Next trigger calculation, notification suppression, DST gaps | ✅ GREEN |
| `ValidationAndConverterTest.kt` | 3 | **T48–T49**: Validation error reporting, atomic habit type log conversions | ✅ GREEN |
| `TodayViewModelTest.kt` | 6 | Today date strip filtering, quick-add, toggle, stopwatch persistence | ✅ GREEN |
| `BackupSerializerTest.kt` | 4 | JSON backup export/import round-trip, malformed schema & integrity checks | ✅ GREEN |
| **TOTAL** | **60** | **Comprehensive Domain, Architecture & Data Layer Tests** | **✅ 100% GREEN** |

---

## ⚠️ Assumptions & Limitations
1. **Whole-Number Values**: Log progress and habit targets are whole integers.
2. **Monday-First Weeks**: In accordance with ISO-8601, all weeks begin on Monday and conclude on Sunday.
3. **Schedule Re-evaluation**: Modifying a habit's schedule re-evaluates all historical logs against the new schedule (schedule versioning is a future enhancement).
4. **Time Zone Shifts**: Stored logs remain anchored to their original local calendar date regardless of subsequent time zone travel.
5. **OEM Battery Killers**: Aggressive third-party battery managers (MIUI, ColorOS) can delay background alarms unless battery optimization is disabled for Tidemark.

---

## 🤖 AI Usage Disclosure

In compliance with the GDG recruitment rules:
- **Assistant Used**: AntiGravity (powered by Google Gemini 2.5 Pro).
- **Work Performed by Assistant**:
  - Implemented multi-module Gradle Kotlin DSL structure.
  - Implemented pure functional streak calculation algorithms and unit test assertions based on the specification.
  - Generated Room entity mapping, DataStore settings, and AlarmManager broadcast receivers.
  - Built declarative Jetpack Compose screens and Canvas charts adhering to the Beach Green design specification.
- **Work Performed by Applicant (Chaitanya Selote)**:
  - Reviewed and analyzed all algorithmic models, streak calculation tables, and edge cases.
  - Verified local build execution and Gradle wrapper integrity on Windows.
  - Recorded live demonstration video covering the normative example, reminders, and automated tests.
  - Prepared submission documentation, repository links, and final package artifacts.
