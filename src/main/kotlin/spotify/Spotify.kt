package spotify

import com.beust.klaxon.*
import internet.HttpClient

class Spotify(private val token: String) {
    private val httpClient = HttpClient()

    fun getUri(artist: String, title: String): String? {
        val result = httpClient.get("https://api.spotify.com/v1/search",
                urlParams = mapOf("q" to "$artist $title", "type" to "track", "limit" to "1"),
                headers = mapOf("Authorization" to "Bearer $token"))
        Thread.sleep(500)

        val json: JsonObject = Parser().parse(StringBuilder(result)) as JsonObject

        val items = json.obj("tracks")?.array<JsonObject>("items")

        return if (items != null && items.isNotEmpty()) items[0].string("uri") else null
    }

    fun addToPlaylist(uris: ArrayList<String>, username: String, playlistId: String) {
        println("Adding to playlist...")

        val uriStrings = ArrayList<String>()

        var current = 0
        uriStrings.add("")
        uris.forEach {
            uriStrings[current] += it + ","

            if (uriStrings[current].length > 1000) {
                current++
                uriStrings.add("")
            }
        }

        // Reverse and add in correct order
        uriStrings.asReversed().forEach {
            httpClient.post("https://api.spotify.com/v1/users/$username/playlists/$playlistId/tracks",
                    urlParams = mapOf("uris" to it.substring(0, it.length - 1 /* remove last ',' */),
                            "position" to "0"),
                    headers = mapOf("Authorization" to "Bearer $token"))
        }

        println("Done")
    }
}