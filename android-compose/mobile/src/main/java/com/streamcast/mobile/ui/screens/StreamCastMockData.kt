package com.streamcast.mobile.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.TempleHindu
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import com.streamcast.shared.model.CastProtocol
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory
import com.streamcast.shared.model.TvCapabilities
import com.streamcast.shared.model.TvDevice

data class CategoryShortcut(
    val category: ChannelCategory,
    val title: String,
    val channelCountText: String,
    val icon: ImageVector
)

data class LiveChannelItem(
    val channel: Channel,
    val currentShow: String,
    val viewerCountText: String,
    val bitrateText: String,
    val codecText: String,
    val resolution: String,
    val uptimePercent: String,
    val isFavorite: Boolean = false,
    val channelNumber: String = "CH 001"
)

object StreamCastMockData {
    val ActiveConnectedTv = TvDevice(
        id = "living_room_tv",
        name = "Living Room TV",
        deviceType = "Google TV 4K",
        protocol = CastProtocol.GOOGLE_TV_CAST,
        ipAddress = "192.168.1.105",
        capabilities = TvCapabilities(
            power = true,
            voiceSearch = true,
            volumeControl = true,
            numericKeypad = true,
            textInput = true
        )
    )

    val AvailableDevices = listOf(
        ActiveConnectedTv,
        TvDevice(
            id = "bedroom_chromecast",
            name = "Bedroom Chromecast",
            deviceType = "Chromecast HD",
            protocol = CastProtocol.GOOGLE_CAST,
            ipAddress = "192.168.1.112",
            capabilities = TvCapabilities(
                power = true,
                voiceSearch = false,
                volumeControl = true,
                numericKeypad = false,
                textInput = false
            )
        ),
        TvDevice(
            id = "android_tv_box",
            name = "Android TV Shield",
            deviceType = "NVIDIA Shield TV",
            protocol = CastProtocol.ANDROID_TV_REMOTE_V2,
            ipAddress = "192.168.1.140",
            capabilities = TvCapabilities(
                power = true,
                voiceSearch = true,
                volumeControl = true,
                numericKeypad = true,
                textInput = true
            )
        )
    )

    val CategoryShortcuts = listOf(
        CategoryShortcut(ChannelCategory.NEWS, "News", "8 channels", Icons.Rounded.Newspaper),
        CategoryShortcut(ChannelCategory.ENTERTAINMENT, "Entertainment", "8 channels", Icons.Rounded.TheaterComedy),
        CategoryShortcut(ChannelCategory.MOVIES, "Movies", "6 channels", Icons.Rounded.Movie),
        CategoryShortcut(ChannelCategory.SPORTS, "Sports", "5 channels", Icons.Rounded.SportsCricket),
        CategoryShortcut(ChannelCategory.MUSIC, "Music", "5 channels", Icons.Rounded.MusicNote),
        CategoryShortcut(ChannelCategory.REGIONAL, "Regional", "6 channels", Icons.Rounded.Translate),
        CategoryShortcut(ChannelCategory.DEVOTIONAL, "Devotional", "4 channels", Icons.Rounded.TempleHindu),
        CategoryShortcut(ChannelCategory.KIDS, "Kids", "4 channels", Icons.Rounded.ChildCare)
    )

    // Complete list of Indian IPTV channels with distinct live stream URLs
    val Channels = listOf(
        // NEWS (8 channels)
        LiveChannelItem(
            channel = Channel(
                id = "dd_national_hd",
                name = "DD National HD",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                category = ChannelCategory.NEWS,
                language = "Hindi / English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Mid-Day Special Broadcast",
            viewerCountText = "12.4k watching",
            bitrateText = "5.2 Mbps",
            codecText = "HLS v7 • Direct H/W",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.4%",
            isFavorite = true,
            channelNumber = "CH 001"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "aaj_tak_hd",
                name = "Aaj Tak HD",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                category = ChannelCategory.NEWS,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Halla Bol Prime Edition",
            viewerCountText = "8.9k watching",
            bitrateText = "4.8 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.9%",
            isFavorite = false,
            channelNumber = "CH 002"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sansad_tv",
                name = "Sansad TV",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                category = ChannelCategory.NEWS,
                language = "Hindi / English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Lok Sabha: Question Hour",
            viewerCountText = "5.1k watching",
            bitrateText = "4.2 Mbps",
            codecText = "H.265 / HEVC",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.1%",
            isFavorite = false,
            channelNumber = "CH 003"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "india_today",
                name = "India Today",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                category = ChannelCategory.NEWS,
                language = "English",
                quality = "HD",
                isLive = true
            ),
            currentShow = "Newstrack at 8",
            viewerCountText = "6.7k watching",
            bitrateText = "4.5 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1280x720 @ 60p",
            uptimePercent = "98.7%",
            isFavorite = false,
            channelNumber = "CH 004"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "abp_news_hd",
                name = "ABP News HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                category = ChannelCategory.NEWS,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Master Stroke Special Edition",
            viewerCountText = "7.3k watching",
            bitrateText = "4.6 Mbps",
            codecText = "H.265 / HEVC",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "98.5%",
            isFavorite = true,
            channelNumber = "CH 005"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "republic_bharat",
                name = "Republic Bharat",
                streamUrl = "https://d35j504z0x1be2.cloudfront.net/live/r_bharat/playlist.m3u8",
                category = ChannelCategory.NEWS,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Super 50 Breaking News",
            viewerCountText = "4.7k watching",
            bitrateText = "4.4 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.1%",
            isFavorite = false,
            channelNumber = "CH 006"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "ndtv_24x7",
                name = "NDTV 24x7",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                category = ChannelCategory.NEWS,
                language = "English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Left, Right & Centre",
            viewerCountText = "5.8k watching",
            bitrateText = "4.9 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.0%",
            isFavorite = false,
            channelNumber = "CH 007"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "news18_india",
                name = "News18 India",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                category = ChannelCategory.NEWS,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Aar Paar Prime Debate",
            viewerCountText = "6.1k watching",
            bitrateText = "4.3 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.4%",
            isFavorite = false,
            channelNumber = "CH 008"
        ),

        // ENTERTAINMENT (8 channels)
        LiveChannelItem(
            channel = Channel(
                id = "star_plus_hd",
                name = "Star Plus HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Anupamaa Drama Episode",
            viewerCountText = "14.1k watching",
            bitrateText = "5.0 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "99.2%",
            isFavorite = true,
            channelNumber = "CH 009"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "zee_tv_hd",
                name = "Zee TV HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Kumkum Bhagya Night Show",
            viewerCountText = "10.8k watching",
            bitrateText = "4.7 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.8%",
            isFavorite = false,
            channelNumber = "CH 010"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sony_tv_hd",
                name = "Sony Entertainment HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "The Kapil Sharma Show Classics",
            viewerCountText = "16.4k watching",
            bitrateText = "5.3 Mbps",
            codecText = "Dolby 2.0",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.5%",
            isFavorite = true,
            channelNumber = "CH 011"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "colors_tv_hd",
                name = "Colors TV HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback2012.mp4",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Bigg Boss Special Highlights",
            viewerCountText = "18.2k watching",
            bitrateText = "5.1 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "99.0%",
            isFavorite = false,
            channelNumber = "CH 012"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sab_tv_hd",
                name = "SAB TV HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Taarak Mehta Ka Ooltah Chashmah",
            viewerCountText = "22.5k watching",
            bitrateText = "4.9 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.7%",
            isFavorite = true,
            channelNumber = "CH 013"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "star_bharat_hd",
                name = "Star Bharat HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "RadhaKrishn Mythological Drama",
            viewerCountText = "8.2k watching",
            bitrateText = "4.5 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.6%",
            isFavorite = false,
            channelNumber = "CH 014"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "dangal_tv",
                name = "Dangal TV",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Hindi",
                quality = "720p",
                isLive = true
            ),
            currentShow = "Nath Zewar Ya Zanjeer",
            viewerCountText = "9.1k watching",
            bitrateText = "3.9 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1280x720 @ 50p",
            uptimePercent = "98.3%",
            isFavorite = false,
            channelNumber = "CH 015"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sun_tv",
                name = "Sun TV HD",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                category = ChannelCategory.ENTERTAINMENT,
                language = "Tamil",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Kayal Mega Serial",
            viewerCountText = "19.8k watching",
            bitrateText = "5.5 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.4%",
            isFavorite = false,
            channelNumber = "CH 016"
        ),

        // MOVIES (6 channels)
        LiveChannelItem(
            channel = Channel(
                id = "sony_max_hd",
                name = "Sony MAX HD",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                category = ChannelCategory.MOVIES,
                language = "Hindi Movie",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Blockbuster South Movie Hindi Dub",
            viewerCountText = "11.2k watching",
            bitrateText = "5.4 Mbps",
            codecText = "Dolby 5.1 Surround",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.0%",
            isFavorite = true,
            channelNumber = "CH 017"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "star_gold_hd",
                name = "Star Gold HD",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                category = ChannelCategory.MOVIES,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Singham Returns Full Movie",
            viewerCountText = "13.5k watching",
            bitrateText = "5.2 Mbps",
            codecText = "Dolby 5.1",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.1%",
            isFavorite = false,
            channelNumber = "CH 018"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "zee_cinema_hd",
                name = "Zee Cinema HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                category = ChannelCategory.MOVIES,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Sooryavansham Special Telecast",
            viewerCountText = "15.0k watching",
            bitrateText = "5.0 Mbps",
            codecText = "Dolby 2.0",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "99.3%",
            isFavorite = false,
            channelNumber = "CH 019"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "colors_cineplex",
                name = "Colors Cineplex",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                category = ChannelCategory.MOVIES,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "KGF Chapter 2 Hindi Premiere",
            viewerCountText = "17.4k watching",
            bitrateText = "5.6 Mbps",
            codecText = "Dolby 5.1",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.6%",
            isFavorite = true,
            channelNumber = "CH 020"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "zee_anmol_cinema",
                name = "Zee Anmol Cinema",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                category = ChannelCategory.MOVIES,
                language = "Hindi",
                quality = "720p",
                isLive = true
            ),
            currentShow = "90s Superhit Action Night",
            viewerCountText = "6.5k watching",
            bitrateText = "3.8 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1280x720 @ 50p",
            uptimePercent = "98.2%",
            isFavorite = false,
            channelNumber = "CH 021"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "utv_movies",
                name = "UTV Movies",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                category = ChannelCategory.MOVIES,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Bollywood Retro Hits",
            viewerCountText = "7.9k watching",
            bitrateText = "4.7 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "98.7%",
            isFavorite = false,
            channelNumber = "CH 022"
        ),

        // SPORTS (5 channels)
        LiveChannelItem(
            channel = Channel(
                id = "dd_sports_hd",
                name = "DD Sports HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                category = ChannelCategory.SPORTS,
                language = "Hindi / English",
                quality = "HD",
                isLive = true
            ),
            currentShow = "Ranji Trophy Semi-Final Day 3",
            viewerCountText = "15.8k watching",
            bitrateText = "6.1 Mbps",
            codecText = "60 FPS Sports",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.6%",
            isFavorite = true,
            channelNumber = "CH 023"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "star_sports_1_hd",
                name = "Star Sports 1 HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                category = ChannelCategory.SPORTS,
                language = "English / Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "India vs Australia Test Match",
            viewerCountText = "34.2k watching",
            bitrateText = "6.8 Mbps",
            codecText = "60 FPS Dolby",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.9%",
            isFavorite = true,
            channelNumber = "CH 024"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sony_sports_ten1",
                name = "Sony Sports Ten 1 HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                category = ChannelCategory.SPORTS,
                language = "English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "UEFA Champions League Live",
            viewerCountText = "21.0k watching",
            bitrateText = "6.2 Mbps",
            codecText = "60 FPS",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.5%",
            isFavorite = false,
            channelNumber = "CH 025"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sports18_1_hd",
                name = "Sports18 1 HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                category = ChannelCategory.SPORTS,
                language = "English / Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "ISL Football Championship",
            viewerCountText = "12.3k watching",
            bitrateText = "5.9 Mbps",
            codecText = "60 FPS",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.1%",
            isFavorite = false,
            channelNumber = "CH 026"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "eurosport_india",
                name = "Eurosport India",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback2012.mp4",
                category = ChannelCategory.SPORTS,
                language = "English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "MotoGP World Tour",
            viewerCountText = "8.4k watching",
            bitrateText = "5.5 Mbps",
            codecText = "60 FPS",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "98.9%",
            isFavorite = false,
            channelNumber = "CH 027"
        ),

        // MUSIC (5 channels)
        LiveChannelItem(
            channel = Channel(
                id = "b4u_music_hd",
                name = "B4U Music HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                category = ChannelCategory.MUSIC,
                language = "Hindi Pop",
                quality = "HD",
                isLive = true
            ),
            currentShow = "Evergreen Bollywood Beats",
            viewerCountText = "3.8k watching",
            bitrateText = "3.8 Mbps",
            codecText = "Dolby 2.0 Audio",
            resolution = "1280x720 @ 60p",
            uptimePercent = "97.8%",
            isFavorite = false,
            channelNumber = "CH 028"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "9xm_music",
                name = "9XM Music",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                category = ChannelCategory.MUSIC,
                language = "Hindi Pop",
                quality = "720p",
                isLive = true
            ),
            currentShow = "Hak Se Hits Chartbusters",
            viewerCountText = "4.3k watching",
            bitrateText = "3.8 Mbps",
            codecText = "AAC / Stereo",
            resolution = "1280x720 @ 50p",
            uptimePercent = "97.8%",
            isFavorite = true,
            channelNumber = "CH 029"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "mtv_india",
                name = "MTV India HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                category = ChannelCategory.MUSIC,
                language = "Hindi / English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "MTV Roadies & Music Top 10",
            viewerCountText = "9.7k watching",
            bitrateText = "4.9 Mbps",
            codecText = "Dolby 2.0",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.0%",
            isFavorite = true,
            channelNumber = "CH 030"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "zing_music",
                name = "Zing Music",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                category = ChannelCategory.MUSIC,
                language = "Hindi Pop",
                quality = "720p",
                isLive = true
            ),
            currentShow = "Pyaar Tune Kya Kiya Hits",
            viewerCountText = "3.4k watching",
            bitrateText = "3.6 Mbps",
            codecText = "Stereo AAC",
            resolution = "1280x720 @ 50p",
            uptimePercent = "97.5%",
            isFavorite = false,
            channelNumber = "CH 031"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "mh1_music",
                name = "MH One Music",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                category = ChannelCategory.MUSIC,
                language = "Punjabi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Punjabi Beats & Bhangra Top 20",
            viewerCountText = "5.2k watching",
            bitrateText = "4.4 Mbps",
            codecText = "Dolby 2.0",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "98.6%",
            isFavorite = false,
            channelNumber = "CH 032"
        ),

        // REGIONAL (6 channels)
        LiveChannelItem(
            channel = Channel(
                id = "dd_bharati",
                name = "DD Bharati",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                category = ChannelCategory.REGIONAL,
                language = "Indian Heritage",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Classical Sitar Recital",
            viewerCountText = "950 watching",
            bitrateText = "4.9 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.3%",
            isFavorite = false,
            channelNumber = "CH 033"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "dd_yadagiri",
                name = "DD Yadagiri",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                category = ChannelCategory.REGIONAL,
                language = "Telugu",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Telangana Cultural Heritage",
            viewerCountText = "2.1k watching",
            bitrateText = "4.1 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.7%",
            isFavorite = false,
            channelNumber = "CH 034"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "dd_chandana",
                name = "DD Chandana",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                category = ChannelCategory.REGIONAL,
                language = "Kannada",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Karnataka Folk Music & News",
            viewerCountText = "1.8k watching",
            bitrateText = "4.0 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.5%",
            isFavorite = false,
            channelNumber = "CH 035"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "dd_punjabi",
                name = "DD Punjabi",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                category = ChannelCategory.REGIONAL,
                language = "Punjabi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Virasat Cultural Bulletin",
            viewerCountText = "3.1k watching",
            bitrateText = "4.3 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "98.9%",
            isFavorite = false,
            channelNumber = "CH 036"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "dd_bangla",
                name = "DD Bangla",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                category = ChannelCategory.REGIONAL,
                language = "Bengali",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Kolkata Evening News Bulletin",
            viewerCountText = "2.9k watching",
            bitrateText = "4.2 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "98.8%",
            isFavorite = false,
            channelNumber = "CH 037"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "dd_sahyadri",
                name = "DD Sahyadri",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                category = ChannelCategory.REGIONAL,
                language = "Marathi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Amchi Mumbai Mid-Day Special",
            viewerCountText = "3.5k watching",
            bitrateText = "4.4 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.0%",
            isFavorite = false,
            channelNumber = "CH 038"
        ),

        // DEVOTIONAL (4 channels)
        LiveChannelItem(
            channel = Channel(
                id = "aastha_tv",
                name = "Aastha TV",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                category = ChannelCategory.DEVOTIONAL,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Live Aarti from Haridwar",
            viewerCountText = "14.3k watching",
            bitrateText = "4.6 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.6%",
            isFavorite = true,
            channelNumber = "CH 039"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sanskar_tv",
                name = "Sanskar TV",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                category = ChannelCategory.DEVOTIONAL,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Vedic Pravachan & Bhajan",
            viewerCountText = "11.7k watching",
            bitrateText = "4.4 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "99.4%",
            isFavorite = false,
            channelNumber = "CH 040"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "sadhna_tv",
                name = "Sadhna TV",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                category = ChannelCategory.DEVOTIONAL,
                language = "Hindi",
                quality = "720p",
                isLive = true
            ),
            currentShow = "Live Temple Broadcast",
            viewerCountText = "5.2k watching",
            bitrateText = "3.7 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1280x720 @ 50p",
            uptimePercent = "98.5%",
            isFavorite = false,
            channelNumber = "CH 041"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "peace_tv",
                name = "Peace TV",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback2012.mp4",
                category = ChannelCategory.DEVOTIONAL,
                language = "English / Urdu",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Islamic Spiritual Discourse",
            viewerCountText = "4.8k watching",
            bitrateText = "4.2 Mbps",
            codecText = "H.264 / AAC",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "98.9%",
            isFavorite = false,
            channelNumber = "CH 042"
        ),

        // KIDS (4 channels)
        LiveChannelItem(
            channel = Channel(
                id = "cartoon_network_india",
                name = "Cartoon Network India",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                category = ChannelCategory.KIDS,
                language = "Hindi / English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Ben 10 & Roll No 21",
            viewerCountText = "16.8k watching",
            bitrateText = "5.2 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.5%",
            isFavorite = true,
            channelNumber = "CH 043"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "pogo_tv",
                name = "Pogo TV",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                category = ChannelCategory.KIDS,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Chhota Bheem Special",
            viewerCountText = "19.2k watching",
            bitrateText = "5.0 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 50p",
            uptimePercent = "99.4%",
            isFavorite = true,
            channelNumber = "CH 044"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "nickelodeon_india",
                name = "Nickelodeon India",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                category = ChannelCategory.KIDS,
                language = "Hindi / English",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Motu Patlu Hilarious Action",
            viewerCountText = "18.5k watching",
            bitrateText = "5.1 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.6%",
            isFavorite = false,
            channelNumber = "CH 045"
        ),
        LiveChannelItem(
            channel = Channel(
                id = "hungama_tv",
                name = "Hungama TV",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                category = ChannelCategory.KIDS,
                language = "Hindi",
                quality = "1080p",
                isLive = true
            ),
            currentShow = "Doraemon & Shinchan Non-stop",
            viewerCountText = "24.1k watching",
            bitrateText = "5.3 Mbps",
            codecText = "H.264 / Stereo",
            resolution = "1920x1080 @ 60p",
            uptimePercent = "99.8%",
            isFavorite = true,
            channelNumber = "CH 046"
        )
    )

    fun getChannelById(id: String): LiveChannelItem {
        return Channels.firstOrNull { it.channel.id == id } ?: Channels.first()
    }
}
