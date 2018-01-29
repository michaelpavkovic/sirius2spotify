package internet

import java.net.URL
import java.io.InputStreamReader
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.io.DataOutputStream
import java.util.*


class HttpClient {
    fun get(url: String, headers: Map<String, String> = HashMap(), urlParams: Map<String, String> = HashMap()): String {
        return request("GET", url, headers = headers, urlParams = urlParams)

    }

    fun post(url: String, formData: String = "", headers: Map<String, String> = HashMap(), urlParams: Map<String, String> = HashMap()): String {
        return request("POST", url, formData = formData, headers = headers, urlParams = urlParams)
    }

    private fun request(method: String, url: String,
                        formData: String = "",
                        headers: Map<String, String> = HashMap(),
                        urlParams: Map<String, String> = HashMap()): String {

        val queryString = if (urlParams.isEmpty()) "" else "?${queryString(urlParams)}"
        val connection: HttpURLConnection = URL("$url$queryString").openConnection() as HttpURLConnection
        connection.requestMethod = method
        headers.forEach { key, value -> connection.setRequestProperty(key, value) }

        if (method == "POST") {
            connection.doOutput = true

            val dOutput = DataOutputStream(connection.outputStream)
            dOutput.write(formData.toByteArray())
            dOutput.close()
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder() // or StringBuffer if Java version 5+
        var line = reader.readLine()

        while (line != null) {
            response.append(line)

            line = reader.readLine()
        }
        reader.close()
        return response.toString()
    }

    companion object {
        fun queryString(urlParams: Map<String, String> = HashMap()): String {
            if (urlParams.isEmpty()) {
                return ""
            }

            val queryBuilder = StringBuilder()
            urlParams.forEach { key, value ->
                queryBuilder.append("${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}&")
            }

            return queryBuilder.toString().substring(0, queryBuilder.length - 1)    // Remove last '&'
        }
    }
}