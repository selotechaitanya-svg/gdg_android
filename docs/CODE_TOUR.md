# Tidemark Code Tour & Technical Guide

This document is your complete walkthrough of the Tidemark codebase. Written specifically for **Chaitanya Selote** to confidently explain every architectural decision, data flow, and algorithmic detail during the recruitment evaluation and live video demo.

---

## 1. Web Frontend Developer Mental Model
If you are coming from modern web engineering (React, TypeScript, Next.js, Redux/Zustand), here is how Tidemark's architecture maps directly to concepts you already know:

| Web Engineering Concept | Tidemark Android Counterpart | How it works in Tidemark |
| :--- | :--- | :--- |
| **React Components & JSX** | **Jetpack Compose (`@Composable`)** | Declarative UI functions emitting widgets. Re-renders ("recomposes") only when input state changes. |
| **Props** | **Composable Parameters** | Passed down to components (e.g., `TodayHabitCard(item, onQuickAdd)`). |
| **Component State (`useState`)** | `remember { mutableStateOf(...) }` | Local component state for ephemeral UI (e.g., dialog visibility, stepper text). |
| **Global Store / Custom Hooks (`useSelector`, Zustand)** | **`ViewModel` + `StateFlow<UiState>`** | Single source of truth. Hoists state above UI lifecycle, survives screen rotation. |
| **Store Dispatch / Actions** | **ViewModel Events (`viewModel.toggleYesNo(...)`)** | Unidirectional Data Flow (UDF). UI dispatches intent; ViewModel executes business logic and updates state. |
| **IndexedDB / Prisma ORM** | **Room Database (SQLite via KSP)** | Local persistent SQL database. Maps SQLite tables to Kotlin data classes with reactive observable streams. |
| **`Promise` / `async` / `await`** | **Kotlin Coroutines (`suspend`)** | Structured asynchronous concurrency without blocking the main/UI thread. |
| **`RxJS` Observables / EventStreams** | **Kotlin `Flow`** | Cold asynchronous data streams (`observeHabits()`). Whenever the Room database updates, Room emits a new list to the collector automatically. |
| **`Context API` / Dependency Injection** | **`AppContainer` (Manual DI)** | A clean dependency graph instantiated in `Application` class and passed to ViewModels, without heavy magic frameworks like Hilt or Dagger. |

---

## 2. Multi-Module Architecture Map

```
tidemark/
├── domain/                      # PURE Kotlin/JVM Module (Zero Android dependencies)
│   ├── model/                  # Domain entities (Habit, HabitLog, Schedule, StreakResult)
│   ├── logic/                  # Pure functional algorithms (Streak, Stats, Validation, Converters)
│   ├── time/                   # TimeProvider interface & SystemTimeProvider
│   └── repo/                   # HabitRepository interface
└── app/                         # Android Application Module
    ├── data/
    │   ├── db/                 # Room Database, DAOs, Entity models (exportSchema = true)
    │   ├── mapper/             # Mappers translating Room entities to pure Domain models
    │   ├── settings/           # DataStore Preferences (Theme & Stopwatch persistence)
    │   └── backup/             # Strict JSON export/import serializer (SAF)
    ├── reminders/              # Exact AlarmManager scheduler & BroadcastReceivers
    └── ui/                     # Jetpack Compose UI (Theme, Components, Screens, Navigation)
```

### Why this boundary matters:
- **Testability**: The graded streak, statistics, reminder planning, and validation engines live in `:domain`. They run on the standard JVM in **seconds** without requiring an Android emulator or SDK stubs.
- **Independence**: Changing database libraries (e.g., switching Room to SQLDelight) or UI frameworks has zero effect on domain business logic.

---

## 3. The Streak Engine Step-by-Step

All streak calculations live in pure functions in `com.tidemark.habits.domain.logic.StreakCalculator`. The system accepts `(habit, logs, today)` and performs no I/O.

### A. Daily and Weekdays Algorithm
1. **Filter Horizon**: We only evaluate dates in `[habit.startDate, today]`. Dates before `startDate` are never missed; future dates cannot be logged.
2. **Scheduled Day Classification**:
   - `COMPLETE`: A log exists with `completed == true`.
   - `MISSED`: The date is scheduled, strictly before `today`, and not complete.
   - `PENDING`: Only possible for `today` if scheduled and not yet complete. The day is not over yet!
3. **Drop Trailing Pending**: If today is scheduled but not completed, it is **PENDING** and dropped from the trailing evaluation so mid-day logging doesn't prematurely kill the streak.
4. **Current Streak**: Length of the contiguous trailing run of `COMPLETE` scheduled days.
5. **Longest Streak**: Longest contiguous run of `COMPLETE` scheduled days across the habit's lifetime.

#### Worked Example: The Brief's Normative Task
- Habit: `read` (Daily, target 20)
- Habit: `gym` (Weekdays Mon/Wed/Fri, target 1)
- Reference: Mon 12 (read), Mon done (gym); Tue 20 (read), Tue unscheduled (gym); Wed 25 (read), Wed done (gym).

| Date | Day | Read Log | Read Target | Read Status | Gym Scheduled? | Gym Log | Gym Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Mon** | Mon | 12 | 20 | **MISSED** (12 < 20) | Yes | 1 (Done) | **COMPLETE** |
| **Tue** | Tue | 20 | 20 | **COMPLETE** | No | — | *Ignored (Unscheduled)* |
| **Wed** | Wed | 25 | 20 | **COMPLETE** | Yes | 1 (Done) | **COMPLETE** |

- **On Wednesday evening**:
  - `read` streak: Trailing run of COMPLETE = `[Tue, Wed]` → **Streak = 2**. (Monday was missed).
  - `gym` streak: Scheduled days are Mon & Wed. Both are COMPLETE. Tuesday was unscheduled and transparent → **Streak = 2**.
- **User corrects Monday**:
  - User updates Monday's read log from 12 to 20.
  - Monday becomes COMPLETE.
  - Trailing run of COMPLETE is now `[Mon, Tue, Wed]` → **Streak = 3**!

---

### B. Times Per Week (`TimesPerWeek(N)`) Algorithm
- **Streak Unit**: **Weeks** (not days).
- **Week Definition**: ISO-8601 Monday through Sunday.
- **Evaluated Weeks**: From the habit's start week up to the current week.
- **Rule**:
  - `doneInWeek`: Number of distinct days in that Monday–Sunday span with `completed == true`.
  - `required`: Normally `N`.
  - **Pro-rated First Week**: If habit starts mid-week (e.g. Saturday), `required = min(N, daysRemainingInWeek)`. Starting on Saturday with goal 3/week only requires 2 days (Sat + Sun).
  - A finished week is **MET** if `doneInWeek >= required`, else **UNMET**.
  - Current week: If already MET, counts towards current streak. If not yet met, it is **PENDING** and dropped from breaking the streak until the week ends Sunday at 23:59:59.

---

## 4. How to Add a New Schedule Type in 5 Minutes

To demonstrate clean architecture to an interviewer:
1. **Add sealed subtype in `:domain`**:
   ```kotlin
   // In domain/model/Schedule.kt
   data class EveryXDays(val intervalDays: Int) : Schedule
   ```
2. **Add evaluation branch in `:domain`**:
   ```kotlin
   // In domain/logic/ScheduleEvaluator.kt
   is Schedule.EveryXDays -> {
       val daysBetween = ChronoUnit.DAYS.between(habit.startDate, date)
       daysBetween >= 0 && daysBetween % schedule.intervalDays == 0L
   }
   ```
3. **Add bitwise/schema mapping in `:app`**:
   Update `HabitMapper.kt` and `RoomHabitRepository.kt` to serialize the new type.
4. **Done!** The entire streak engine, rate calculator, calendar view, and reminder planner immediately support the new schedule without modifying core logic.

---

## 5. 12 Evaluator Questions & High-Scoring Answers

### Q1: Why did you separate `:domain` and `:app` into two Gradle modules?
**Answer:** "The recruitment rubric places the highest weight on Correctness and Architecture. By keeping `:domain` pure Kotlin/JVM with zero Android dependencies, all streak algorithms, date calculations, and business rules are 100% unit-tested on the host JVM in seconds. It enforces strict separation of concerns: the UI cannot bleed into business logic, and database entities never contaminate domain models."

### Q2: Why did you use manual Dependency Injection (`AppContainer`) instead of Hilt or Koin?
**Answer:** "For an offline-first single-module application of this scope, Hilt introduces heavy compile-time annotation processing, code generation overhead, and reflection. Koin introduces runtime resolution overhead. Pure constructor injection via an `AppContainer` in the `Application` class is explicit, compile-time verified, zero-reflection, and trivial to trace and debug."

### Q3: How do you handle daylight saving time (DST) transitions?
**Answer:** "We use `java.time.LocalDate` for all daily keys and calendar math. We never add 24 hours (86,400 seconds) to an `Instant` to find 'tomorrow'—doing so breaks on 23-hour or 25-hour DST transition days. For reminders, we construct `ZonedDateTime.of(date, reminderTime, zone)`. In a DST spring gap (e.g. 02:30 skipped), `java.time` cleanly adjusts forward to 03:30; in an autumn overlap, it defaults to the earlier valid offset."

### Q4: How do habit reminders survive a phone reboot?
**Answer:** "Alarms registered in Android's `AlarmManager` are cleared by the OS upon power down. We registered a `BootReceiver` listening for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, and `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`. When triggered, `BootReceiver` uses `goAsync()`, queries active habits with reminders from Room, and calls `reminderScheduler.rescheduleAll()`. We also run this reschedule on app launch."

### Q5: Why one-shot alarms instead of `setRepeating`?
**Answer:** "Android deprecated inexact `setRepeating` for exact timing starting in API 19. Furthermore, repeating alarms cannot dynamically account for weekday schedules or whether a habit has already been completed today. Tidemark uses `setExactAndAllowWhileIdle` to arm only the *next* trigger. When `ReminderReceiver` fires, it checks `ReminderPlanner.shouldNotify()`, notifies if needed, and immediately calculates and arms the subsequent trigger."

### Q6: What happens if a user force-stops the app or runs on an aggressive OEM battery saver?
**Answer:** "In Android, if an app is Force-Stopped by the user, the OS puts it in a 'stopped state' where no alarms or broadcast receivers fire until the user opens the app manually. Furthermore, OEM skins like MIUI (Xiaomi) or ColorOS (Oppo) kill background alarms. In Settings, Tidemark provides a 'Reminder Health' card that guides users to battery optimization settings and provides a 'Send test reminder (10 s)' button to verify delivery."

### Q7: How does 'correcting a past day' work in Room?
**Answer:** "The `habit_logs` table has a composite primary key `(habitId, date)`. Writing a log for an existing date executes an `UPSERT` via Room. Because `observeLogs()` returns a cold `Flow`, any update immediately emits a new log list to the ViewModel. The ViewModel re-runs `StreakCalculator.calculateStreak()`, instantly updating current and longest streaks across the entire history."

### Q8: What if a habit type is changed (e.g. Yes/No to Countable)?
**Answer:** "Changing type is handled atomically inside a single database transaction via `changeHabitType()`. Per section 6 rules:
- Yes/No → Countable/Timed: Completed days get `value = newTarget`, others get 0.
- Countable/Timed → Yes/No: Completed days get `value = 1`, others get 0.
- Countable ↔ Timed: Values and flags are preserved.
A confirmation dialog warns the user before applying the change."

### Q9: Why did you not declare the `INTERNET` permission?
**Answer:** "Rule 4 of the Prime Directives states 'Zero cloud: no accounts, no network calls, no API keys'. Tidemark guarantees 100% offline privacy by omitting `android.permission.INTERNET` from `AndroidManifest.xml`. Graders can verify this directly in the merged manifest."

### Q10: How are partial logs handled in streak calculations?
**Answer:** "A day is only complete if `value >= habit.target`. If a user logs 12 out of 20 pages, `ProgressEvaluator.isComplete()` returns `false`. In calendar and stats, this day is classified as `DayState.PARTIAL`. For Daily habits, a past partial day breaks the streak just like an unlogged day."

### Q11: How is midnight rollover handled in the UI?
**Answer:** "TodayScreen observes system time changes through three mechanisms:
1. On resume: Screen checks `timeProvider.today()`.
2. Dynamic BroadcastReceiver: Listens for `ACTION_DATE_CHANGED`, `ACTION_TIME_SET`, and `ACTION_TIMEZONE_CHANGED`.
3. Stopwatch ticker: Live tick checks for date boundary crossings.
When the clock strikes 00:00:00, the date strip and habit items roll over automatically without requiring an app restart."

### Q12: How does Data Backup ensure referential integrity?
**Answer:** "`BackupSerializer.validateAndParse()` strictly validates the JSON envelope: verifies `schemaVersion == 1`, parses all ISO dates and enums, ensures values are non-negative, and verifies that every log references an existing habit ID in the file. If any check fails, the import aborts without touching the database."
