package validator

import model.Booking
import store.BookingStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object BookingValidator {

    fun isAvailable(venueId: String, roomId: String, start: Instant, end: Instant): Boolean {
        val candidates = BookingStore.findByRoomInRange(roomId, venueId, start, end)
        return candidates.isEmpty()
    }

    fun validate(booking: Booking): Result<Booking> {
        if (booking.startTime <= Clock.System.now())
            return Result.failure(IllegalArgumentException("Booking must be in the future"))

        if (booking.startTime >= booking.endTime)
            return Result.failure(IllegalArgumentException("Start must be before end"))

        if (!isAvailable(booking.venueId, booking.roomId, booking.startTime, booking.endTime))
            return Result.failure(IllegalStateException("Room is not available for the requested time"))

        return Result.success(BookingStore.add(booking))
    }
}