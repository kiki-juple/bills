package com.disheveled.bills.model

import kotlinx.serialization.Serializable

/** Someone chipping in on the bill. */
@Serializable
data class Person(
    val id: String,
    val name: String,
) {
    /** First letter, for the circular avatar in the UI. */
    val initial: String get() = name.trim().take(1).uppercase()
}
