package com.learning.dashboard.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateProgressUseCaseTest {

    private lateinit var useCase: CalculateProgressUseCase

    @Before
    fun setUp() {
        useCase = CalculateProgressUseCase()
    }

    @Test
    fun `invoke with zero total lessons returns zero percent`() {
        val result = useCase(completedCount = 0, totalCount = 0)
        assertEquals(0, result)
    }

    @Test
    fun `invoke with negative total lessons returns zero percent`() {
        val result = useCase(completedCount = 5, totalCount = -1)
        assertEquals(0, result)
    }

    @Test
    fun `invoke with zero completed lessons returns zero percent`() {
        val result = useCase(completedCount = 0, totalCount = 20)
        assertEquals(0, result)
    }

    @Test
    fun `invoke with all completed lessons returns 100 percent`() {
        val result = useCase(completedCount = 20, totalCount = 20)
        assertEquals(100, result)
    }

    @Test
    fun `invoke with completed exceeding total returns clamped 100 percent`() {
        val result = useCase(completedCount = 25, totalCount = 20)
        assertEquals(100, result)
    }

    @Test
    fun `invoke with fractional progress rounds correctly`() {
        // 13 out of 20 = 65%
        assertEquals(65, useCase(completedCount = 13, totalCount = 20))

        // 1 out of 3 = 33.333% -> 33%
        assertEquals(33, useCase(completedCount = 1, totalCount = 3))

        // 2 out of 3 = 66.666% -> 67%
        assertEquals(67, useCase(completedCount = 2, totalCount = 3))

        // 10 out of 16 = 62.5% -> 63% (round half to even / roundToInt) or 62/63
        val midResult = useCase(completedCount = 10, totalCount = 16)
        assertEquals(63, midResult)
    }
}
