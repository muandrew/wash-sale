package com.muandrew.forecast.model

enum class TimeMode(val displayName: String) {
    ENTITY_AGE("Entity Age"),
    CALENDAR_YEAR("Calendar Year")
}

data class Entity(
    val id: String,
    val name: String,
    val birthYear: Int, // e.g. 1996
    val isPrimary: Boolean = false,
    val retirementAge: Int = 65,
    val lifeExpectancy: Int = 90
) {
    fun ageInYear(calendarYear: Int): Int = calendarYear - birthYear
    fun yearAtAge(age: Int): Int = birthYear + age
}
