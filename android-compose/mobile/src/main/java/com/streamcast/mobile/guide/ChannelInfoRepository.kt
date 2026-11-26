package com.streamcast.mobile.guide

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import com.streamcast.shared.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * A short description of a channel, built from two public sources:
 *
 *  - iptv-org's channel catalogue (the same project the default playlist comes
 *    from): network, owners, launch date and website, keyed by the playlist's
 *    tvg-id, so the match is exact rather than guessed;
 *  - Wikipedia's page summary, used only when the article is plainly about a
 *    television channel, so "Aastha" never comes back as a dictionary entry.
 *
 * The catalogue is 8 MB of JSON for every channel in the world. It is
 * stream-read once a week and only the Indian entries plus anything in the
 * current playlist are kept, as a small tab-separated file.
 */
class ChannelInfoRepository(context: Context) {

    private val catalogueFile = File(context.applicationContext.filesDir, "iptv_catalogue.tsv")
    private val mutex = Mutex()

    private var catalogue: Map<String, CatalogueEntry>? = null
    private val summaries = HashMap<String, WikiSummary?>()

    private data class CatalogueEntry(
        val name: String,
        val network: String?,
        val owners: List<String>,
        val launched: String?,
        val website: String?
    )

    private data class WikiSummary(val text: String, val url: String?)

    suspend fun infoFor(channel: Channel, playlist: List<Channel>): ChannelInfo =
        withContext(Dispatchers.IO) {
            val entry = runCatching { catalogue(playlist)[catalogueId(channel)] }
                .onFailure { Log.w(TAG, "catalogue unavailable", it) }
                .getOrNull()

            val summary = runCatching { wikipediaSummary(channel, entry) }
                .onFailure { Log.d(TAG, "no summary for ${channel.name}: ${it.message}") }
                .getOrNull()

            ChannelInfo(
                network = entry?.network,
                owners = entry?.owners.orEmpty(),
                launched = entry?.launched,
                website = entry?.website,
                summary = summary?.text,
                summarySource = summary?.let { "Wikipedia" },
                summaryUrl = summary?.url
            )
        }

    private fun catalogueId(channel: Channel): String? =
        channel.tvgId?.substringBefore('@')?.ifBlank { null }

    // ------------------------------------------------------------- catalogue

    private suspend fun catalogue(playlist: List<Channel>): Map<String, CatalogueEntry> =
        mutex.withLock {
            catalogue?.let { return it }
            val stale = System.currentTimeMillis() - catalogueFile.lastModified() > CATALOGUE_TTL_MS
            if (!catalogueFile.exists() || stale) {
                runCatching { downloadCatalogue(playlist) }
                    .onFailure { Log.w(TAG, "catalogue download failed", it) }
            }
            readCatalogue().also { catalogue = it }
        }

    private fun downloadCatalogue(playlist: List<Channel>) {
        val wanted = playlist.mapNotNullTo(HashSet()) { catalogueId(it) }
        val connection = open(CATALOGUE_URL)
        val partial = File(catalogueFile.parentFile, catalogueFile.name + ".part")
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            partial.bufferedWriter().use { writer ->
                JsonReader(connection.inputStream.bufferedReader()).use { reader ->
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val row = readCatalogueRow(reader) ?: continue
                        if (row.country == "IN" || row.id in wanted) {
                            writer.write(
                                listOf(
                                    row.id, row.name, row.network.orEmpty(),
                                    row.owners.joinToString("|"),
                                    row.launched.orEmpty(), row.website.orEmpty()
                                ).joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') }
                            )
                            writer.newLine()
                        }
                    }
                    reader.endArray()
                }
            }
            if (!partial.renameTo(catalogueFile)) {
                catalogueFile.delete()
                partial.renameTo(catalogueFile)
            }
        } finally {
            connection.disconnect()
            partial.delete()
        }
    }

    private class Row(
        val id: String,
        val name: String,
        val country: String?,
        val network: String?,
        val owners: List<String>,
        val launched: String?,
        val website: String?
    )

    private fun readCatalogueRow(reader: JsonReader): Row? {
        var id: String? = null
        var name: String? = null
        var country: String? = null
        var network: String? = null
        var owners: List<String> = emptyList()
        var launched: String? = null
        var website: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                continue
            }
            when (key) {
                "id" -> id = reader.nextString()
                "name" -> name = reader.nextString()
                "country" -> country = reader.nextString()
                "network" -> network = reader.nextString()
                "launched" -> launched = reader.nextString()
                "website" -> website = reader.nextString()
                "owners" -> {
                    val list = mutableListOf<String>()
                    reader.beginArray()
                    while (reader.hasNext()) list += reader.nextString()
                    reader.endArray()
                    owners = list
                }

                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return if (id != null && name != null) {
            Row(id, name, country, network, owners, launched, website)
        } else {
            null
        }
    }

    private fun readCatalogue(): Map<String, CatalogueEntry> {
        if (!catalogueFile.exists()) return emptyMap()
        return catalogueFile.useLines { lines ->
            lines.mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 6) return@mapNotNull null
                f[0] to CatalogueEntry(
                    name = f[1],
                    network = f[2].ifBlank { null },
                    owners = f[3].split('|').filter { it.isNotBlank() },
                    launched = f[4].ifBlank { null },
                    website = f[5].ifBlank { null }
                )
            }.toMap()
        }
    }

    // ------------------------------------------------------------- wikipedia

    private fun wikipediaSummary(channel: Channel, entry: CatalogueEntry?): WikiSummary? {
        val baseName = channel.name.replace(HD_SUFFIX, "").trim()
        val cacheKey = entry?.name ?: baseName
        synchronized(summaries) {
            if (summaries.containsKey(cacheKey)) return summaries[cacheKey]
        }

        val candidates = listOfNotNull(entry?.name, baseName, "$baseName (TV channel)")
            .distinct()
        var found: WikiSummary? = null
        for (title in candidates) {
            found = fetchSummary(title)
            if (found != null) break
        }
        synchronized(summaries) { summaries[cacheKey] = found }
        return found
    }

    private fun fetchSummary(title: String): WikiSummary? {
        val path = URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "_")
        val connection = open("$WIKI_SUMMARY_URL$path?redirect=true")
        try {
            if (connection.responseCode !in 200..299) return null
            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            if (json.optString("type") == "disambiguation") return null
            val extract = json.optString("extract").trim()
            val description = json.optString("description")
            // Only accept articles that are actually about a broadcaster.
            val haystack = "$description $extract".lowercase()
            if (extract.length < 40 || TV_WORDS.none { it in haystack }) return null
            val url = json.optJSONObject("content_urls")
                ?.optJSONObject("mobile")
                ?.optString("page")
                ?.ifBlank { null }
            return WikiSummary(extract, url)
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            // Wikimedia asks API clients to identify themselves.
            setRequestProperty("User-Agent", USER_AGENT)
        }

    private companion object {
        const val TAG = "StreamCastInfo"
        const val CATALOGUE_URL = "https://iptv-org.github.io/api/channels.json"
        const val WIKI_SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/"
        const val USER_AGENT = "FourStreamsTV/1.0 (Android; channel descriptions)"
        const val CATALOGUE_TTL_MS = 7L * 24 * 60 * 60 * 1000

        val HD_SUFFIX = Regex("""\s+(HD|SD)$""", RegexOption.IGNORE_CASE)
        val TV_WORDS = listOf("television", "channel", "broadcaster")
    }
}
