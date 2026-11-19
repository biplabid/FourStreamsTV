import {
  Cast,
  ChevronRight,
  Compass,
  Film,
  Heart,
  Music,
  Newspaper,
  Play,
  Radio,
  Search,
  Sparkles,
  Trophy,
  Tv,
} from 'lucide-react';
import type { CategoryName, Channel, TvDevice } from '../types/iptv';
import { ChannelLogo } from './ChannelLogo';
import { LiveBadge } from './LiveBadge';
import { VideoPreviewSurface } from './VideoPreviewSurface';

interface HomeDashboardProps {
  featuredChannel: Channel | null;
  continueWatching: Channel[];
  popularChannels: Channel[];
  availableCategories: Exclude<CategoryName, 'All'>[];
  connectedTv: TvDevice | null;
  isPlaying: boolean;
  isMuted: boolean;
  volume: number;
  favoriteIds: Set<string>;
  onTogglePlay: () => void;
  onSelectChannel: (channel: Channel) => void;
  onBrowseLiveTv: (category?: CategoryName) => void;
  onOpenSearch: () => void;
  onOpenTvDiscovery: () => void;
  onToggleFavorite: (channelId: string) => void;
}

const CATEGORY_ICONS: Record<Exclude<CategoryName, 'All'>, typeof Newspaper> = {
  News: Newspaper,
  Entertainment: Tv,
  Movies: Film,
  Sports: Trophy,
  Music: Music,
  Regional: Compass,
  Kids: Sparkles,
  Devotional: Radio,
  Others: Tv,
};

export function HomeDashboard({
  featuredChannel,
  continueWatching,
  popularChannels,
  availableCategories,
  connectedTv,
  isPlaying,
  isMuted,
  volume,
  favoriteIds,
  onTogglePlay,
  onSelectChannel,
  onBrowseLiveTv,
  onOpenSearch,
  onOpenTvDiscovery,
  onToggleFavorite,
}: HomeDashboardProps) {
  return (
    <div className="space-y-8">
      {/* Home Header */}
      <section className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-[#A7A9B2]">Good Evening</p>
          <h1 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white mt-0.5 text-balance">
            Live Indian Television
          </h1>
        </div>

        <div className="flex items-center gap-2.5 flex-wrap">
          <button
            type="button"
            onClick={onOpenTvDiscovery}
            className={`min-h-[44px] px-4 py-2 rounded-xl border text-sm font-medium flex items-center gap-2 transition-colors whitespace-nowrap shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] ${
              connectedTv
                ? 'bg-[#35D07F]/15 border-[#35D07F]/40 text-[#35D07F]'
                : 'bg-[#1B1D23] border-[#2E323E] text-white hover:border-[#7657FF]/60'
            }`}
          >
            <Cast className="w-4 h-4 shrink-0" aria-hidden="true" />
            <span>{connectedTv ? `Connected: ${connectedTv.name}` : 'Connect to TV'}</span>
          </button>

          <button
            type="button"
            onClick={onOpenSearch}
            className="hidden lg:inline-flex min-h-[44px] px-4 py-2 rounded-xl bg-[#1B1D23] hover:bg-[#242731] border border-[#2E323E] text-sm font-medium text-[#A7A9B2] hover:text-white items-center gap-2 transition-colors whitespace-nowrap shrink-0"
          >
            <Search className="w-4 h-4" aria-hidden="true" />
            <span>Search Channels</span>
          </button>
        </div>
      </section>

      {/* Featured Player Section */}
      <section aria-labelledby="featured-player-heading" className="space-y-3">
        <div className="flex items-center justify-between gap-2">
          <h2 id="featured-player-heading" className="font-display text-lg font-semibold text-white">
            {featuredChannel ? 'Now Previewing' : 'Featured Stage'}
          </h2>
          {featuredChannel && (
            <button
              type="button"
              onClick={() => onSelectChannel(featuredChannel)}
              className="text-xs font-medium text-[#35C9E8] hover:text-white flex items-center gap-1 min-h-[36px] px-2"
            >
              <span>Open Full Player</span>
              <ChevronRight className="w-4 h-4" aria-hidden="true" />
            </button>
          )}
        </div>

        {featuredChannel ? (
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-stretch">
            <div className="xl:col-span-8">
              <VideoPreviewSurface
                channel={featuredChannel}
                isPlaying={isPlaying}
                isMuted={isMuted}
                volume={volume}
                castTargetName={connectedTv?.name ?? null}
                onTogglePlay={onTogglePlay}
                onCastClick={onOpenTvDiscovery}
                compact
              />
            </div>

            <div className="xl:col-span-4 bg-[#1B1D23] border border-[#2E323E] rounded-2xl p-5 flex flex-col justify-between gap-5">
              <div className="space-y-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <ChannelLogo
                      name={featuredChannel.name}
                      logoUrl={featuredChannel.logoUrl}
                      category={featuredChannel.category}
                      size="lg"
                    />
                    <div className="min-w-0">
                      <h3 className="font-display text-lg font-bold text-white truncate">
                        {featuredChannel.name}
                      </h3>
                      <p className="text-xs text-[#A7A9B2] mt-0.5 truncate">
                        {featuredChannel.category}
                        {featuredChannel.language ? ` · ${featuredChannel.language}` : ''}
                        {featuredChannel.quality ? ` · ${featuredChannel.quality}` : ''}
                      </p>
                    </div>
                  </div>
                  <LiveBadge compact />
                </div>

                <p className="text-xs text-[#A7A9B2] leading-relaxed">
                  {connectedTv
                    ? `Ready to cast directly to ${connectedTv.name} (${connectedTv.protocol}).`
                    : 'Streaming locally on your device. Connect a compatible Android TV or Google TV to cast.'}
                </p>
              </div>

              <div className="flex flex-col sm:flex-row xl:flex-col gap-2.5">
                <button
                  type="button"
                  onClick={() => onSelectChannel(featuredChannel)}
                  className="w-full min-h-[48px] px-4 py-2.5 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white font-display font-semibold text-sm flex items-center justify-center gap-2 shadow-[0_4px_20px_rgba(118,87,255,0.35)] transition-colors"
                >
                  <Play className="w-4 h-4 fill-current" aria-hidden="true" />
                  <span>Watch in Player</span>
                </button>
                <button
                  type="button"
                  onClick={onOpenTvDiscovery}
                  className="w-full min-h-[44px] px-4 py-2.5 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white text-sm font-medium flex items-center justify-center gap-2 transition-colors"
                >
                  <Cast className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
                  <span>{connectedTv ? `Cast to ${connectedTv.name}` : 'Cast to TV'}</span>
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="relative overflow-hidden rounded-2xl border border-[#2E323E] bg-gradient-to-br from-[#242731] via-[#1B1D23] to-[#101114] p-6 sm:p-10">
            <div className="max-w-xl space-y-4">
              <p className="text-xs font-display font-semibold uppercase tracking-widest text-[#35C9E8]">
                StreamCast Live IPTV
              </p>
              <h3 className="font-display text-2xl sm:text-3xl font-bold text-white tracking-tight text-balance">
                Your entertainment starts here
              </h3>
              <p className="text-sm text-[#A7A9B2] leading-relaxed">
                Pick any live Indian television channel to preview locally on your screen or cast wirelessly to your Android TV or Google TV.
              </p>
              <div className="flex flex-wrap items-center gap-3 pt-1">
                <button
                  type="button"
                  onClick={() => onBrowseLiveTv('All')}
                  className="min-h-[48px] px-6 py-3 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white font-display font-semibold text-sm flex items-center gap-2 shadow-[0_4px_20px_rgba(118,87,255,0.35)] transition-colors whitespace-nowrap"
                >
                  <Radio className="w-4 h-4" aria-hidden="true" />
                  <span>Browse Live Channels</span>
                </button>
                <button
                  type="button"
                  onClick={onOpenTvDiscovery}
                  className="min-h-[48px] px-5 py-3 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white text-sm font-medium flex items-center gap-2 transition-colors whitespace-nowrap"
                >
                  <Cast className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
                  <span>Find My TV</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* Continue Watching */}
      {continueWatching.length > 0 && (
        <section aria-labelledby="continue-watching-heading" className="space-y-3">
          <div className="flex items-center justify-between gap-2">
            <h2 id="continue-watching-heading" className="font-display text-lg font-semibold text-white">
              Continue Watching
            </h2>
            <span className="text-xs text-[#A7A9B2] tabular-nums">
              {continueWatching.length} recent
            </span>
          </div>

          <div className="flex gap-3.5 overflow-x-auto no-scrollbar pb-1">
            {continueWatching.map((channel) => (
              <button
                key={channel.id}
                type="button"
                onClick={() => onSelectChannel(channel)}
                className="w-56 shrink-0 rounded-2xl bg-[#242731] hover:bg-[#292d39] border border-[#2E323E] hover:border-[#7657FF]/60 p-3.5 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF]"
              >
                <div className="flex items-center justify-between gap-2 mb-3">
                  <ChannelLogo
                    name={channel.name}
                    logoUrl={channel.logoUrl}
                    category={channel.category}
                    size="md"
                  />
                  {channel.isLive && <LiveBadge compact />}
                </div>
                <p className="font-display font-semibold text-sm text-white truncate">
                  {channel.name}
                </p>
                <p className="text-xs text-[#A7A9B2] truncate mt-0.5">
                  {channel.category}
                  {channel.language ? ` · ${channel.language}` : ''}
                </p>
              </button>
            ))}
          </div>
        </section>
      )}

      {/* Popular Channels */}
      <section aria-labelledby="popular-channels-heading" className="space-y-3">
        <div className="flex items-center justify-between gap-2">
          <h2 id="popular-channels-heading" className="font-display text-lg font-semibold text-white">
            Popular Channels
          </h2>
          <button
            type="button"
            onClick={() => onBrowseLiveTv('All')}
            className="text-xs font-medium text-[#35C9E8] hover:text-white flex items-center gap-1 min-h-[36px] px-2"
          >
            <span>See all</span>
            <ChevronRight className="w-4 h-4" aria-hidden="true" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
          {popularChannels.slice(0, 8).map((channel) => {
            const isFav = favoriteIds.has(channel.id);
            return (
              <div
                key={channel.id}
                className="rounded-2xl bg-[#242731] border border-[#2E323E] hover:border-[#7657FF]/60 p-3.5 flex items-center justify-between gap-3 transition-colors"
              >
                <button
                  type="button"
                  onClick={() => onSelectChannel(channel)}
                  className="flex items-center gap-3 min-w-0 flex-1 text-left min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-lg"
                >
                  <ChannelLogo
                    name={channel.name}
                    logoUrl={channel.logoUrl}
                    category={channel.category}
                    size="md"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="font-display font-semibold text-sm text-white truncate">
                      {channel.name}
                    </p>
                    <p className="text-xs text-[#A7A9B2] truncate mt-0.5">
                      {channel.category}
                      {channel.quality ? ` · ${channel.quality}` : ''}
                    </p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => onToggleFavorite(channel.id)}
                  className="w-10 h-10 shrink-0 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white hover:bg-[#1B1D23] transition-colors"
                  aria-label={isFav ? `Remove ${channel.name} from favorites` : `Add ${channel.name} to favorites`}
                >
                  <Heart
                    className={`w-4 h-4 ${isFav ? 'fill-[#7657FF] text-[#7657FF]' : ''}`}
                    aria-hidden="true"
                  />
                </button>
              </div>
            );
          })}
        </div>
      </section>

      {/* Categories supported by loaded playlist */}
      {availableCategories.length > 0 && (
        <section aria-labelledby="categories-heading" className="space-y-3">
          <h2 id="categories-heading" className="font-display text-lg font-semibold text-white">
            Browse by Category
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
            {availableCategories.map((category) => {
              const Icon = CATEGORY_ICONS[category] ?? Tv;
              return (
                <button
                  key={category}
                  type="button"
                  onClick={() => onBrowseLiveTv(category)}
                  className="min-h-[60px] rounded-2xl bg-[#1B1D23] hover:bg-[#242731] border border-[#2E323E] hover:border-[#7657FF]/60 px-4 py-3 flex items-center gap-3 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF]"
                >
                  <div className="w-9 h-9 rounded-xl bg-[#242731] border border-[#2E323E] flex items-center justify-center shrink-0">
                    <Icon className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
                  </div>
                  <span className="font-display font-semibold text-sm text-white truncate">
                    {category}
                  </span>
                </button>
              );
            })}
          </div>
        </section>
      )}
    </div>
  );
}
