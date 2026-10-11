package com.tidemark.habits.ui.editor

import com.tidemark.habits.MainCoroutineRule
import com.tidemark.habits.TestTimeProvider
import com.tidemark.habits.data.FakeHabitRepository
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitEditorViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var repository: FakeHabitRepository
    private lateinit var timeProvider: TestTimeProvider

    @Before
    fun setUp() {
        repository = FakeHabitRepository()
        timeProvider = TestTimeProvider(
            currentInstant = Instant.parse("2026-10-12T10:00:00Z")
        )
    }

    @Test
    fun `new habit form is invalid when name is blank`() = runTest {
        val viewModel = HabitEditorViewModel(
            habitId = null,
            prefillName = "",
            repository = repository,
            timeProvider = timeProvider,
            onScheduleReminder = {},
            onCancelReminder = {}
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isValid)

        viewModel.updateName("Morning Walk")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isValid)
    }

    @Test
    fun `saveHabit persists valid habit and invokes onComplete`() = runTest {
        val viewModel = HabitEditorViewModel(
            habitId = null,
            prefillName = "Gym",
            repository = repository,
            timeProvider = timeProvider,
            onScheduleReminder = {},
            onCancelReminder = {}
        )
        advanceUntilIdle()

        var savedId: Long? = null
        viewModel.saveHabit { id -> savedId = id }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        val habitInRepo = repository.getHabit(savedId ?: 0L)
        assertEquals("Gym", habitInRepo?.name)
    }

    @Test
    fun `changing type on existing habit triggers confirmation prompt`() = runTest {
        val existing = Habit(
            id = 1,
            name = "Reading",
            type = HabitType.YES_NO,
            schedule = Schedule.Daily,
            target = 1,
            unit = null,
            step = 1,
            startDate = LocalDate.of(2026, 10, 1),
            createdAt = timeProvider.now()
        )
        repository.saveHabit(existing)

        val viewModel = HabitEditorViewModel(
            habitId = 1,
            prefillName = null,
            repository = repository,
            timeProvider = timeProvider,
            onScheduleReminder = {},
            onCancelReminder = {}
        )
        advanceUntilIdle()

        viewModel.requestTypeChange(HabitType.COUNTABLE)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showTypeChangeConfirmation)
        assertEquals(HabitType.COUNTABLE, viewModel.uiState.value.pendingTypeChange)

        viewModel.confirmTypeChange()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showTypeChangeConfirmation)
        assertEquals(HabitType.COUNTABLE, viewModel.uiState.value.type)
    }
}
