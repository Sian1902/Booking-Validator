package model

import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val venueId: String,
    val roomId: String,
    val startTime: Instant,
    val endTime: Instant,
    val customerEmail: String
)
