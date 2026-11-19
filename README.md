# StreamCast

IPTV player and Google Cast sender for live Indian television channels.

The repository holds two things:

| Path | What it is |
|---|---|
| [`android-compose/`](android-compose/) | The Android app. Jetpack Compose, Media3 ExoPlayer, Google Cast. This is the product. |
| [`src/`](src/), [`server.ts`](server.ts) | An earlier React/Vite web prototype, kept as a UI reference. Its casting is simulated. |

## Android app

### Modules

- **`:shared`** — playlist fetching, M3U parsing, filtering, persisted preferences. No UI.
- **`:mobile`** — the phone app: browse, play locally, cast to a TV.
- **`:tv`** — an Android TV / Google TV leanback browser. Reads the same playlist; on-device playback is not wired up yet.

### How channel data flows

```
playlist URL (default: iptv-org India list)
  → PlaylistRemoteSource      HTTP GET, gzip, 16 MB ceiling
  → M3uParser                 #EXTINF attrs, #EXTGRP, geo markers, dedupe
  → PlaylistRepository        disk cache (6 h TTL), StateFlow
  → ChannelsViewModel         category + search filtering
  → Compose screens
```

The cache is served first so the UI is never empty on launch, then refreshed in
the background. A failed refresh keeps the previous channel list on screen and
reports the error alongside it rather than blanking the app.

### How casting works

Discovery runs through `MediaRouter` with an active scan and a multicast lock,
filtered to remote playback routes. Connection state is derived **only** from
the Cast SDK's `SessionManagerListener`, so the UI never claims a connection
the receiver has not confirmed. Media loads report their result, and failures
surface the receiver's own status message.

`StreamCastOptionsProvider` currently targets Google's **Default Media
Receiver**. That receiver plays HLS but gives the sender no control over request
headers, so Indian streams that require a `Referer` or a specific `User-Agent`
will fail on the TV even when they play on the phone. To fix those, register a
custom web receiver at <https://cast.google.com/publish> and replace
`RECEIVER_APPLICATION_ID`; nothing else needs to change.

### What the app deliberately does not do

- **No D-pad remote.** Google Cast carries transport and volume only. Arrow
  keys, power and voice search need the Android TV Remote v2 protocol (TLS
  pairing on ports 6466/6467), which is not implemented. The remote screen says
  so instead of showing dead buttons.
- **No programme guide.** M3U playlists carry no schedule data, so there are no
  "now playing" titles, viewer counts or uptime figures anywhere in the UI.

## Building

Requires **JDK 17–21** and the Android SDK with platform 34.

```bash
./gradlew :mobile:assembleDebug          # build the phone APK
./gradlew :mobile:installDebug           # build and install on a connected device
./gradlew :shared:testDebugUnitTest      # parser and filter unit tests
```

`gradle.properties` pins `org.gradle.java.home` to a local JDK 21, because
Android Studio's bundled JBR 25 is newer than AGP 8.7 supports. Remove that
line if your Gradle JDK is already 17–21.

## Legal note

The default playlist points at a public aggregator that indexes streams this
project does not own or license, and a share of the Indian entries are
unauthorised rebroadcasts. The playlist URL is user-configurable in Settings;
point it at a source you are entitled to use. Shipping a bundled channel list
is what gets IPTV apps removed from app stores.

## Web prototype

```bash
npm install
npm run dev     # express + vite on :3000, proxies the playlist at /api/playlist
```
