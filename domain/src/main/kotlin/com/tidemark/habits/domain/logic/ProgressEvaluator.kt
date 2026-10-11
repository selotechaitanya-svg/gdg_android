package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.Habit

/**
 * Pure evaluator for habit progress and completion determination.
 */
object ProgressEvaluator {
    /**
     * Returns true if [value] meets or exceeds [habit]'s target.
     * Note: A non-positive value is never complete.
     */
    fun isComplete(habit: Habit, value: Int): Boolean {
        if (value <= 0) return false
        return value >= habit.target
    }
}
