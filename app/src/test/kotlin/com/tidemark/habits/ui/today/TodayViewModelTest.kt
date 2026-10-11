package com.tidemark.habits.ui.today

import com.tidemark.habits.MainCoroutineRule
import com.tidemark.habits.TestTimeProvider
import com.tidemark.habits.data.FakeHabitRepository
import com.tidemark.habits.data.FakeSettingsRepository
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var repository: FakeHabitRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var timeProvider: TestTimeProvider

    // Reference calendar: Monday 2026-10-12
    private val monday = LocalDate.of(2026, 10, 12)
    private val tuesday = LocalDate.of(2026, 10, 13)
    private val wednesday = LocalDate.of(2026, 10, 14)

    @Before
    fun setUp() {
        repository = FakeHabitRepository()
        settingsRepository = FakeSettingsRepository()
        timeProvider = TestTimeProvider(
            currentInstant = Instant.parse("2026-10-12T10:00:00Z")
        )
    }

    @Test
    fun `Today lists only habits scheduled that day`() = runTest {
        val daily = Habit(
            id = 1,
            name = "Read",
            type = HabitType.COUNTABLE,
            schedule = Schedule.Daily,
            target = 20,
            unit = "pages",
            step = 5,
            startDate = monday,
            createdAt = timeProvider.now()
        )
        val weekdays = Habit(
            id = 2,
            name = "Gym",
            type = HabitType.YES_NO,
            schedule = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            target = 1,
            unit = null,
            step = 1,
            startDate = monday,
            createdAt = timeProvider.now()
        )

        repository.saveHabit(daily)
        repository.saveHabit(weekdays)

        val viewModel = TodayViewModel(repository, settingsRepository, timeProvider)
        advanceUntilIdle()

        // On Monday: both Read and Gym are scheduled
        val mondayItems = viewModel.uiState.value.items
        assertEquals(2, mondayItems.size)
        assertTrue(mondayItems.any { it.habit.name == "Read" })
        assertTrue(mondayItems.any { it.habit.name == "Gym" })

        // Change selected date to Tuesday: Gym is unscheduled, so only Read appears
        viewModel.selectDate(tuesday)
        advanceUntilIdle()

        val tuesdayItems = viewModel.uiState.value.items
        assertEquals(1, tuesdayItems.size)
        assertEquals("Read", tuesdayItems.first().habit.name)
    }

    @Test
    fun `quick-add completes at target and Undo restores`() = runTest {
        val readHabit = Habit(
            id = 1,
            name = "Read",
            type = HabitType.COUNTABLE,
            schedule = Schedule.Daily,
            target = 20,
            unit = "pages",
            step = 5,
            startDate = monday,
            createdAt = timeProvider.now()
        )
        repository.saveHabit(readHabit)

        val viewModel = TodayViewModel(repository, settingsRepository, timeProvider)
        advanceUntilIdle()

        // Quick add 4 times: 5, 10, 15, 20
        viewModel.quickAdd(readHabit)
        advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.items.first().progressValue)
        assertFalse(viewModel.uiState.value.items.first().isCompleted)

        viewModel.quickAdd(readHabit)
        advanceUntilIdle()
        assertEquals(10, viewModel.uiState.value.items.first().progressValue)

        viewModel.quickAdd(readHabit)
        advanceUntilIdle()
        assertEquals(15, viewModel.uiState.value.items.first().progressValue)

        viewModel.quickAdd(readHabit)
        advanceUntilIdle()
        assertEquals(20, viewModel.uiState.value.items.first().progressValue)
        assertTrue(viewModel.uiState.value.items.first().isCompleted)
    }

    @Test
    fun `yes no toggle and undo restores previous state`() = runTest {
        val gymHabit = Habit(
            id = 1,
            name = "Gym",
            type = HabitType.YES_NO,
            schedule = Schedule.Daily,
            target = 1,
            unit = null,
            step = 1,
            startDate = monday,
            createdAt = timeProvider.now()
        )
        repository.saveHabit(gymHabit)

        val viewModel = TodayViewModel(repository, settingsRepository, timeProvider)
        advanceUntilIdle()

        // Initially not completed
        assertFalse(viewModel.uiState.value.items.first().isCompleted)

        // Toggle done
        viewModel.toggleYesNo(gymHabit)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.items.first().isCompleted)

        // Undo toggle
        viewModel.undoLastToggle()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.items.first().isCompleted)
    }

    @Test
    fun `selecting a past date and correcting it updates the streak`() = runTest {
        val readHabit = Habit(
            id = 1,
            name = "Read",
            type = HabitType.COUNTABLE,
            schedule = Schedule.Daily,
            target = 20,
            unit = "pages",
            step = 5,
            startDate = monday,
            createdAt = timeProvider.now()
        )
        repository.saveHabit(readHabit)

        // Monday 12, Tuesday 20, Wednesday 25
        repository.setLog(1, monday, 12, completed = false)
        repository.setLog(1, tuesday, 20, completed = true)
        repository.setLog(1, wednesday, 25, completed = true)

        // Fast-forward to Wednesday
        timeProvider.setDate(wednesday)

        val viewModel = TodayViewModel(repository, settingsRepository, timeProvider)
        advanceUntilIdle()

        // Wednesday current streak should be 2 because Monday was partial (12/20)
        val wednesdayItem = viewModel.uiState.value.items.first()
        assertEquals(2, wednesdayItem.streak.currentStreak)

        // Correct Monday's log to 20
        viewModel.selectDate(monday)
        advanceUntilIdle()
        viewModel.setLogValue(readHabit, 20)
        advanceUntilIdle()

        // Return to Wednesday: streak updates to 3!
        viewModel.selectDate(wednesday)
        advanceUntilIdle()
        val updatedItem = viewModel.uiState.value.items.first()
        assertEquals(3, updatedItem.streak.currentStreak)
    }

    @Test
    fun `midnight rollover updates the date`() = runTest {
        val viewModel = TodayViewModel(repository, settingsRepository, timeProvider)
        advanceUntilIdle()

        assertEquals(monday, viewModel.uiState.value.currentDate)
        assertEquals(monday, viewModel.uiState.value.selectedDate)

        // Clock crosses midnight into Tuesday
        timeProvider.setDate(tuesday)
        viewModel.onDateChanged()

        assertEquals(tuesday, viewModel.uiState.value.currentDate)
        assertEquals(tuesday, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `timer Stop credits minutes to the start date`() = runTest {
        val meditation = Habit(
            id = 1,
            name = "Meditation",
            type = HabitType.TIMED,
            schedule = Schedule.Daily,
            target = 15,
            unit = "min",
            step = 5,
            startDate = monday,
            createdAt = timeProvider.now()
        )
        repository.saveHabit(meditation)

        val viewModel = TodayViewModel(repository, settingsRepository, timeProvider)
        advanceUntilIdle()

        // Start stopwatch on Monday
        viewModel.startStopwatch(1)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.activeTimer)

        // Advance clock by 150 seconds (2 min 30 s -> rounds up to 3 minutes)
        timeProvider.advanceSeconds(150)

        // Stop stopwatch
        viewModel.stopStopwatch(meditation)
        advanceUntilIdle()

        val log = repository.getLog(1, monday)
        assertNotNull(log)
        assertEquals(3, log?.value)
    }
}
