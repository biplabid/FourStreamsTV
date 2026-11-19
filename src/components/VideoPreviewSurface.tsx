import { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import {
  AlertCircle,
  Cast,
  Maximize2,
  Minimize2,
  Pause,
  Play,
  RefreshCw,
  SkipForward,
  Volume2,
  VolumeX,
} from 'lucide-react';
import type { Channel, PlayerPlaybackStatus } from '../types/iptv';
import { ChannelLogo } from './ChannelLogo';
import { LiveBadge } from './LiveBadge';

interface VideoPreviewSurfaceProps {
  channel: Channel;
  isPlaying: boolean;
  isMuted: boolean;
  volume: number;
  castTargetName?: string | null;
  onStatusChange?: (status: PlayerPlaybackStatus) => void;
  onTogglePlay: () => void;
  onNextChannel?: () => void;
  onCastClick?: () => void;
  compact?: boolean;
}

export function VideoPreviewSurface({
  channel,
  isPlaying,
  isMuted,
  volume,
  castTargetName,
  onStatusChange,
  onTogglePlay,
  onNextChannel,
  onCastClick,
  compact = false,
}: VideoPreviewSurfaceProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);

  const [status, setStatus] = useState<PlayerPlaybackStatus>('loading');
  const [errorDetail, setErrorDetail] = useState<string>('');
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [retryToken, setRetryToken] = useState(0);

  const updateStatus = (next: PlayerPlaybackStatus) => {
    setStatus(next);
    onStatusChange?.(next);
  };

  useEffect(() => {
    const onFsChange = () => {
      setIsFullscreen(Boolean(document.fullscreenElement));
    };
    document.addEventListener('fullscreenchange', onFsChange);
    return () => document.removeEventListener('fullscreenchange', onFsChange);
  }, []);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    setErrorDetail('');
    updateStatus('loading');

    const streamUrl = channel.streamUrl;
    let cancelled = false;

    const failTimer = setTimeout(() => {
      if (!cancelled && video.readyState < 2) {
        setErrorDetail(
          channel.isGeoRestricted
            ? 'This broadcast is geo-restricted or currently offline from the source.'
            : 'The live stream did not respond in time or may be temporarily offline.'
        );
        updateStatus('error');
      }
    }, 10000);

    const handleCanPlay = () => {
      if (cancelled) return;
      clearTimeout(failTimer);
      if (isPlaying) {
        video
          .play()
          .then(() => {
            if (!cancelled) updateStatus('playing');
          })
          .catch(() => {
            // Autoplay blocked with audio; stay paused gracefully
            if (!cancelled) updateStatus('paused');
          });
      } else {
        updateStatus('paused');
      }
    };

    const handleVideoError = () => {
      if (cancelled) return;
      clearTimeout(failTimer);
      setErrorDetail('Stream unavailable or incompatible format in browser.');
      updateStatus('error');
    };

    video.addEventListener('canplay', handleCanPlay);
    video.addEventListener('error', handleVideoError);

    if (Hls.isSupported() && /\.m3u8(\?|$)/i.test(streamUrl)) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        manifestLoadingTimeOut: 7000,
        fragLoadingTimeOut: 7000,
      });
      hlsRef.current = hls;
      hls.loadSource(streamUrl);
      hls.attachMedia(video);
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (data.fatal && !cancelled) {
          clearTimeout(failTimer);
          setErrorDetail(
            channel.isGeoRestricted
              ? 'Channel stream is geo-restricted or offline.'
              : 'Live stream could not be loaded (network, CORS, or broadcaster offline).'
          );
          updateStatus('error');
          hls.destroy();
          hlsRef.current = null;
        }
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl') || !/\.m3u8/i.test(streamUrl)) {
      video.src = streamUrl;
    } else {
      clearTimeout(failTimer);
      setErrorDetail('HLS playback is not supported on this browser.');
      updateStatus('error');
    }

    return () => {
      cancelled = true;
      clearTimeout(failTimer);
      video.removeEventListener('canplay', handleCanPlay);
      video.removeEventListener('error', handleVideoError);
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      video.removeAttribute('src');
      video.load();
    };
  }, [channel.id, channel.streamUrl, retryToken]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = isMuted;
    video.volume = Math.max(0, Math.min(1, volume / 100));
  }, [isMuted, volume]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || status === 'error' || status === 'loading') return;

    if (isPlaying) {
      video
        .play()
        .then(() => updateStatus('playing'))
        .catch(() => updateStatus('paused'));
    } else {
      video.pause();
      updateStatus('paused');
    }
  }, [isPlaying]);

  const handleToggleFullscreen = async () => {
    const el = containerRef.current;
    if (!el) return;
    try {
      if (!document.fullscreenElement) {
        await el.requestFullscreen();
      } else {
        await document.exitFullscreen();
      }
    } catch {
      // Fullscreen may be restricted inside sandboxed iframes
    }
  };

  return (
    <div
      ref={containerRef}
      className="relative w-full aspect-video bg-[#0d0e11] rounded-2xl overflow-hidden border border-[#2E323E] group select-none"
      data-testid="video-preview-container"
    >
      <video
        ref={videoRef}
        playsInline
        muted={isMuted}
        className="w-full h-full object-contain bg-black"
        aria-label={`${channel.name} live video player`}
      />

      {/* Ambient Backdrop when loading or paused */}
      {(status === 'loading' || status === 'paused') && (
        <div
          className="absolute inset-0 bg-gradient-to-br from-[#1B1D23]/90 via-[#101114]/90 to-[#242731]/90 pointer-events-none"
          aria-hidden="true"
        />
      )}

      {/* Top Overlay: Channel Logo + Live Status */}
      <div className="absolute top-0 inset-x-0 p-3 sm:p-4 bg-gradient-to-b from-black/80 via-black/35 to-transparent flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5 min-w-0">
          <ChannelLogo
            name={channel.name}
            logoUrl={channel.logoUrl}
            category={channel.category}
            size="sm"
          />
          <div className="min-w-0">
            <p className="font-display font-semibold text-sm sm:text-base text-white truncate">
              {channel.name}
            </p>
            <p className="text-xs text-[#A7A9B2] truncate">
              {channel.category}
              {channel.language ? ` · ${channel.language}` : ''}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {castTargetName && (
            <span className="hidden sm:inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#35C9E8]/15 border border-[#35C9E8]/40 text-[#35C9E8] text-xs font-medium whitespace-nowrap">
              <Cast className="w-3.5 h-3.5" aria-hidden="true" />
              Casting to {castTargetName}
            </span>
          )}
          <LiveBadge compact={compact} />
        </div>
      </div>

      {/* Center State: Loading or Unavailable */}
      {status === 'loading' && (
        <div className="absolute inset-0 flex flex-col items-center justify-center p-4 text-center">
          <div className="w-10 h-10 rounded-full border-2 border-[#7657FF]/30 border-t-[#7657FF] animate-spin mb-3" />
          <p className="font-display text-sm font-semibold text-white">
            Connecting to live stream…
          </p>
          <p className="text-xs text-[#A7A9B2] mt-1">{channel.name}</p>
        </div>
      )}

      {status === 'error' && (
        <div className="absolute inset-0 bg-[#101114]/92 flex flex-col items-center justify-center p-4 sm:p-6 text-center">
          <div className="w-12 h-12 rounded-2xl bg-[#242731] border border-[#2E323E] flex items-center justify-center mb-3">
            <AlertCircle className="w-6 h-6 text-[#FFB4AB]" aria-hidden="true" />
          </div>
          <h3 className="font-display font-semibold text-base text-white">
            Stream Unavailable
          </h3>
          <p className="text-xs sm:text-sm text-[#A7A9B2] max-w-md mt-1 leading-relaxed">
            {errorDetail || 'This IPTV stream is currently unreachable.'}
          </p>
          <div className="flex flex-wrap items-center justify-center gap-2.5 mt-4">
            <button
              type="button"
              onClick={() => setRetryToken((t) => t + 1)}
              className="min-h-[40px] px-4 py-2 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white text-xs font-semibold flex items-center gap-1.5 transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5" aria-hidden="true" />
              Retry Stream
            </button>
            {onNextChannel && (
              <button
                type="button"
                onClick={onNextChannel}
                className="min-h-[40px] px-4 py-2 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white text-xs font-semibold flex items-center gap-1.5 transition-colors"
              >
                <SkipForward className="w-3.5 h-3.5" aria-hidden="true" />
                Next Channel
              </button>
            )}
          </div>
        </div>
      )}

      {/* Bottom Overlay Quick Controls */}
      <div className="absolute bottom-0 inset-x-0 p-3 sm:p-4 bg-gradient-to-t from-black/85 via-black/40 to-transparent flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onTogglePlay}
            className="w-10 h-10 rounded-full bg-[#1B1D23]/85 hover:bg-[#7657FF] border border-white/10 flex items-center justify-center text-white transition-colors"
            aria-label={isPlaying ? 'Pause stream' : 'Play stream'}
          >
            {isPlaying ? (
              <Pause className="w-4 h-4" aria-hidden="true" />
            ) : (
              <Play className="w-4 h-4 ml-0.5" aria-hidden="true" />
            )}
          </button>
          <span className="text-xs font-medium text-[#E3E2E6] hidden sm:inline">
            {status === 'playing'
              ? 'Playing live'
              : status === 'paused'
              ? 'Paused'
              : status === 'loading'
              ? 'Buffering'
              : 'Stream offline'}
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          <span className="px-2 py-0.5 rounded bg-black/50 text-[11px] font-mono text-[#A7A9B2] flex items-center gap-1">
            {isMuted || volume === 0 ? (
              <VolumeX className="w-3.5 h-3.5" aria-hidden="true" />
            ) : (
              <Volume2 className="w-3.5 h-3.5" aria-hidden="true" />
            )}
            {isMuted ? 'Muted' : `${volume}%`}
          </span>

          {onCastClick && (
            <button
              type="button"
              onClick={onCastClick}
              className="w-10 h-10 rounded-full bg-[#1B1D23]/85 hover:bg-[#35C9E8]/20 border border-white/10 flex items-center justify-center text-white transition-colors"
              aria-label="Cast to TV"
            >
              <Cast className="w-4 h-4" aria-hidden="true" />
            </button>
          )}

          <button
            type="button"
            onClick={handleToggleFullscreen}
            className="w-10 h-10 rounded-full bg-[#1B1D23]/85 hover:bg-[#242731] border border-white/10 flex items-center justify-center text-white transition-colors"
            aria-label={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
          >
            {isFullscreen ? (
              <Minimize2 className="w-4 h-4" aria-hidden="true" />
            ) : (
              <Maximize2 className="w-4 h-4" aria-hidden="true" />
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
