import org.junit.Assert.assertEquals
import org.junit.Test
import spotify.Spotify

class Sirius2SpotifyTests {
    @Test fun testGetSegmentsAvoidingDuplicates() {
        val existing = arrayListOf("a", "b", "c")
        val toAdd = arrayListOf("a", "b", "d")

        val segments = Spotify.getSegmentsFor(toAdd, false, existing)
        assertEquals(arrayListOf("d,"), segments)
    }

    @Test fun testGetSegmentsWithoutAvoidingDuplicates() {
        val existing = arrayListOf("a", "b", "c")
        val toAdd = arrayListOf("a", "b", "d")

        val segments = Spotify.getSegmentsFor(toAdd, true, existing)
        assertEquals(arrayListOf("a,b,d,"), segments)
    }
}