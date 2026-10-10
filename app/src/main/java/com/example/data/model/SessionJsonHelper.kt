package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object SessionJsonHelper {

    /**
     * Exports a list of [ConferenceSession] to a formatted JSON string.
     */
    fun exportToJson(sessions: List<ConferenceSession>): String {
        val array = JSONArray()
        for (session in sessions) {
            val obj = JSONObject().apply {
                put("title", session.title)
                put("speaker", session.speaker)
                if (session.speakerRole.isNotBlank()) {
                    put("speakerRole", session.speakerRole)
                }
                put("date", session.date)
                put("startTime", session.startTime)
                put("endTime", session.endTime)
                put("locationName", session.locationName)
                if (session.address.isNotBlank()) {
                    put("address", session.address)
                }
                put("latitude", session.latitude)
                put("longitude", session.longitude)
                if (session.colorHex.isNotBlank()) {
                    put("colorHex", session.colorHex)
                }
                if (session.description.isNotBlank()) {
                    put("description", session.description)
                }
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    /**
     * Parses a JSON string into a list of [ConferenceSession].
     * Supports either a JSON array root `[...]` or a JSON object with `"sessions"` array `{ "sessions": [...] }`.
     */
    fun importFromJson(jsonString: String): Result<List<ConferenceSession>> {
        return try {
            val cleanJson = jsonString.trim()
            if (cleanJson.isBlank()) {
                return Result.failure(IllegalArgumentException("JSON content is empty"))
            }

            val array: JSONArray = if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else if (cleanJson.startsWith("{")) {
                val root = JSONObject(cleanJson)
                if (root.has("sessions")) {
                    root.getJSONArray("sessions")
                } else {
                    return Result.failure(IllegalArgumentException("JSON object must contain a 'sessions' array"))
                }
            } else {
                return Result.failure(IllegalArgumentException("Invalid JSON format. Expected JSON array or object."))
            }

            val sessions = mutableListOf<ConferenceSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.optString("title", "").trim()
                val speaker = obj.optString("speaker", "").trim()
                val date = obj.optString("date", "").trim()
                val startTime = obj.optString("startTime", "09:00").trim()
                val endTime = obj.optString("endTime", "10:00").trim()
                val locationName = obj.optString("locationName", "Venue").trim()

                if (title.isBlank()) {
                    continue // Skip sessions without a title
                }

                val session = ConferenceSession(
                    id = 0L,
                    title = title,
                    speaker = if (speaker.isNotBlank()) speaker else "TBD",
                    speakerRole = obj.optString("speakerRole", ""),
                    date = if (date.isNotBlank()) date else LocalDate.now().toString(),
                    startTime = startTime,
                    endTime = endTime,
                    locationName = locationName,
                    address = obj.optString("address", ""),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    colorHex = obj.optString("colorHex", ""),
                    track = obj.optString("track", ""),
                    description = obj.optString("description", "")
                )
                sessions.add(session)
            }

            if (sessions.isEmpty()) {
                Result.failure(IllegalArgumentException("No valid sessions found in JSON"))
            } else {
                Result.success(sessions)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
