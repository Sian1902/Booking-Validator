# Hearth Bookings — Question 1

## The Bug

The legacy validator had two functions doing the same job with different logic:

```kotlin
// isAvailable — used for actual validation
if (start <= b.end && end >= b.start) return false  // ← inclusive, flags back-to-back as conflict

// overlaps — never used by validate()
return a.start < b.end && a.end > b.start           // ← strict, correct
```

Back-to-back bookings (one ends at 13:00, the next starts at 13:00) were incorrectly flagged as conflicts by `isAvailable` because of the `<=`/`>=` operators. The fix is strict inequality throughout: a booking that starts exactly when another ends is not an overlap.

---

## Legacy Design Issues

### 1. Inconsistent overlap semantics between `isAvailable` and `overlaps`

The validator defined overlap logic twice and got it wrong one of the two times. `isAvailable` used `<=`/`>=` (inclusive), while `overlaps` used `<`/`>` (strict). Only `overlaps` was correct, but `validate()` never called it — it called `isAvailable`. This means the correct logic existed in the codebase but was completely ignored, making the bug harder to spot and fix.

**How my version avoids it:** overlap semantics are defined in exactly one place — inside `BookingStore.findByRoomInRange`. The validator calls that and checks if the result is empty. There is no second definition to drift from.

### 2. Unbounded query with no time scoping

```kotlin
val bookings = BookingRepository.findByRoom(roomId)
```

This fetches the full booking history for a room on every availability check — including every booking from years ago that can never overlap with a future request. For a venue that has been running for a year, this means potentially thousands of irrelevant past records loaded into memory on every call. Performance degrades silently as the platform grows.

**How my version avoids it:** `BookingStore.findByRoomInRange` scopes the query to only bookings that could possibly overlap the requested window:

```kotlin
fun findByRoomInRange(roomId: String, venueId: String, start: Instant, end: Instant): List<Booking> =
    bookings.filter {
        it.venueId == venueId &&
        it.roomId == roomId &&
        it.startTime < end &&
        it.endTime > start
    }
```

Past records are never loaded. In a real Postgres implementation this maps directly to a bounded `WHERE` clause.

### 3. `validate()` returns `Boolean` with no failure reason

The legacy `validate` returns `true` or `false` with no indication of why validation failed — was it a conflict? An invalid time range? The caller has no way to tell, which makes surfacing useful error messages to customers impossible.

**How my version avoids it:** `validate` returns `Result<Booking>`, carrying either the saved booking on success or a typed exception with a descriptive message on failure. The caller can branch on `isSuccess`/`isFailure` and surface the right message to the user.

---

## Additional Guards Added

Beyond fixing the core bug I added two defensive checks that a production validator should always have:

- **Past date guard** — a booking with a `startTime` in the past is rejected immediately. Silently storing invalid bookings would be a data integrity issue.
- **Zero-duration guard** — a booking where `startTime >= endTime` is rejected. The legacy validator had no protection against this.

---

## Test Coverage

| Test | What it covers |
|---|---|
| `valid booking is accepted` | Happy path |
| `two non-overlapping bookings in same room are accepted` | No false positives |
| `back-to-back bookings are not a conflict` | **The bug** |
| `same room different venue does not conflict` | Venue scoping |
| `same time different room does not conflict` | Room scoping |
| `exact same time slot is rejected` | Direct conflict |
| `partially overlapping booking is rejected` | Overlap from the right |
| `booking that overlaps the start of an existing booking is rejected` | Overlap from the left |
| `booking contained within existing booking is rejected` | Fully contained overlap |
| `booking in the past is rejected` | Past date guard |
| `booking with start after end is rejected` | Invalid range guard |
| `booking with equal start and end is rejected` | Zero duration guard |

---

## What I'd Add With More Time

- **Cancellation and rescheduling** — freeing a slot should immediately make it available for new bookings, with its own validation pass
- **Thread safety** — the in-memory store uses a plain `MutableList`; a real concurrent environment would need a thread-safe structure or optimistic locking at the DB level
- **Email format validation** — `customerEmail` is currently accepted as any string
