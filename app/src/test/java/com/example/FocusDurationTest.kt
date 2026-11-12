package com.example

import com.example.data.model.focusMinutesToRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusDurationTest {
    @Test fun earlyFinishDoesNotLogTheEntirePlannedSession() {
        assertEquals(1, focusMinutesToRecord("pomodoro",1500,1490))
    }
    @Test fun naturalCompletionRecordsTheElapsedPlan() {
        assertEquals(25, focusMinutesToRecord("pomodoro",1500,0))
    }
    @Test fun stopwatchUsesElapsedTimeRatherThanRemainingTime() {
        assertEquals(2, focusMinutesToRecord("stopwatch",0,121))
    }
    @Test fun invalidOrExtremeValuesAreBoundedWithoutOverflow() {
        assertEquals(1, focusMinutesToRecord("pomodoro",0,Int.MAX_VALUE))
        assertEquals(600, focusMinutesToRecord("stopwatch",0,Int.MAX_VALUE))
    }
}
