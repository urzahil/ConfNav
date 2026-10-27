package rocks.urzahil.confnav.data.model

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Outcome of parsing a sessions file: the usable rows plus a human-readable reason per skipped row. */
data class ImportReport(
    val sessions: List<ConferenceSession>,
    val skipped: List<String>
) {
    /** Sessions that were kept but have no usable coordinates, so they will not appear on the map. */
    val withoutLocation: Int get() = sessions.count { !it.hasLocation }
}

/**
 * Reads and writes the ConfNav sessions JSON format (see docs/public/sessions.schema.json).
 * The web companion produces the same format.
 */
object SessionJsonHelper {

    private val moshi: Moshi = Moshi.Builder().build()
    private val anyAdapter = moshi.adapter(Any::class.java)

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT)
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT)
    private val OUTPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
    private val HEX_COLOR = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$")

    /** Strict `yyyy-MM-dd` parsing (also tolerates a missing leading zero, which is normalised). */
    fun parseDate(value: String): LocalDate? =
        try {
            LocalDate.parse(value.trim(), DATE_FORMAT)
        } catch (_: Exception) {
            null
        }

    /** Strict 24-hour `HH:mm` parsing (also tolerates `9:00`, which is normalised). */
    fun parseTime(value: String): LocalTime? =
        try {
            LocalTime.parse(value.trim(), TIME_FORMAT)
        } catch (_: Exception) {
            null
        }

    /** Returns a `#RRGGBB` string for `#RGB` / `#RRGGBB` input, or an empty string if invalid. */
    fun normalizeColor(value: String): String {
        val v = value.trim()
        if (!HEX_COLOR.matches(v)) return ""
        val expanded = if (v.length == 4) {
            "#" + v.substring(1).map { "$it$it" }.joinToString("")
        } else {
            v
        }
        return expanded.uppercase()
    }

    /** Exports a list of [ConferenceSession] to a formatted JSON string. */
    fun exportToJson(sessions: List<ConferenceSession>): String {
        val list = sessions.map { session ->
            val obj = LinkedHashMap<String, Any?>()
            obj["title"] = session.title
            obj["speaker"] = session.speaker
            if (session.speakerRole.isNotBlank()) obj["speakerRole"] = session.speakerRole
            obj["date"] = session.date
            obj["startTime"] = session.startTime
            obj["endTime"] = session.endTime
            obj["locationName"] = session.locationName
            if (session.address.isNotBlank()) obj["address"] = session.address
            obj["latitude"] = session.latitude
            obj["longitude"] = session.longitude
            if (session.colorHex.isNotBlank()) obj["colorHex"] = session.colorHex
            if (session.track.isNotBlank()) obj["track"] = session.track
            if (session.description.isNotBlank()) obj["description"] = session.description
            if (session.starred) obj["starred"] = true
            obj
        }
        return anyAdapter.indent("  ").toJson(list)
    }

    /**
     * Parses a JSON string into a list of [ConferenceSession], failing if no row is usable.
     * Use [parse] when the caller needs to know which rows were skipped and why.
     */
    fun importFromJson(jsonString: String): Result<List<ConferenceSession>> =
        parse(jsonString).mapCatching { report ->
            if (report.sessions.isEmpty()) {
                val reason = report.skipped.firstOrNull()?.let { ": $it" }.orEmpty()
                throw IllegalArgumentException("No valid sessions found in JSON$reason")
            }
            report.sessions
        }

    /**
     * Parses a JSON array, a `{ "sessions": [...] }` object, or a single session object.
     * Rows that fail validation are skipped and reported rather than silently altered.
     */
    fun parse(jsonString: String): Result<ImportReport> {
        val text = jsonString.removePrefix("\uFEFF").trim()
        if (text.isEmpty()) {
            return Result.failure(IllegalArgumentException("JSON content is empty"))
        }

        val root: Any? = try {
            anyAdapter.fromJson(text)
        } catch (e: JsonDataException) {
            return Result.failure(IllegalArgumentException("Invalid JSON: ${e.message}"))
        } catch (e: IOException) {
            return Result.failure(IllegalArgumentException("Invalid JSON: ${e.message}"))
        }

        val rows: List<*> = when {
            root is List<*> -> root
            root is Map<*, *> && root["sessions"] is List<*> -> root["sessions"] as List<*>
            root is Map<*, *> && root.containsKey("sessions") ->
                return Result.failure(IllegalArgumentException("'sessions' must be an array"))
            root is Map<*, *> && root.containsKey("title") -> listOf(root)
            root is Map<*, *> ->
                return Result.failure(IllegalArgumentException("JSON object must contain a 'sessions' array"))
            else ->
                return Result.failure(IllegalArgumentException("Invalid JSON format. Expected JSON array or object."))
        }

        val sessions = mutableListOf<ConferenceSession>()
        val skipped = mutableListOf<String>()
        rows.forEachIndexed { index, row ->
            val label = "Item #${index + 1}"
            val obj = row as? Map<*, *>
            if (obj == null) {
                skipped.add("$label is not an object")
                return@forEachIndexed
            }
            when (val result = toSession(obj)) {
                is RowResult.Ok -> sessions.add(result.session)
                is RowResult.Skip -> skipped.add("$label: ${result.reason}")
            }
        }
        return Result.success(ImportReport(sessions, skipped))
    }

    private sealed interface RowResult {
        data class Ok(val session: ConferenceSession) : RowResult
        data class Skip(val reason: String) : RowResult
    }

    private fun toSession(obj: Map<*, *>): RowResult {
        val title = obj.text("title")
        if (title.isEmpty()) return RowResult.Skip("missing title")

        val rawDate = obj.text("date")
        val date = parseDate(rawDate)
            ?: return RowResult.Skip("\"$title\" has an invalid date \"$rawDate\" (expected YYYY-MM-DD)")

        val rawStart = obj.text("startTime")
        val start = parseTime(rawStart)
            ?: return RowResult.Skip("\"$title\" has an invalid start time \"$rawStart\" (expected HH:mm)")

        val rawEnd = obj.text("endTime")
        val end = parseTime(rawEnd)
            ?: return RowResult.Skip("\"$title\" has an invalid end time \"$rawEnd\" (expected HH:mm)")

        if (!end.isAfter(start)) {
            return RowResult.Skip("\"$title\" ends at $rawEnd, which is not after its start time $rawStart")
        }

        // Coordinates are optional; a session without a valid pair is kept but not placed on the map.
        val lat = obj.number("latitude")
        val lng = obj.number("longitude")
        val validCoords = lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0
        val speaker = obj.text("speaker")
        val locationName = obj.text("locationName")

        return RowResult.Ok(
            ConferenceSession(
                id = 0L,
                title = title,
                speaker = speaker.ifEmpty { "TBD" },
                speakerRole = obj.text("speakerRole"),
                date = date.toString(),
                startTime = start.format(OUTPUT_TIME_FORMAT),
                endTime = end.format(OUTPUT_TIME_FORMAT),
                locationName = locationName.ifEmpty { "Venue" },
                address = obj.text("address"),
                latitude = if (validCoords) lat else 0.0,
                longitude = if (validCoords) lng else 0.0,
                colorHex = normalizeColor(obj.text("colorHex")),
                track = obj.text("track"),
                description = obj.text("description"),
                starred = obj.flag("starred")
            )
        )
    }

    /** Reads a value as trimmed text. JSON `null` and non-scalar values become an empty string. */
    private fun Map<*, *>.text(key: String): String =
        when (val v = this[key]) {
            is String -> v.trim()
            is Number, is Boolean -> v.toString()
            else -> ""
        }

    /** Reads a boolean, accepting `"true"` as well. Absent or anything else is false. */
    private fun Map<*, *>.flag(key: String): Boolean =
        when (val v = this[key]) {
            is Boolean -> v
            is String -> v.trim().equals("true", ignoreCase = true)
            else -> false
        }

    /** Reads a finite number, accepting numeric strings. Returns null if absent or invalid. */
    private fun Map<*, *>.number(key: String): Double? {
        val value = when (val v = this[key]) {
            is Number -> v.toDouble()
            is String -> v.trim().toDoubleOrNull()
            else -> null
        }
        return value?.takeIf { it.isFinite() }
    }
}
