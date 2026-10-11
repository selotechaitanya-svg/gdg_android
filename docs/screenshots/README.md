# Tidemark Screenshots Guide

Please capture screenshots from an emulator or physical Android device and place them in this directory (`docs/screenshots/`) using the exact filenames below:

1. `01_today_screen.png` — **Today Screen**: Shows today's scheduled habits, progress rings, top wave header, and 14-day date strip.
2. `02_quick_log_stepper.png` — **Log Stepper Sheet**: The bottom modal sheet for quick-logging/correcting progress on a countable habit.
3. `03_habits_list.png` — **Habits Overview**: The full habits list showing schedule badges ("Mon · Wed · Fri", "Every day"), reminder times, and 30-day completion rates.
4. `04_habit_editor.png` — **Habit Editor**: The create/edit form displaying type segmented buttons, weekday chips, time picker, and validation feedback.
5. `05_habit_detail_calendar.png` — **Habit Detail & Calendar**: Detail screen showing big current streak, longest streak, and the custom Monday-first DayState calendar grid.
6. `06_habit_detail_chart.png` — **12-Week Bar Chart**: Canvas-drawn weekly completion bar chart on the detail screen.
7. `07_stats_overview.png` — **Statistics Dashboard**: Aggregate metrics (7-day rate, 30-day rate, best streak) and 7-day completion chart across all habits.
8. `08_dark_theme.png` — **Night Sea Dark Theme**: Today or Detail screen captured in dark theme mode.
9. `09_reminder_notification.png` — **Notification with Action**: System status bar showing habit reminder notification with progress text and "Mark done" action button.
10. `10_settings_health.png` — **Settings & Reminder Health**: Settings screen showing the Reminder Health diagnostic card and data export/import controls.

### Quick Capture with adb
Once a device or emulator is connected:
```bash
adb exec-out screencap -p > docs/screenshots/01_today_screen.png
```
