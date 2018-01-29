package spotify

import spotifyClientId
import spotifyClientSecret
import internet.HttpClient
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.awt.Desktop
import java.net.URI
import java.util.*

class SpotifyAuth {
    private fun generateRandomString(length: Int): String {
        var text = ""
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        for (i in 0..length) {
            text += possible[(Math.random() * possible.length).toInt()]
        }
        return text
    }

    fun getToken() {
        val redirectUri = "http://localhost:8888"
        
        val stateKey = "spotify_auth_state"
        val scopes = "playlist-modify-public playlist-modify-private"

        val url = "https://accounts.spotify.com/authorize?" +
                HttpClient.queryString(mapOf("response_type" to "code",
                                    "client_id" to spotifyClientId,
                                    "scope" to scopes,
                                    "redirect_uri" to redirectUri,
                                    "state" to generateRandomString(16)))

        if (Desktop.isDesktopSupported()) {
            println(url)
            Desktop.getDesktop().browse(URI(url))
        }

        embeddedServer(Netty, 8888) {
            routing {
                get("/") {
                    println("GET on /")

                    try {
                        val code = call.request.queryParameters["code"] as String
                        val state = call.request.queryParameters["state"] as String
//                        val storedState = call.request.cookies[stateKey] as String

                        if (state == "") {
                            println("Error: state mismatch")
                            call.respondText("Error: state mismatch", ContentType.Text.Html)
                        } else {
                            val encodedClient = String(Base64.getEncoder().encode("$spotifyClientId:$spotifyClientSecret".toByteArray()))

                            val result = HttpClient().post("https://accounts.spotify.com/api/token",
                                    formData = HttpClient.queryString(mapOf("code" to code,
                                            "redirect_uri" to redirectUri,
                                            "grant_type" to "authorization_code")),
                                    headers = mapOf("Authorization" to "Basic $encodedClient"))

                            if (result.contains("access_token")) {
                                call.respondText("Spotify Login Successful! (you can close this window)",
                                        ContentType.Text.Html)
                            }
                            println(result)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }




                }
            }
        }.start(wait = true)
    }
}