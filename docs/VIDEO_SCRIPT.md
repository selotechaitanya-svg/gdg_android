# Tidemark Video Recording Script (5–10 Minutes)

This script provides an exact minute-by-minute walkthrough for **Chaitanya Selote** to record the graded recruitment demo video.

---

## 0. Pre-Recording Checklist (Complete Before Hitting Record)
- [ ] **Device & Tooling**: Phone or Android Emulator running API 34/35.
- [ ] **Do Not Disturb**: Turn ON Do Not Disturb to block personal notifications during screen recording.
- [ ] **Permissions Granted**: Ensure Notification and Exact Alarm permissions are enabled for Tidemark.
- [ ] **Clean State or Demo Data**:
  - If starting fresh: Start with empty app to demonstrate creating habits.
  - Or load demo data via Settings → "Load demo data" beforehand if showing charts first.
- [ ] **Terminal Open**: Open terminal in project root ready to run `./gradlew :domain:test`.
- [ ] **Audio**: Clear microphone test. Speak at an even, confident pace.

---

## Minute-by-Minute Outline

### [0:00 – 1:30] Introduction & Architecture
- **Camera / Intro**: "Hello! My name is Chaitanya Selote, applying for the Android position at GDG on Campus RBU for recruitment drive 2026-27. This is **Tidemark**, a 100% offline-first habit tracker designed for high reliability, precision streak math, and guaranteed reminder delivery."
- **Show Architecture Diagram (or explain code structure)**:
  - Explain the multi-module separation:
    - `:domain`: Pure Kotlin/JVM. Zero Android dependencies. Contains domain models, the streak calculation engine, rate statistics, reminder planning, and validation rules. Runs 50+ unit tests in seconds.
    - `:app`: Android application module. Built with Jetpack Compose + Material 3, Room SQLite persistence, DataStore preferences, and AlarmManager.
  - Explain Unidirectional Data Flow (UDF) & Manual DI (`AppContainer`) avoiding reflection.
  - Highlight: "Zero cloud dependencies, no accounts, and strictly no `INTERNET` permission in the manifest."

---

### [1:30 – 4:00] Live App Demo: The Normative GDG Example
- **Open Tidemark on Phone / Emulator**:
  - Show Today Screen and "Beach Green" design theme (sand mist background, lagoon green primary, custom canvas wave header).
- **Create Habit 1 (Read)**:
  - Tap `+` in header.
  - Name: "Read".
  - Type: Countable. Unit: "pages". Target: 20. Step: 5.
  - Schedule: Daily.
  - Tap "Save".
- **Create Habit 2 (Gym)**:
  - Tap `+`.
  - Name: "Gym".
  - Type: Yes/No.
  - Schedule: Weekdays → Select Monday, Wednesday, Friday.
  - Tap "Save".
- **Log Monday (Past Day Correction Demonstration)**:
  - Tap Monday on the 14-day date strip (banner appears: "Editing Mon, ... — Back to today").
  - Log Read: Tap body, enter 12 pages (missed the 20-page goal).
  - Log Gym: Tap check ring (completed).
- **Log Tuesday**:
  - Tap Tuesday on the date strip.
  - Log Read: Quick-add or enter 20 pages (completed).
  - Note for grader: "Gym is not scheduled on Tuesday, so it doesn't appear or penalize the streak."
- **Log Wednesday**:
  - Tap Wednesday.
  - Log Read: Enter 25 pages (completed).
  - Log Gym: Tap check ring (completed).
- **Demonstrate Streaks on Wednesday Evening**:
  - Open "Read" Detail screen:
    - Current streak is **2** (Tuesday & Wednesday complete; Monday was missed at 12/20 pages).
  - Open "Gym" Detail screen:
    - Current streak is **2** (Monday & Wednesday complete; Tuesday was unscheduled and transparent).
- **The Core Test — Correct Monday's Log**:
  - Tap Monday on the calendar grid.
  - Correct Read from 12 to 20 pages. Save.
  - Return to Wednesday / Habit Detail:
  - **Show Current Streak dynamically updates from 2 to 3!**
  - "Notice how the streak engine retroactively recalculated the history in Room and updated the UI live."

---

### [4:00 – 5:30] Live Reminders & Test Alarm Demonstration
- **Show Settings Screen**:
  - Navigate to the Settings tab.
  - Point out the **"Reminder Health"** diagnostic card:
    - Shows Notification permission status (Granted) and Exact Alarm permission status (Granted).
    - Explains battery optimization tips for OEM devices (MIUI, ColorOS).
- **Demonstrate Test Reminder**:
  - Tap **"Send Test Reminder (10 s)"**.
  - Snackbar shows: "Test reminder armed! Will fire in 10 seconds."
  - Lock phone or go to home screen.
  - **10 seconds later**: Exact notification fires in the notification drawer!
  - Point out notification features:
    - High-importance channel (`habit_reminders`).
    - Title with habit name.
    - Progress-aware text ("X / Y pages today").
    - Action button: **"Mark Done"** (Bonus B2). Tap "Mark Done"—notification auto-cancels and credits the date!

---

### [5:30 – 7:00] Running Automated Tests in Terminal
- **Switch to Terminal**:
  - Run the pure domain test suite:
    ```bash
    ./gradlew :domain:test --console=plain
    ```
  - Highlight the output:
    - **50 / 50 unit tests pass in seconds.**
  - Show the specific test classes:
    - `TaskExampleTest`: Reproduces T1, T2, T3 (the GDG brief's exact example).
    - `DailyStreakTest`: Tests missed days, pending today, and leap-day boundaries.
    - `WeekdayStreakTest`: Tests transparent unscheduled weekends and bonus logs.
    - `TimesPerWeekStreakTest`: Tests ISO weeks, N-per-week trailing runs, and pro-rated first weeks.
    - `MidnightTimezoneDstTest`: Tests 23:59:59 rollover and US DST clock shifts.
    - `ReminderPlannerTest`: Tests exact next trigger planning and notification suppression when already completed.
  - Show HTML report in browser (`domain/build/reports/tests/test/index.html`).

---

### [7:00 – 8:30] Technical Challenges & How They Were Solved
Share real challenges encountered during development and how they were solved (from Git history):
1. **Challenge 1: The "Today is Pending" Edge Case**:
   - *Problem*: If a user logs into the app at 10 AM, today's habit is not yet complete. If we treated today as missed, their current streak would reset to 0 every morning!
   - *Fix*: In `StreakCalculator`, today is classified as `PENDING` if not completed. Trailing pending days are dropped from the streak run, allowing streaks to remain active throughout the day until midnight.
2. **Challenge 2: Week Goal Pro-rating**:
   - *Problem*: For `TimesPerWeek(3)`, if a habit starts on Saturday, requiring 3 completions in 2 days (Sat + Sun) unfairly doomed the user's first week.
   - *Fix*: Implemented `minOf(N, daysRemainingInWeek)` for the habit's initial week in `StatsCalculator` and `StreakCalculator`.
3. **Challenge 3: Reliable Alarms After Reboot**:
   - *Problem*: Android wipes registered `AlarmManager` alarms upon reboot.
   - *Fix*: Implemented `BootReceiver` listening for `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, and `MY_PACKAGE_REPLACED` that queries Room and reschedules all active reminders asynchronously via `goAsync()`.

---

### [8:30 – 9:00] Conclusion & Submission
- **Wrap up**:
  - "Tidemark is built to last: clean separation of concerns, zero network reliance, robust automated tests, and a calm, deliberate Beach Green design."
  - "The complete codebase, commit history, and release APK are available in the repository. Thank you for your time and consideration!"
