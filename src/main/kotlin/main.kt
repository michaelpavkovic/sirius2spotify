import argparsing.ProgramArgs
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.string
import com.xenomachina.argparser.ArgParser
import sirius.SiriusXmDataFetcher
import spotify.Spotify
import spotify.SpotifyAuth
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.experimental.runBlocking
import java.lang.Integer.parseInt
import java.util.*

fun main(args: Array<String>) {
    val env = dotenv {
        directory = "/"
    }

    // Parse arguments passed in, exit if one or more is invalid
    val arguments = ArgParser(args).parseInto(::ProgramArgs)
    arguments.write()

    // TODO: Use refresh tokens
    val auth = SpotifyAuth()
    var token: String? = null
    runBlocking {
        token = auth.getToken()
    }

    print("Server finished")

    if (token != null) {
        val json: JsonObject = Parser().parse(StringBuilder(token)) as JsonObject

        val tokenString = json.string("access_token")

        if (tokenString != null) {
            val songs = SiriusXmDataFetcher(parseInt(arguments.time)).fetch(parseInt(arguments.channel))

            val spotify = Spotify(tokenString)

            val uris = ArrayList<String>()
            songs.forEach {
                val uri = spotify.getUri(it.artist, it.title)

                if (uri != null) uris.add(uri)
            }

            if (arguments.spotifyUsername != null && arguments.spotifyPlaylistId != null) {
                val existing: List<String> =
                        if (!arguments.allowDuplicates) spotify.getTracksForPlaylist(arguments.spotifyPlaylistId!!).toList()
                        else ArrayList()

                val segments = Spotify.getSegmentsFor(uris, arguments.allowDuplicates, existing)

                spotify.addSegmentsToPlaylist(segments,
                        arguments.spotifyUsername!!,
                        arguments.spotifyPlaylistId!!)
            } else {
                println("Cannot add songs to playlist, username or playlist not provided")
            }
            // chill:    2YdDHdY7ApTdZ0IhR9a4sf
            // bpm:      13mXb1GdVzjbEIcFsYr8Tq
            // 80s on 8: 2rbPTorM5KlcAh3N2vzt5A
        } else {
            println("Error authenticating Spotify account access")
        }
    }
}
