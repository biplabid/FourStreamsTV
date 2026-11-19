import { useState } from 'react';
import {
  ArrowLeft,
  Cast,
  ChevronLeft,
  ChevronRight,
  Heart,
  Info,
  Pause,
  Play,
  Radio,
  Square,
  Volume2,
  VolumeX,
} from 'lucide-react';
import type {
  CastPlaybackState,
  Channel,
  PlayerPlaybackStatus,
  TvDevice,
} from '../types/iptv';
import { ChannelLogo } from './ChannelLogo';
import { LiveBadge } from './LiveBadge';
import { VideoPreviewSurface } from './VideoPreviewSurface';

interface ChannelPlayerScreenProps {
  channel: Channel;
  relatedChannels: Channel[];
  connectedTv: TvDevice | null;
  castState: CastPlaybackState;
  isPlaying: boolean;
  isMuted: boolean;
  volume: number;
  isFavorite: boolean;
  onBack: () => void;
  onTogglePlay: () => void;
  onStop: () => void;
  onToggleMute: () => void;
  onVolumeChange: (val: number) => void;
  onPrevChannel: () => void;
  onNextChannel: () => void;
  onToggleFavorite: (channelId: string) => void;
  onCastToTv: () => void;
  onSelectChannel: (channel: Channel) => void;
  onOpenRemote: () => void;
}

export function ChannelPlayerScreen({
  channel,
  relatedChannels,
  connectedTv,
  castState,
  isPlaying,
  isMuted,
  volume,
  isFavorite,
  onBack,
  onTogglePlay,
  onStop,
  onToggleMute,
  onVolumeChange,
  onPrevChannel,
  onNextChannel,
  onToggleFavorite,
  onCastToTv,
  onSelectChannel,
  onOpenRemote,
}: ChannelPlayerScreenProps) {
  const [streamStatus, setStreamStatus] = useState<PlayerPlaybackStatus>('loading');

  const isCastingActive = Boolean(connectedTv && castState === 'casting_active');
  const isCastingBusy = castState === 'casting_in_progress';

  return (
    <div className="space-y-6">
      {/* Top Back Bar */}
      <div className="flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={onBack}
          className="min-h-[44px] px-3 -ml-2 rounded-xl text-sm font-medium text-[#A7A9B2] hover:text-white hover:bg-[#1B1D23] flex items-center gap-1.5 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" aria-hidden="true" />
          <span>Back to Channels</span>
        </button>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onToggleFavorite(channel.id)}
            className={`min-h-[44px] px-3.5 rounded-xl border text-xs font-semibold flex items-center gap-2 transition-colors ${
              isFavorite
                ? 'bg-[#7657FF]/20 border-[#7657FF]/60 text-white'
                : 'bg-[#1B1D23] border-[#2E323E] text-[#A7A9B2] hover:text-white'
            }`}
            aria-label={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
          >
            <Heart
              className={`w-4 h-4 ${isFavorite ? 'fill-[#7657FF] text-[#7657FF]' : ''}`}
              aria-hidden="true"
            />
            <span>{isFavorite ? 'Favorited' : 'Favorite'}</span>
          </button>
        </div>
      </div>

      {/* Main Layout: 16:9 Player + Side Info/Cast Panel on Desktop */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
        <div className="xl:col-span-8 space-y-4">
          <VideoPreviewSurface
            channel={channel}
            isPlaying={isPlaying}
            isMuted={isMuted}
            volume={volume}
            castTargetName={isCastingActive ? connectedTv?.name : null}
            onStatusChange={setStreamStatus}
            onTogglePlay={onTogglePlay}
            onNextChannel={onNextChannel}
            onCastClick={onCastToTv}
          />

          {/* Player Transport & Volume Controls (No seeking controls for live IPTV) */}
          <div className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-2 flex-wrap">
              <button
                type="button"
                onClick={onPrevChannel}
                className="min-h-[44px] px-3.5 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white text-xs font-semibold flex items-center gap-1.5 transition-colors"
                aria-label="Previous channel"
              >
                <ChevronLeft className="w-4 h-4" aria-hidden="true" />
                <span>Prev</span>
              </button>

              <button
                type="button"
                onClick={onTogglePlay}
                className="min-h-[44px] px-5 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white text-sm font-display font-semibold flex items-center gap-2 shadow-[0_4px_16px_rgba(118,87,255,0.3)] transition-colors"
                aria-label={isPlaying ? 'Pause' : 'Play'}
              >
                {isPlaying ? (
                  <>
                    <Pause className="w-4 h-4" aria-hidden="true" />
                    <span>Pause</span>
                  </>
                ) : (
                  <>
                    <Play className="w-4 h-4 fill-current" aria-hidden="true" />
                    <span>Play</span>
                  </>
                )}
              </button>

              <button
                type="button"
                onClick={onStop}
                className="min-h-[44px] px-3.5 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-[#A7A9B2] hover:text-white text-xs font-semibold flex items-center gap-1.5 transition-colors"
                aria-label="Stop playback"
              >
                <Square className="w-3.5 h-3.5 fill-current" aria-hidden="true" />
                <span>Stop</span>
              </button>

              <button
                type="button"
                onClick={onNextChannel}
                className="min-h-[44px] px-3.5 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white text-xs font-semibold flex items-center gap-1.5 transition-colors"
                aria-label="Next channel"
              >
                <span>Next</span>
                <ChevronRight className="w-4 h-4" aria-hidden="true" />
              </button>
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={onToggleMute}
                className="w-11 h-11 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] flex items-center justify-center text-white transition-colors shrink-0"
                aria-label={isMuted ? 'Unmute audio' : 'Mute audio'}
              >
                {isMuted || volume === 0 ? (
                  <VolumeX className="w-4 h-4 text-[#FFB4AB]" aria-hidden="true" />
                ) : (
                  <Volume2 className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
                )}
              </button>
              <label htmlFor="player-volume-slider" className="sr-only">
                Volume
              </label>
              <input
                id="player-volume-slider"
                type="range"
                min={0}
                max={100}
                value={isMuted ? 0 : volume}
                onChange={(e) => onVolumeChange(Number(e.target.value))}
                className="w-28 sm:w-32 accent-[#7657FF] cursor-pointer"
              />
              <span className="text-xs font-mono tabular-nums text-[#A7A9B2] w-9 text-right">
                {isMuted ? '0%' : `${volume}%`}
              </span>
            </div>
          </div>
        </div>

        {/* Channel Information & Prominent Cast Card */}
        <div className="xl:col-span-4 space-y-4">
          <div className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-5 space-y-4">
            <div className="flex items-start gap-3.5">
              <ChannelLogo
                name={channel.name}
                logoUrl={channel.logoUrl}
                category={channel.category}
                size="xl"
              />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <h1 className="font-display text-xl font-bold text-white truncate">
                    {channel.name}
                  </h1>
                  <LiveBadge compact />
                </div>
                <p className="text-sm text-[#A7A9B2] mt-1">
                  {channel.category}
                  {channel.language ? ` · ${channel.language}` : ''}
                  {channel.quality ? ` · ${channel.quality}` : ''}
                </p>
              </div>
            </div>

            <div className="pt-3 border-t border-[#2E323E] grid grid-cols-2 gap-3 text-xs">
              <div>
                <span className="text-[#A7A9B2] block">Stream Status</span>
                <span className="text-white font-semibold mt-0.5 inline-flex items-center gap-1.5 capitalize">
                  <Radio
                    className={`w-3.5 h-3.5 ${
                      streamStatus === 'error' ? 'text-[#FFB4AB]' : 'text-[#35D07F]'
                    }`}
                    aria-hidden="true"
                  />
                  {streamStatus === 'error' ? 'Unavailable' : streamStatus}
                </span>
              </div>
              <div>
                <span className="text-[#A7A9B2] block">Programme Guide</span>
                <span className="text-[#A7A9B2] mt-0.5 block">
                  {channel.epgAvailable ? 'EPG Loaded' : 'Not provided in M3U'}
                </span>
              </div>
            </div>

            {/* Cast Action */}
            <div className="pt-2 space-y-2.5">
              <button
                type="button"
                onClick={onCastToTv}
                disabled={isCastingBusy}
                className={`w-full min-h-[48px] px-4 py-3 rounded-xl font-display font-semibold text-sm flex items-center justify-center gap-2 transition-colors ${
                  isCastingActive
                    ? 'bg-[#35D07F]/20 border border-[#35D07F]/50 text-[#35D07F]'
                    : 'bg-[#7657FF] hover:bg-[#6646F5] text-white shadow-[0_4px_20px_rgba(118,87,255,0.35)]'
                }`}
              >
                <Cast className="w-4 h-4 shrink-0" aria-hidden="true" />
                <span className="truncate">
                  {isCastingBusy
                    ? 'Casting to TV…'
                    : isCastingActive && connectedTv
                    ? `Playing on ${connectedTv.name}`
                    : connectedTv
                    ? `Cast to ${connectedTv.name}`
                    : 'Cast to TV'}
                </span>
              </button>

              {castState === 'casting_failed' && (
                <p className="text-xs text-[#FFB4AB] flex items-center gap-1.5">
                  <Info className="w-3.5 h-3.5 shrink-0" aria-hidden="true" />
                  Casting failed — check that your TV receiver is awake on the same Wi-Fi.
                </p>
              )}

              {connectedTv && (
                <button
                  type="button"
                  onClick={onOpenRemote}
                  className="w-full min-h-[44px] px-4 py-2.5 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-sm font-medium text-white transition-colors"
                >
                  Open Wi-Fi Remote for {connectedTv.name}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Related Channels */}
      {relatedChannels.length > 0 && (
        <section aria-labelledby="related-channels-heading" className="space-y-3">
          <h2 id="related-channels-heading" className="font-display text-lg font-semibold text-white">
            Related Channels
          </h2>
          <div className="flex gap-3.5 overflow-x-auto no-scrollbar pb-1">
            {relatedChannels.map((rel) => (
              <button
                key={rel.id}
                type="button"
                onClick={() => onSelectChannel(rel)}
                className="w-52 shrink-0 rounded-2xl bg-[#242731] hover:bg-[#2a2e3a] border border-[#2E323E] hover:border-[#7657FF]/60 p-3.5 text-left transition-colors"
              >
                <div className="flex items-center justify-between gap-2 mb-2.5">
                  <ChannelLogo
                    name={rel.name}
                    logoUrl={rel.logoUrl}
                    category={rel.category}
                    size="md"
                  />
                  <LiveBadge compact />
                </div>
                <p className="font-display font-semibold text-sm text-white truncate">
                  {rel.name}
                </p>
                <p className="text-xs text-[#A7A9B2] truncate mt-0.5">
                  {rel.category}
                  {rel.language ? ` · ${rel.language}` : ''}
                </p>
              </button>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
