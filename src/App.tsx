import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { WifiOff } from 'lucide-react';
import type {
  AppSettings,
  CastPlaybackState,
  CategoryName,
  Channel,
  PlaylistMeta,
  PrimaryTab,
  RemoteLogEntry,
  TvDevice,
  TvDiscoveryState,
  ViewMode,
} from './types/iptv';
import { getSortedAvailableCategories, parseM3U } from './utils/m3uParser';
import {
  filterChannels,
  getAdjacentChannel,
  getPopularChannels,
  getRelatedChannels,
} from './utils/channelFilter';
import { DISCOVERABLE_TV_DEVICES } from './data/tvDevices';
import { NavigationShell } from './components/BottomNav';
import { TopBar } from './components/TopBar';
import { OnboardingFlow } from './components/OnboardingFlow';
import { HomeDashboard } from './components/HomeDashboard';
import { LiveTvBrowser } from './components/LiveTvBrowser';
import { ChannelPlayerScreen } from './components/ChannelPlayerScreen';
import { DeviceDiscoveryModal } from './components/DeviceDiscoveryModal';
import { WifiRemoteScreen } from './components/WifiRemoteScreen';
import { FavoritesScreen } from './components/FavoritesScreen';
import { SettingsScreen } from './components/SettingsScreen';

const DEFAULT_PLAYLIST_URL = 'https://iptv-org.github.io/iptv/countries/in.m3u';

const DEFAULT_SETTINGS: AppSettings = {
  themeAccent: 'violet',
  defaultLandingTab: 'home',
  language: 'English (India)',
  autoRefreshPlaylist: true,
  defaultQuality: 'Auto',
  autoPlayNext: false,
  keepScreenAwake: true,
  defaultPlayerMode: 'inline',
  preferredDeviceId: null,
  autoConnectLastTv: false,
  preferCastWhenConnected: true,
  playlistUrl: DEFAULT_PLAYLIST_URL,
};

function readStorageJson<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export default function App() {
  const autostart =
    typeof window !== 'undefined' &&
    new URLSearchParams(window.location.search).get('autostart') === '1';

  const [showOnboarding, setShowOnboarding] = useState<boolean>(() => {
    if (autostart) return false;
    return !readStorageJson<boolean>('streamcast_onboarded', false);
  });

  const [settings, setSettings] = useState<AppSettings>(() =>
    readStorageJson<AppSettings>('streamcast_settings', DEFAULT_SETTINGS)
  );

  const [activeTab, setActiveTab] = useState<PrimaryTab>(settings.defaultLandingTab);
  const [channels, setChannels] = useState<Channel[]>([]);
  const [playlistMeta, setPlaylistMeta] = useState<PlaylistMeta>({
    url: settings.playlistUrl,
    source: 'live',
    fetchedAt: '',
    channelCount: 0,
    status: 'loading',
  });

  const [selectedCategory, setSelectedCategory] = useState<CategoryName>('All');
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<ViewMode>('grid');

  const [favoriteIdsList, setFavoriteIdsList] = useState<string[]>(() =>
    readStorageJson<string[]>('streamcast_favorites', [])
  );
  const [recentIdsList, setRecentIdsList] = useState<string[]>(() =>
    readStorageJson<string[]>('streamcast_recent', [])
  );

  // Player state
  const [activeChannel, setActiveChannel] = useState<Channel | null>(null);
  const [playerOpen, setPlayerOpen] = useState(false);
  const [isPlaying, setIsPlaying] = useState(true);
  const [isMuted, setIsMuted] = useState(true); // Muted initially for smooth browser playback
  const [volume, setVolume] = useState(80);

  // TV Discovery & Casting state
  const [isDiscoveryOpen, setIsDiscoveryOpen] = useState(false);
  const [discoveryState, setDiscoveryState] = useState<TvDiscoveryState>('idle');
  const [discoveredDevices, setDiscoveredDevices] = useState<TvDevice[]>(DISCOVERABLE_TV_DEVICES);
  const [connectedTv, setConnectedTv] = useState<TvDevice | null>(() => {
    const savedId = readStorageJson<string | null>('streamcast_connected_tv_id', null);
    if (!savedId) return null;
    return DISCOVERABLE_TV_DEVICES.find((d) => d.id === savedId) ?? null;
  });
  const [connectingDeviceId, setConnectingDeviceId] = useState<string | null>(null);
  const [discoveryError, setDiscoveryError] = useState<string | undefined>(undefined);
  const [castState, setCastState] = useState<CastPlaybackState>('idle');
  const [remoteLogs, setRemoteLogs] = useState<RemoteLogEntry[]>([]);

  const [isOffline, setIsOffline] = useState<boolean>(
    typeof navigator !== 'undefined' ? !navigator.onLine : false
  );
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const discoveryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((curr) => (curr === msg ? null : curr));
    }, 3200);
  }, []);

  // Offline / online network awareness
  useEffect(() => {
    const handleOnline = () => setIsOffline(false);
    const handleOffline = () => setIsOffline(true);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const loadPlaylist = useCallback(
    async (forceRefresh = false) => {
      setPlaylistMeta((prev) => ({
        ...prev,
        status: 'loading',
        errorMessage: undefined,
      }));

      try {
        const params = new URLSearchParams({
          url: settings.playlistUrl,
          ...(forceRefresh ? { refresh: '1' } : {}),
        });
        const response = await fetch(`/api/playlist?${params.toString()}`);
        if (!response.ok) {
          throw new Error(`Server returned ${response.status}`);
        }
        const payload = (await response.json()) as {
          url: string;
          source: 'live' | 'fallback';
          fetchedAt: string;
          raw: string;
        };

        const parsed = parseM3U(payload.raw);
        setChannels(parsed);
        setPlaylistMeta({
          url: payload.url,
          source: payload.source,
          fetchedAt: payload.fetchedAt,
          channelCount: parsed.length,
          status: 'loaded',
        });

        if (forceRefresh) {
          showToast(`Playlist refreshed (${parsed.length} channels)`);
        }
      } catch (err) {
        setPlaylistMeta((prev) => ({
          ...prev,
          status: 'error',
          errorMessage:
            err instanceof Error ? err.message : 'Unable to reach IPTV playlist service.',
        }));
      }
    },
    [settings.playlistUrl, showToast]
  );

  useEffect(() => {
    loadPlaylist(false);
  }, [loadPlaylist]);

  // Persist favorites, recents, and settings
  useEffect(() => {
    try {
      window.localStorage.setItem('streamcast_favorites', JSON.stringify(favoriteIdsList));
    } catch {
      // Ignore quota errors
    }
  }, [favoriteIdsList]);

  useEffect(() => {
    try {
      window.localStorage.setItem('streamcast_recent', JSON.stringify(recentIdsList));
    } catch {
      // Ignore quota errors
    }
  }, [recentIdsList]);

  useEffect(() => {
    try {
      window.localStorage.setItem('streamcast_settings', JSON.stringify(settings));
    } catch {
      // Ignore quota errors
    }
  }, [settings]);

  useEffect(() => {
    try {
      window.localStorage.setItem(
        'streamcast_connected_tv_id',
        JSON.stringify(connectedTv?.id ?? null)
      );
    } catch {
      // Ignore quota errors
    }
  }, [connectedTv]);

  const favoriteIds = useMemo(() => new Set(favoriteIdsList), [favoriteIdsList]);

  const availableCategories = useMemo(
    () => getSortedAvailableCategories(channels),
    [channels]
  );

  const filteredChannels = useMemo(
    () => filterChannels(channels, selectedCategory, searchQuery),
    [channels, selectedCategory, searchQuery]
  );

  const popularChannels = useMemo(() => getPopularChannels(channels, 10), [channels]);

  const continueWatching = useMemo(() => {
    const map = new Map(channels.map((ch) => [ch.id, ch]));
    return recentIdsList.map((id) => map.get(id)).filter((ch): ch is Channel => Boolean(ch));
  }, [channels, recentIdsList]);

  const favoriteChannels = useMemo(
    () => channels.filter((ch) => favoriteIds.has(ch.id)),
    [channels, favoriteIds]
  );

  const relatedChannels = useMemo(
    () => getRelatedChannels(channels, activeChannel, 10),
    [channels, activeChannel]
  );

  const handleToggleFavorite = (channelId: string) => {
    setFavoriteIdsList((prev) => {
      if (prev.includes(channelId)) {
        return prev.filter((id) => id !== channelId);
      }
      return [channelId, ...prev];
    });
  };

  const handleSelectChannel = (channel: Channel) => {
    setActiveChannel(channel);
    setPlayerOpen(true);
    setIsPlaying(true);
    setRecentIdsList((prev) => [channel.id, ...prev.filter((id) => id !== channel.id)].slice(0, 10));
    if (connectedTv && settings.preferCastWhenConnected) {
      setCastState('casting_active');
    } else {
      setCastState('idle');
    }
  };

  const startDeviceScan = (simulateEmpty = false) => {
    if (discoveryTimerRef.current) clearTimeout(discoveryTimerRef.current);
    setDiscoveryError(undefined);
    setDiscoveryState('searching');
    discoveryTimerRef.current = setTimeout(() => {
      if (simulateEmpty) {
        setDiscoveredDevices([]);
        setDiscoveryState('empty');
      } else {
        setDiscoveredDevices(DISCOVERABLE_TV_DEVICES);
        setDiscoveryState(connectedTv ? 'connected' : 'found');
      }
    }, 650);
  };

  const handleOpenDiscovery = () => {
    setIsDiscoveryOpen(true);
    startDeviceScan(false);
  };

  const handleConnectDevice = (device: TvDevice, simulateFailure = false) => {
    setDiscoveryError(undefined);
    setConnectingDeviceId(device.id);
    setDiscoveryState('connecting');

    setTimeout(() => {
      setConnectingDeviceId(null);
      if (simulateFailure) {
        setDiscoveryState('failed');
        setDiscoveryError(
          `Pairing timed out for ${device.name}. Ensure Cast receiver is active on your TV.`
        );
        return;
      }

      setConnectedTv(device);
      setDiscoveryState('connected');
      showToast(`Connected to ${device.name}`);

      if (activeChannel) {
        setCastState('casting_active');
      }
    }, 550);
  };

  const handleDisconnectTv = () => {
    const previousName = connectedTv?.name;
    setConnectedTv(null);
    setCastState('idle');
    setDiscoveryState('found');
    if (previousName) {
      showToast(`Disconnected from ${previousName}`);
    }
  };

  const handleCastChannel = (channel: Channel) => {
    setActiveChannel(channel);
    setPlayerOpen(true);
    setRecentIdsList((prev) => [channel.id, ...prev.filter((id) => id !== channel.id)].slice(0, 10));

    if (!connectedTv) {
      handleOpenDiscovery();
      return;
    }

    setCastState('casting_in_progress');
    setTimeout(() => {
      setCastState('casting_active');
      showToast(`Playing ${channel.name} on ${connectedTv.name}`);
    }, 450);
  };

  const handleSendRemoteCommand = (command: string, label: string) => {
    const now = new Date().toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
    setRemoteLogs((prev) => [
      { id: `${Date.now()}-${Math.random()}`, command, label, timestamp: now },
      ...prev.slice(0, 14),
    ]);

    if (command === 'KEYCODE_MEDIA_PLAY_PAUSE') {
      setIsPlaying((p) => !p);
    } else if (command === 'KEYCODE_MEDIA_STOP') {
      setIsPlaying(false);
    } else if (command === 'KEYCODE_VOLUME_MUTE') {
      setIsMuted((m) => !m);
    } else if (command === 'KEYCODE_VOLUME_UP') {
      setIsMuted(false);
      setVolume((v) => Math.min(100, v + 10));
    } else if (command === 'KEYCODE_VOLUME_DOWN') {
      setVolume((v) => Math.max(0, v - 10));
    } else if (command === 'KEYCODE_CHANNEL_UP' && channels.length > 0) {
      const next = getAdjacentChannel(channels, activeChannel, 'next');
      if (next) setActiveChannel(next);
    } else if (command === 'KEYCODE_CHANNEL_DOWN' && channels.length > 0) {
      const prev = getAdjacentChannel(channels, activeChannel, 'prev');
      if (prev) setActiveChannel(prev);
    }
  };

  const handleOpenSearch = () => {
    setPlayerOpen(false);
    setActiveTab('live');
    setTimeout(() => {
      searchInputRef.current?.focus();
    }, 80);
  };

  const handleSelectTab = (tab: PrimaryTab) => {
    setPlayerOpen(false);
    setActiveTab(tab);
  };

  if (showOnboarding) {
    return (
      <OnboardingFlow
        onComplete={(action) => {
          try {
            window.localStorage.setItem('streamcast_onboarded', 'true');
          } catch {
            // Ignore
          }
          setShowOnboarding(false);
          if (action === 'find-tv') {
            handleOpenDiscovery();
          }
        }}
      />
    );
  }

  return (
    <div
      className="min-h-screen bg-[#101114] text-white flex flex-col lg:flex-row"
      data-app-state={playerOpen ? 'player' : activeTab}
    >
      <NavigationShell
        activeTab={activeTab}
        onSelectTab={handleSelectTab}
        connectedTv={connectedTv}
        favoritesCount={favoriteChannels.length}
        onOpenTvDiscovery={handleOpenDiscovery}
      />

      <div className="flex-1 min-w-0 flex flex-col">
        <TopBar
          connectedTv={connectedTv}
          onOpenTvDiscovery={handleOpenDiscovery}
          onOpenSearch={handleOpenSearch}
          onGoHome={() => handleSelectTab('home')}
        />

        {/* Offline Banner */}
        {isOffline && (
          <div
            role="status"
            className="mx-4 lg:mx-8 mt-4 rounded-2xl bg-[#93000A]/30 border border-[#FFB4AB]/40 px-4 py-3 flex items-center justify-between gap-3 text-xs"
          >
            <span className="flex items-center gap-2 text-[#FFDBD6]">
              <WifiOff className="w-4 h-4 shrink-0" aria-hidden="true" />
              Internet connection lost. Live streams require an active network connection.
            </span>
          </div>
        )}

        <main className="flex-1 px-4 sm:px-6 lg:px-8 py-6 pb-24 lg:pb-10 max-w-7xl w-full mx-auto">
          {playerOpen && activeChannel ? (
            <ChannelPlayerScreen
              channel={activeChannel}
              relatedChannels={relatedChannels}
              connectedTv={connectedTv}
              castState={castState}
              isPlaying={isPlaying}
              isMuted={isMuted}
              volume={volume}
              isFavorite={favoriteIds.has(activeChannel.id)}
              onBack={() => setPlayerOpen(false)}
              onTogglePlay={() => setIsPlaying((p) => !p)}
              onStop={() => setIsPlaying(false)}
              onToggleMute={() => setIsMuted((m) => !m)}
              onVolumeChange={(val) => {
                setVolume(val);
                if (val > 0 && isMuted) setIsMuted(false);
              }}
              onPrevChannel={() => {
                const prev = getAdjacentChannel(channels, activeChannel, 'prev');
                if (prev) handleSelectChannel(prev);
              }}
              onNextChannel={() => {
                const next = getAdjacentChannel(channels, activeChannel, 'next');
                if (next) handleSelectChannel(next);
              }}
              onToggleFavorite={handleToggleFavorite}
              onCastToTv={() => handleCastChannel(activeChannel)}
              onSelectChannel={handleSelectChannel}
              onOpenRemote={() => {
                setPlayerOpen(false);
                setActiveTab('remote');
              }}
            />
          ) : activeTab === 'home' ? (
            <HomeDashboard
              featuredChannel={activeChannel}
              continueWatching={continueWatching}
              popularChannels={popularChannels}
              availableCategories={availableCategories}
              connectedTv={connectedTv}
              isPlaying={isPlaying}
              isMuted={isMuted}
              volume={volume}
              favoriteIds={favoriteIds}
              onTogglePlay={() => setIsPlaying((p) => !p)}
              onSelectChannel={handleSelectChannel}
              onBrowseLiveTv={(cat) => {
                if (cat) setSelectedCategory(cat);
                setActiveTab('live');
              }}
              onOpenSearch={handleOpenSearch}
              onOpenTvDiscovery={handleOpenDiscovery}
              onToggleFavorite={handleToggleFavorite}
            />
          ) : activeTab === 'live' ? (
            <LiveTvBrowser
              channels={channels}
              filteredChannels={filteredChannels}
              availableCategories={availableCategories}
              selectedCategory={selectedCategory}
              searchQuery={searchQuery}
              viewMode={viewMode}
              playlistMeta={playlistMeta}
              favoriteIds={favoriteIds}
              connectedTv={connectedTv}
              searchInputRef={searchInputRef}
              onSelectCategory={setSelectedCategory}
              onSearchChange={setSearchQuery}
              onViewModeChange={setViewMode}
              onRefreshPlaylist={() => loadPlaylist(true)}
              onSelectChannel={handleSelectChannel}
              onCastChannel={handleCastChannel}
              onToggleFavorite={handleToggleFavorite}
            />
          ) : activeTab === 'favorites' ? (
            <FavoritesScreen
              favoriteChannels={favoriteChannels}
              connectedTv={connectedTv}
              onSelectChannel={handleSelectChannel}
              onCastChannel={handleCastChannel}
              onRemoveFavorite={handleToggleFavorite}
              onExploreChannels={() => setActiveTab('live')}
            />
          ) : activeTab === 'remote' ? (
            <WifiRemoteScreen
              connectedTv={connectedTv}
              activeChannel={activeChannel}
              isPlaying={isPlaying}
              isMuted={isMuted}
              remoteLogs={remoteLogs}
              onConnectTv={handleOpenDiscovery}
              onDisconnectTv={handleDisconnectTv}
              onOpenSettings={() => setActiveTab('settings')}
              onSendCommand={handleSendRemoteCommand}
            />
          ) : (
            <SettingsScreen
              settings={settings}
              playlistMeta={playlistMeta}
              availableDevices={DISCOVERABLE_TV_DEVICES}
              onUpdateSettings={(patch) =>
                setSettings((prev) => ({ ...prev, ...patch }))
              }
              onRefreshPlaylist={() => loadPlaylist(true)}
              onReplayOnboarding={() => setShowOnboarding(true)}
            />
          )}
        </main>
      </div>

      {/* Device Discovery & TV Connection Modal */}
      <DeviceDiscoveryModal
        isOpen={isDiscoveryOpen}
        discoveryState={discoveryState}
        devices={discoveredDevices}
        connectedTv={connectedTv}
        connectingDeviceId={connectingDeviceId}
        errorMessage={discoveryError}
        onClose={() => setIsDiscoveryOpen(false)}
        onRescan={startDeviceScan}
        onConnectDevice={handleConnectDevice}
        onDisconnect={handleDisconnectTv}
        onLaunchRemote={() => {
          setIsDiscoveryOpen(false);
          setPlayerOpen(false);
          setActiveTab('remote');
        }}
      />

      {/* Non-intrusive Feedback Toast */}
      {toastMessage && (
        <div
          role="status"
          aria-live="polite"
          className="pointer-events-none fixed bottom-20 lg:bottom-6 right-4 lg:right-6 z-50 px-4 py-3 rounded-xl bg-[#242731] border border-[#7657FF]/60 text-sm font-medium text-white shadow-2xl"
        >
          {toastMessage}
        </div>
      )}
    </div>
  );
}
