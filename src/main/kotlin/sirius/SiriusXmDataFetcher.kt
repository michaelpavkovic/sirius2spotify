package sirius

import internet.HttpClient
import media.Song
import java.text.SimpleDateFormat
import java.util.*

class SiriusXmDataFetcher(private val timeIntervalHours: Int) {
    private val baseUrl = "http://dogstarradio.com/search_xm_playlist.php"

    fun fetch(channel: Int): ArrayList<Song> {
        val oldestDate = Date(System.currentTimeMillis() - hoursToMillis(timeIntervalHours))
        println(oldestDate)

        val songs = ArrayList<Song>()

        val http = HttpClient()
        var html = http.get(baseUrl, urlParams = mapOf("channel" to "$channel"))
        var page = 0

        var temp: String
        temp = html.substring(html.indexOf("Artist") + 6)
        temp = temp.substring(temp.indexOf("Artist") + 6)

        var artist: String; var title: String; var day: String; var time: String

        var timeIntervalCovered = false

        //Loop through lines of HTML parsing out artists, titles, days, and times
        while(!timeIntervalCovered) {
            temp = temp.substring(temp.indexOf("</td><td>") + 9)

            if (temp.substring(0, 7) == "<a href") {    // End of page has been reached
                println("loading more")
                html = http.get(baseUrl, urlParams = mapOf("channel" to "$channel", "page" to "${++page}"))
                temp = html.substring(html.indexOf("Artist") + 6)
                temp = temp.substring(temp.indexOf("Artist") + 6)
                temp = temp.substring(temp.indexOf("</td><td>") + 9)
            }

            artist = temp.substring(0, temp.indexOf("</td><td>"))

            temp = temp.substring(temp.indexOf("</td><td>") + 9)
            title = temp.substring(0, temp.indexOf("</td><td>"))

            temp = temp.substring(temp.indexOf("</td><td>") + 9)
            day = temp.substring(0, temp.indexOf("</td><td>"))

            temp = temp.substring(temp.indexOf("</td><td>") + 9)
            time = temp.substring(0, temp.indexOf("</td><td>"))

            val songTimestamp = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").parse("$day $time")

            songs.add(Song(artist, title, songTimestamp))

            if (songTimestamp < oldestDate) {
                timeIntervalCovered = true
            }
        }
        return songs
    }

    private fun hoursToMillis(hours: Int) = hours * 60 * 60 * 1000
}