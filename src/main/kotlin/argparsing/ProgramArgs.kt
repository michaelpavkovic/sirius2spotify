package argparsing

import PROEPRTIES_NAME
import PROPERTIES_DIR
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

class ProgramArgs(parser: ArgParser) {
    private var properties: Properties? = null
    private val path = Paths.get(PROPERTIES_DIR, PROEPRTIES_NAME)

    init {
        // Initialise and read properties from file
        properties = Properties()

        if (!Files.exists(path)) Files.createFile(path)

        properties?.load(FileInputStream(path.toString()))

        var inputStream: FileInputStream? = null
        try {
            inputStream = FileInputStream(path.toString())
            properties?.load(inputStream)
        } catch (e: IOException) {
            println("Cannot read from $path... no persisting available")
            properties = null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    println("Problem closing input stream for argument properties file")
                }
            }
        }
    }

    /**
     * Writes arguments passed into current session into properties file
     */
    fun write() {
        // Write properties file with updated arguments
        try {
            properties?.store(FileWriter(PROEPRTIES_NAME), "sirius2spotify last used arguments")
        } catch (e: IOException) {
            println("Cannot write to $path... last used arguments not stored.")
        }
    }

    val time: String? by parser.storing("-t", "--time",
            help = "Time (in hours) in the past to start reading songs from")
            .storeInto("time", properties, true)

    val channel: String? by parser.storing("-c", "--channel",
            help = "SiriusXM channel to read from")
            .storeInto("channel", properties, true)

    val spotifyUsername: String? by parser.storing("-u", "--spotifyUsername",
            help = "Spotify user who owns outputPlaylist")
            .storeInto("spotifyUsername", properties, true)

    val spotifyPlaylistId: String? by parser.storing("-p", "--outputPlaylist",
            help = "Spotify playlist to output to")
            .storeInto("spotifyPlaylistId", properties, true)

    val allowDuplicates: Boolean by parser.flagging("-d", "--allowDuplicates",
            help = "Whether or not Spotify tracks already in playlist should be added again")

}

// Extension function to store an argument into a key in the
fun ArgParser.Delegate<String?>.storeInto(key: String, properties: Properties?, shouldDefault: Boolean): ArgParser.Delegate<String?> {
    return this
            .default(if (shouldDefault) properties?.get(key) as? String? else null)
            .addValidator {
                if (value != null && properties != null) properties[key] = value
            }
}