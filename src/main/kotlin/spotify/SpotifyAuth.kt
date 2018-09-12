package spotify

import spotifyClientId
import spotifyClientSecret
import internet.HttpClient
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ShutDownUrl
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.experimental.delay
import java.awt.Desktop
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

class SpotifyAuth {
    companion object {
        private const val RESPONSE_SUCCESS = """
            <!doctype html>
            <html>

            <body>
                <header>
                    <h1>Spotify login successful!</h1>
                </header>

                <p>Spotify account connected to sirius2spotify! Click <a href="/shutdown">here</a> to continue.</p>
            </body>

            <style type="text/css">
                h1, p {
                    font-family: "Helvetica Neue", Helvetica;
                    text-align: center;
                }
            </style>
            </html>
            """

        private const val RESPONSE_FAILURE = """
            <!doctype html>
            <html>

            <body>
                <header>
                    <h1>Spotify login failed.</h1>
                </header>

                <p>Please try again by restarting sirius2spotify. Click <a href="/shutdown">here</a> to stop.</p>
            </body>

            <style type="text/css">
                h1, p {
                    font-family: "Helvetica Neue", Helvetica;
                    text-align: center;
                }
            </style>
            </html>
            """
    }

    var token: String? = null
    var server: NettyApplicationEngine? = null

    private fun generateRandomString(length: Int): String {
        var text = ""
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        for (i in 0..length) {
            text += possible[(Math.random() * possible.length).toInt()]
        }
        return text
    }

    suspend fun getToken(): String? {
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

        var failed = false

        server = embeddedServer(Netty, 8888) {
            install(ShutDownUrl.ApplicationCallFeature) {
                // The URL that will be intercepted
                shutDownUrl = "/shutdown"
                // A function that will be executed to get the exit code of the process
                exitCodeSupplier = { 0 }
            }

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
                            val encodedClient = String.toBase64("$spotifyClientId:$spotifyClientSecret")

                            val result = HttpClient().post("https://accounts.spotify.com/api/token",
                                    formData = HttpClient.queryString(mapOf("code" to code,
                                            "redirect_uri" to redirectUri,
                                            "grant_type" to "authorization_code")),
                                    headers = mapOf("Authorization" to "Basic $encodedClient"))

                            if (result.contains("access_token")) {
                                call.respondText(RESPONSE_SUCCESS, ContentType.Text.Html)
                                this@SpotifyAuth.token = result
                            } else {
                                call.respondText(RESPONSE_FAILURE, ContentType.Text.Html)
                                failed = true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.start(wait = false)

        // Warning! This is a very stupid hack and there's probably a better way
        while (token == null && !failed) {
            delay(1000)
        }

        server?.stop(0, 0, TimeUnit.SECONDS)

        return token
    }
}

fun String.Companion.toBase64(s: String): String = String(Base64.getEncoder().encode(s.toByteArray()))