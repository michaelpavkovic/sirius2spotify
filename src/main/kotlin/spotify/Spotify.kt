package spotify

import com.beust.klaxon.*
import internet.HttpClient
import java.io.IOException

class Spotify(private val token: String) {
    private val httpClient = HttpClient()

    companion object {
        fun getSegmentsFor(tracks: List<String>, allowDuplicates: Boolean, existingTracks: List<String>): List<String> {
            var urisToAdd = tracks

            if (!allowDuplicates) {
                val temp = HashSet<String>()
                temp.addAll(tracks)

                urisToAdd = temp.minus(existingTracks).toList()
            }

            val segments = ArrayList<String>()

            var current = 0
            segments.add("")
            urisToAdd.forEach {
                segments[current] += "$it,"

                if (segments[current].length > 1000) {
                    current++
                    segments.add("")
                }
            }

            return segments
        }
    }

    fun getUri(artist: String, title: String): String? {
        return try {
            val result = httpClient.get("https://api.spotify.com/v1/search",
                    urlParams = mapOf("q" to "$artist $title", "type" to "track", "limit" to "1"),
                    headers = mapOf("Authorization" to "Bearer $token"))
            Thread.sleep(500)

            val json: JsonObject = Parser().parse(StringBuilder(result)) as JsonObject

            val items = json.obj("tracks")?.array<JsonObject>("items")

            if (items != null && items.isNotEmpty()) items[0].string("uri") else null
        } catch (e: IOException) {
            null
        }
    }

    fun addSegmentsToPlaylist(segments: List<String>, username: String, playlistId: String) {
        println("Adding to playlist...")

        // Reverse and add in correct order
        segments.asReversed().forEach {
            httpClient.post("https://api.spotify.com/v1/users/$username/playlists/$playlistId/tracks",
                    urlParams = mapOf("uris" to it.substring(0, it.length - 1 /* remove last ',' */),
                            "position" to "0"),
                    headers = mapOf("Authorization" to "Bearer $token"))
        }

        println("Done")
    }

    fun getTracksForPlaylist(id: String): Set<String> {
        val set = HashSet<String>()

        val statsResult = httpClient.get("https://api.spotify.com/v1/playlists/$id/tracks",
                urlParams = mapOf("fields" to "total,limit"),
                headers = mapOf("Authorization" to "Bearer $token"))
        val statsJson = Parser().parse(StringBuilder(statsResult)) as JsonObject

        val total = statsJson.int("total")!!
        val limit = statsJson.int("limit")!!



        var offset = 0
        do {
            // Get present Spotify track IDs
            val result = httpClient.get("https://api.spotify.com/v1/playlists/$id/tracks",
                    urlParams = mapOf("fields" to "items(track.id)",
                            "offset" to "$offset"),
                    headers = mapOf("Authorization" to "Bearer $token"))

            val json: JsonObject = Parser().parse(StringBuilder(result)) as JsonObject
            json.array<JsonObject>("items")?.forEach {
                val trackId = it.obj("track")?.string("id")

                if (trackId != null) {
                    set.add("spotify:track:$trackId")
                }
            }

            offset += limit
        } while (offset < total)


        return set
    }
}