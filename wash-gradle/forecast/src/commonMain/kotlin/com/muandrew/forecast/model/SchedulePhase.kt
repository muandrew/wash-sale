package com.muandrew.forecast.model

data class SchedulePhase(
    val id: String = "",
    val name: String = "",
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val startAge: Int = 30,
    val endAge: Int = 90,
    val startYear: Int = 2026,
    val endYear: Int = 2086,
    val amount: Money = Money.ZERO,
    val isWithdrawal: Boolean = false
) {
    fun effectiveStartYear(entity: Entity?): Int {
        return when (timeMode) {
            TimeMode.CALENDAR_YEAR -> startYear
            TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(startAge) else startYear
        }
    }

    fun effectiveEndYear(entity: Entity?): Int {
        return when (timeMode) {
            TimeMode.CALENDAR_YEAR -> endYear
            TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(endAge) else endYear
        }
    }

    fun isApplicableInYear(calendarYear: Int, entity: Entity?): Boolean {
        val sYear = effectiveStartYear(entity)
        val eYear = effectiveEndYear(entity)
        return calendarYear in sYear..eYear
    }
}
