package validator

import model.Booking
import store.BookingStore
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class BookingValidatorTest {

    private val venueId = "venue-1"
    private val roomId = "room-1"
    private val email = "ahmed@example.com"

    @BeforeTest
    fun setup() {
        BookingStore.clear()
    }

    @Test
    fun `valid booking is accepted`() {
        val booking = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        assertTrue(BookingValidator.validate(booking).isSuccess)
    }

    @Test
    fun `two non-overlapping bookings in same room are accepted`() {
        val first = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T09:00:00Z"),
            endTime = Instant.parse("2027-06-01T10:00:00Z"),
            customerEmail = email
        )
        val second = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T11:00:00Z"),
            endTime = Instant.parse("2027-06-01T12:00:00Z"),
            customerEmail = email
        )
        assertTrue(BookingValidator.validate(first).isSuccess)
        assertTrue(BookingValidator.validate(second).isSuccess)
    }

    @Test
    fun `back-to-back bookings are not a conflict`() {
        val first = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T12:00:00Z"),
            endTime = Instant.parse("2027-06-01T13:00:00Z"),
            customerEmail = email
        )
        val second = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T13:00:00Z"),
            endTime = Instant.parse("2027-06-01T14:00:00Z"),
            customerEmail = email
        )
        assertTrue(BookingValidator.validate(first).isSuccess)
        assertTrue(BookingValidator.validate(second).isSuccess)
    }

    @Test
    fun `same room different venue does not conflict`() {
        val first = Booking(
            venueId = "venue-1",
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        val second = Booking(
            venueId = "venue-2",
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        assertTrue(BookingValidator.validate(first).isSuccess)
        assertTrue(BookingValidator.validate(second).isSuccess)
    }

    @Test
    fun `same time different room does not conflict`() {
        val first = Booking(
            venueId = venueId,
            roomId = "room-1",
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        val second = Booking(
            venueId = venueId,
            roomId = "room-2",
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        assertTrue(BookingValidator.validate(first).isSuccess)
        assertTrue(BookingValidator.validate(second).isSuccess)
    }

    @Test
    fun `exact same time slot is rejected`() {
        val first = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        val second = first.copy(id = java.util.UUID.randomUUID().toString())
        BookingValidator.validate(first)
        assertTrue(BookingValidator.validate(second).isFailure)
    }

    @Test
    fun `partially overlapping booking is rejected`() {
        val first = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T12:00:00Z"),
            customerEmail = email
        )
        val second = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T11:00:00Z"),
            endTime = Instant.parse("2027-06-01T13:00:00Z"),
            customerEmail = email
        )
        BookingValidator.validate(first)
        assertTrue(BookingValidator.validate(second).isFailure)
    }

    @Test
    fun `booking that overlaps the start of an existing booking is rejected`() {
        val first = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T12:00:00Z"),
            customerEmail = email
        )
        val second = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T09:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        BookingValidator.validate(first)
        assertTrue(BookingValidator.validate(second).isFailure)
    }

    @Test
    fun `booking contained within existing booking is rejected`() {
        val outer = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T09:00:00Z"),
            endTime = Instant.parse("2027-06-01T17:00:00Z"),
            customerEmail = email
        )
        val inner = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T11:00:00Z"),
            customerEmail = email
        )
        BookingValidator.validate(outer)
        assertTrue(BookingValidator.validate(inner).isFailure)
    }

    @Test
    fun `booking in the past is rejected`() {
        val booking = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2020-01-01T10:00:00Z"),
            endTime = Instant.parse("2020-01-01T11:00:00Z"),
            customerEmail = email
        )
        val result = BookingValidator.validate(booking)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `booking with start after end is rejected`() {
        val booking = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T13:00:00Z"),
            endTime = Instant.parse("2027-06-01T10:00:00Z"),
            customerEmail = email
        )
        val result = BookingValidator.validate(booking)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `booking with equal start and end is rejected`() {
        val booking = Booking(
            venueId = venueId,
            roomId = roomId,
            startTime = Instant.parse("2027-06-01T10:00:00Z"),
            endTime = Instant.parse("2027-06-01T10:00:00Z"),
            customerEmail = email
        )
        val result = BookingValidator.validate(booking)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}