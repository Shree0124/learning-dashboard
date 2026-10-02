package com.learning.dashboard.domain.usecase

import kotlin.math.roundToInt

class CalculateProgressUseCase {

    /**
     * Calculates the integer completion percentage (0 - 100).
     *
     * @param completedCount The number of completed lessons.
     * @param totalCount The total number of lessons.
     * @return Clamped percentage between 0 and 100.
     */
    operator fun invoke(completedCount: Int, totalCount: Int): Int {
        if (totalCount <= 0) return 0
        if (completedCount <= 0) return 0
        if (completedCount >= totalCount) return 100

        val ratio = (completedCount.toDouble() / totalCount.toDouble()) * 100.0
        return ratio.roundToInt().coerceIn(0, 100)
    }
}
