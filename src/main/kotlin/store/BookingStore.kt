package store

import model.Booking
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object BookingStore {
    private val bookings = mutableListOf<Booking>()

    fun add(booking: Booking): Booking {
        bookings.add(booking)
        return booking
    }

    @OptIn(ExperimentalTime::class)
    fun findByRoomInRange(roomId: String, venueId: String, start: Instant, end: Instant): List<Booking> =
        bookings.filter {
            it.venueId == venueId &&
                    it.roomId == roomId &&
                    it.startTime < end &&
                    it.endTime > start
        }

    fun clear() { bookings.clear() }
}