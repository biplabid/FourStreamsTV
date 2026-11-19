import { useState } from 'react';
import {
  Cast,
  Code2,
  Info,
  RefreshCw,
  Shield,
  Sliders,
  Tv,
  X,
} from 'lucide-react';
import type {
  AppSettings,
  PlaylistMeta,
  PrimaryTab,
  TvDevice,
} from '../types/iptv';

interface SettingsScreenProps {
  settings: AppSettings;
  playlistMeta: PlaylistMeta;
  availableDevices: TvDevice[];
  onUpdateSettings: (patch: Partial<AppSettings>) => void;
  onRefreshPlaylist: () => void;
  onReplayOnboarding: () => void;
}

const KOTLIN_COMPOSE_SNIPPET = `// StreamCast Android Mobile & TV Compose Architecture
// Project files generated in /android-compose/:
//  ├── shared/model/IptvModels.kt
//  ├── shared/parser/M3uParser.kt
//  ├── shared/cast/CastController.kt
//  ├── mobile/ui/theme/StreamCastTheme.kt
//  ├── mobile/ui/navigation/StreamCastNavHost.kt
//  ├── mobile/ui/screens/{HomeScreen,LiveTvScreen,PlayerScreen,DeviceDiscoveryScreen,WifiRemoteScreen}.kt
//  └── tv/ui/{TvMainActivity,TvBrowseScreen}.kt

@Composable
fun StreamCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF101114),
            surface = Color(0xFF1B1D23),
            surfaceVariant = Color(0xFF242731),
            primary = Color(0xFF7657FF),
            secondary = Color(0xFF35C9E8),
            tertiary = Color(0xFF35D07F)
        ),
        content = content
    )
}`;

export function SettingsScreen({
  settings,
  playlistMeta,
  availableDevices,
  onUpdateSettings,
  onRefreshPlaylist,
  onReplayOnboarding,
}: SettingsScreenProps) {
  const [showKotlinModal, setShowKotlinModal] = useState(false);

  const formattedTimestamp = playlistMeta.fetchedAt
    ? new Date(playlistMeta.fetchedAt).toLocaleString()
    : 'Not yet loaded';

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white">
          Settings
        </h1>
        <p className="text-sm text-[#A7A9B2] mt-0.5">
          Configure playback, casting preferences, and your IPTV playlist source
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* General */}
        <section className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-5 space-y-4">
          <div className="flex items-center gap-2.5">
            <Sliders className="w-4 h-4 text-[#7657FF]" aria-hidden="true" />
            <h2 className="font-display text-base font-bold text-white">General</h2>
          </div>

          <div className="space-y-3.5 text-sm">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="font-medium text-white">App Theme</p>
                <p className="text-xs text-[#A7A9B2]">AMOLED Dark Theater</p>
              </div>
              <span className="text-xs font-medium text-[#35C9E8]">Dark Only</span>
            </div>

            <div className="flex items-center justify-between gap-3">
              <label htmlFor="setting-landing" className="font-medium text-white">
                Default Landing Screen
              </label>
              <select
                id="setting-landing"
                value={settings.defaultLandingTab}
                onChange={(e) =>
                  onUpdateSettings({ defaultLandingTab: e.target.value as PrimaryTab })
                }
                className="h-10 px-3 rounded-xl bg-[#242731] border border-[#2E323E] text-xs text-white focus:border-[#35C9E8] focus:outline-none"
              >
                <option value="home">Home</option>
                <option value="live">Live TV</option>
                <option value="favorites">Favorites</option>
              </select>
            </div>

            <div className="flex items-center justify-between gap-3">
              <label htmlFor="setting-language" className="font-medium text-white">
                Preferred Language
              </label>
              <select
                id="setting-language"
                value={settings.language}
                onChange={(e) => onUpdateSettings({ language: e.target.value })}
                className="h-10 px-3 rounded-xl bg-[#242731] border border-[#2E323E] text-xs text-white focus:border-[#35C9E8] focus:outline-none"
              >
                <option value="English (India)">English (India)</option>
                <option value="Hindi">Hindi</option>
                <option value="Tamil">Tamil</option>
                <option value="Bengali">Bengali</option>
              </select>
            </div>

            <label className="flex items-center justify-between gap-3 cursor-pointer min-h-[44px]">
              <span className="font-medium text-white">Auto-refresh playlist on launch</span>
              <input
                type="checkbox"
                checked={settings.autoRefreshPlaylist}
                onChange={(e) => onUpdateSettings({ autoRefreshPlaylist: e.target.checked })}
                className="w-5 h-5 accent-[#7657FF] rounded"
              />
            </label>
          </div>
        </section>

        {/* Playback */}
        <section className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-5 space-y-4">
          <div className="flex items-center gap-2.5">
            <Tv className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
            <h2 className="font-display text-base font-bold text-white">Playback</h2>
          </div>

          <div className="space-y-3.5 text-sm">
            <div className="flex items-center justify-between gap-3">
              <label htmlFor="setting-quality" className="font-medium text-white">
                Default Stream Quality
              </label>
              <select
                id="setting-quality"
                value={settings.defaultQuality}
                onChange={(e) =>
                  onUpdateSettings({
                    defaultQuality: e.target.value as AppSettings['defaultQuality'],
                  })
                }
                className="h-10 px-3 rounded-xl bg-[#242731] border border-[#2E323E] text-xs text-white focus:border-[#35C9E8] focus:outline-none"
              >
                <option value="Auto">Auto (Adaptive HLS)</option>
                <option value="1080p">1080p (if supported)</option>
                <option value="720p">720p (if supported)</option>
                <option value="480p">480p (if supported)</option>
              </select>
            </div>

            <div className="flex items-center justify-between gap-3">
              <label htmlFor="setting-player-mode" className="font-medium text-white">
                Default Player Mode
              </label>
              <select
                id="setting-player-mode"
                value={settings.defaultPlayerMode}
                onChange={(e) =>
                  onUpdateSettings({
                    defaultPlayerMode: e.target.value as AppSettings['defaultPlayerMode'],
                  })
                }
                className="h-10 px-3 rounded-xl bg-[#242731] border border-[#2E323E] text-xs text-white focus:border-[#35C9E8] focus:outline-none"
              >
                <option value="inline">Inline Preview</option>
                <option value="fullscreen">Fullscreen Preferred</option>
              </select>
            </div>

            <label className="flex items-center justify-between gap-3 cursor-pointer min-h-[44px]">
              <span className="font-medium text-white">Auto-play next channel on failure</span>
              <input
                type="checkbox"
                checked={settings.autoPlayNext}
                onChange={(e) => onUpdateSettings({ autoPlayNext: e.target.checked })}
                className="w-5 h-5 accent-[#7657FF] rounded"
              />
            </label>

            <label className="flex items-center justify-between gap-3 cursor-pointer min-h-[44px]">
              <span className="font-medium text-white">Keep screen awake during playback</span>
              <input
                type="checkbox"
                checked={settings.keepScreenAwake}
                onChange={(e) => onUpdateSettings({ keepScreenAwake: e.target.checked })}
                className="w-5 h-5 accent-[#7657FF] rounded"
              />
            </label>
          </div>
        </section>

        {/* Casting */}
        <section className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-5 space-y-4">
          <div className="flex items-center gap-2.5">
            <Cast className="w-4 h-4 text-[#35D07F]" aria-hidden="true" />
            <h2 className="font-display text-base font-bold text-white">Casting</h2>
          </div>

          <div className="space-y-3.5 text-sm">
            <div className="flex items-center justify-between gap-3">
              <label htmlFor="setting-preferred-tv" className="font-medium text-white">
                Preferred TV Device
              </label>
              <select
                id="setting-preferred-tv"
                value={settings.preferredDeviceId ?? ''}
                onChange={(e) =>
                  onUpdateSettings({ preferredDeviceId: e.target.value || null })
                }
                className="h-10 px-3 rounded-xl bg-[#242731] border border-[#2E323E] text-xs text-white max-w-[190px] focus:border-[#35C9E8] focus:outline-none"
              >
                <option value="">Ask each time</option>
                {availableDevices.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>

            <label className="flex items-center justify-between gap-3 cursor-pointer min-h-[44px]">
              <span className="font-medium text-white">Auto-connect to last connected TV</span>
              <input
                type="checkbox"
                checked={settings.autoConnectLastTv}
                onChange={(e) => onUpdateSettings({ autoConnectLastTv: e.target.checked })}
                className="w-5 h-5 accent-[#7657FF] rounded"
              />
            </label>

            <label className="flex items-center justify-between gap-3 cursor-pointer min-h-[44px]">
              <span className="font-medium text-white">Prefer Cast when TV is connected</span>
              <input
                type="checkbox"
                checked={settings.preferCastWhenConnected}
                onChange={(e) =>
                  onUpdateSettings({ preferCastWhenConnected: e.target.checked })
                }
                className="w-5 h-5 accent-[#7657FF] rounded"
              />
            </label>
          </div>
        </section>

        {/* Playlist */}
        <section className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-5 space-y-4">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2.5">
              <RefreshCw className="w-4 h-4 text-[#7657FF]" aria-hidden="true" />
              <h2 className="font-display text-base font-bold text-white">Playlist</h2>
            </div>
            <span
              className={`text-xs font-semibold ${
                playlistMeta.source === 'live' ? 'text-[#35D07F]' : 'text-[#35C9E8]'
              }`}
            >
              {playlistMeta.source === 'live' ? 'Live M3U Loaded' : 'Offline Fallback Active'}
            </span>
          </div>

          <div className="space-y-2.5 text-xs">
            <div>
              <span className="text-[#A7A9B2] block">Playlist Source URL</span>
              <span className="font-mono text-white break-all mt-0.5 block">
                {playlistMeta.url}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-1">
              <div>
                <span className="text-[#A7A9B2] block">Channel Count</span>
                <span className="font-display font-bold text-base text-white tabular-nums">
                  {playlistMeta.channelCount}
                </span>
              </div>
              <div>
                <span className="text-[#A7A9B2] block">Last Refreshed</span>
                <span className="text-white block mt-0.5">{formattedTimestamp}</span>
              </div>
            </div>

            <button
              type="button"
              onClick={onRefreshPlaylist}
              disabled={playlistMeta.status === 'loading'}
              className="w-full min-h-[44px] mt-2 px-4 py-2.5 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] disabled:opacity-60 text-white font-display font-semibold text-xs flex items-center justify-center gap-2 transition-colors"
            >
              <RefreshCw
                className={`w-4 h-4 ${playlistMeta.status === 'loading' ? 'animate-spin' : ''}`}
                aria-hidden="true"
              />
              <span>Refresh Playlist Now</span>
            </button>
          </div>
        </section>
      </div>

      {/* About & Kotlin Compose Project Card */}
      <section className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-5 space-y-4">
        <div className="flex items-center gap-2.5">
          <Info className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
          <h2 className="font-display text-base font-bold text-white">About StreamCast</h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs text-[#A7A9B2]">
          <div>
            <p className="font-semibold text-white">StreamCast</p>
            <p className="mt-1">Live TV. Anywhere. On Any Screen. Designed for Galaxy S24 &amp; Android TV.</p>
          </div>
          <div>
            <p className="font-semibold text-white flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5 text-[#35D07F]" aria-hidden="true" />
              Open-Source Attribution
            </p>
            <p className="mt-1">
              Channel index powered by iptv-org Indian public broadcaster directory (`countries/in.m3u`).
            </p>
          </div>
          <div className="flex flex-col justify-between gap-2">
            <button
              type="button"
              onClick={() => setShowKotlinModal(true)}
              className="min-h-[40px] px-3.5 py-2 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white font-medium flex items-center justify-center gap-2"
            >
              <Code2 className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
              <span>Android Compose Architecture</span>
            </button>
            <button
              type="button"
              onClick={onReplayOnboarding}
              className="min-h-[38px] px-3.5 py-2 rounded-xl bg-transparent hover:bg-[#242731] border border-[#2E323E] text-[#A7A9B2] hover:text-white font-medium"
            >
              Replay Onboarding Tour
            </button>
          </div>
        </div>
      </section>

      {/* Kotlin Compose Architecture Modal */}
      {showKotlinModal && (
        <div
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-label="Android Kotlin Jetpack Compose Architecture"
          onClick={() => setShowKotlinModal(false)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-2xl rounded-3xl bg-[#1B1D23] border border-[#2E323E] p-6 space-y-4 shadow-2xl"
          >
            <div className="flex items-center justify-between gap-3">
              <div>
                <h3 className="font-display text-lg font-bold text-white">
                  Kotlin Jetpack Compose Project (Mobile &amp; Android TV)
                </h3>
                <p className="text-xs text-[#A7A9B2] mt-0.5">
                  Included in the workspace under <span className="font-mono text-white">/android-compose/</span>
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowKotlinModal(false)}
                aria-label="Close Kotlin Compose overview"
                className="w-10 h-10 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white"
              >
                <X className="w-5 h-5" aria-hidden="true" />
              </button>
            </div>
            <pre className="p-4 rounded-2xl bg-[#101114] border border-[#2E323E] text-xs font-mono text-[#E3E2E6] overflow-x-auto leading-relaxed">
              {KOTLIN_COMPOSE_SNIPPET}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
