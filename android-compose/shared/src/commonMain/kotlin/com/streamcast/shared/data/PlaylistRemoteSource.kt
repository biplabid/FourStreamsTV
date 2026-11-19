package com.streamcast.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Downloads a raw M3U playlist over HTTP.
 *
 * Deliberately dependency-free (HttpURLConnection rather than OkHttp): the job
 * is a single GET of a text file, and keeping the dependency surface small
 * keeps the build simple.
 */
class PlaylistRemoteSource {

    suspend fun fetch(url: String): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty(
                    "Accept",
                    "audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*"
                )
                setRequestProperty("Accept-Encoding", "gzip")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(
                    PlaylistException("Playlist server returned HTTP $code")
                )
            }

            val stream = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }

            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                val builder = StringBuilder()
                val buffer = CharArray(DEFAULT_BUFFER_SIZE)
                var read = reader.read(buffer)
                var total = 0
                while (read >= 0) {
                    total += read
                    if (total > MAX_PLAYLIST_BYTES) {
                        return@withContext Result.failure(
                            PlaylistException("Playlist is larger than 16 MB; refusing to load.")
                        )
                    }
                    builder.appendRange(buffer, 0, read)
                    read = reader.read(buffer)
                }
                builder.toString()
            }

            if (!body.contains("#EXTINF")) {
                Result.failure(
                    PlaylistException("That URL did not return an M3U playlist (no #EXTINF entries).")
                )
            } else {
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(
                PlaylistException(e.message ?: "Could not reach the playlist server.", e)
            )
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {
                // Nothing useful to do if teardown fails.
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 20_000
        const val MAX_PLAYLIST_BYTES = 16 * 1024 * 1024
        const val USER_AGENT = "StreamCast-IPTV/1.0 (Android)"
    }
}

class PlaylistException(message: String, cause: Throwable? = null) : Exception(message, cause)
