export type PrimaryTab = 'home' | 'live' | 'favorites' | 'remote' | 'settings';

export type CategoryName =
  | 'All'
  | 'News'
  | 'Entertainment'
  | 'Movies'
  | 'Sports'
  | 'Music'
  | 'Regional'
  | 'Kids'
  | 'Devotional'
  | 'Others';

export interface Channel {
  id: string;
  name: string;
  streamUrl: string;
  logoUrl?: string;
  groupRaw?: string;
  category: Exclude<CategoryName, 'All'>;
  language?: string;
  quality?: string;
  isLive: boolean;
  isGeoRestricted?: boolean;
  epgAvailable: boolean;
}

export type PlaylistStatus = 'idle' | 'loading' | 'loaded' | 'error';

export interface PlaylistMeta {
  url: string;
  source: 'live' | 'fallback';
  fetchedAt: string;
  channelCount: number;
  status: PlaylistStatus;
  errorMessage?: string;
}

export type TvDiscoveryState =
  | 'idle'
  | 'searching'
  | 'found'
  | 'connecting'
  | 'connected'
  | 'failed'
  | 'empty';

export type CastProtocol = 'Google Cast v2' | 'Android TV Remote v2' | 'Google TV Cast';

export interface TvDeviceCapabilities {
  power: boolean;
  voiceSearch: boolean;
  volumeControl: boolean;
  numericKeypad: boolean;
  textInput: boolean;
}

export interface TvDevice {
  id: string;
  name: string;
  deviceType: 'Samsung Smart TV' | 'Android TV' | 'Google TV';
  protocol: CastProtocol;
  ipAddress: string;
  signalStrength: 'Strong' | 'Good' | 'Fair';
  capabilities: TvDeviceCapabilities;
  model: string;
}

export type CastPlaybackState = 'idle' | 'casting_in_progress' | 'casting_active' | 'casting_failed';

export type PlayerPlaybackStatus = 'idle' | 'loading' | 'playing' | 'paused' | 'error';

export type ViewMode = 'grid' | 'list';

export interface RemoteLogEntry {
  id: string;
  command: string;
  label: string;
  timestamp: string;
}

export interface AppSettings {
  themeAccent: 'violet' | 'cyan';
  defaultLandingTab: PrimaryTab;
  language: string;
  autoRefreshPlaylist: boolean;
  defaultQuality: 'Auto' | '1080p' | '720p' | '480p';
  autoPlayNext: boolean;
  keepScreenAwake: boolean;
  defaultPlayerMode: 'inline' | 'fullscreen';
  preferredDeviceId: string | null;
  autoConnectLastTv: boolean;
  preferCastWhenConnected: boolean;
  playlistUrl: string;
}
