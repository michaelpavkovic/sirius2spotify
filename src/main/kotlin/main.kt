import sirius.SiriusXmDataFetcher
import spotify.Spotify
import spotify.SpotifyAuth

fun main(args: Array<String>) {
    // TODO: enable different channels
    val songs = SiriusXmDataFetcher(12).fetch(53)

    // TODO: authenticate
    //    SpotifyAuth().getToken()
    val spotify = Spotify("token goes here")

    val uris = ArrayList<String>()
    songs.forEach {
        val uri = spotify.getUri(it.artist, it.title)

        if (uri != null) uris.add(uri)
    }

    // TODO: enable different playlists
    spotify.addToPlaylist(uris, "michaelminer20", "2YdDHdY7ApTdZ0IhR9a4sf")
}
