import { useRef, useState } from 'react';
import {
  AlertTriangle,
  Cast,
  Heart,
  LayoutGrid,
  List,
  MoreVertical,
  Play,
  RefreshCw,
  Search,
  Tv,
  X,
} from 'lucide-react';
import type { CategoryName, Channel, PlaylistMeta, TvDevice, ViewMode } from '../types/iptv';
import { ChannelLogo } from './ChannelLogo';
import { LiveBadge } from './LiveBadge';

interface LiveTvBrowserProps {
  channels: Channel[];
  filteredChannels: Channel[];
  availableCategories: Exclude<CategoryName, 'All'>[];
  selectedCategory: CategoryName;
  searchQuery: string;
  viewMode: ViewMode;
  playlistMeta: PlaylistMeta;
  favoriteIds: Set<string>;
  connectedTv: TvDevice | null;
  searchInputRef?: React.RefObject<HTMLInputElement | null>;
  onSelectCategory: (cat: CategoryName) => void;
  onSearchChange: (q: string) => void;
  onViewModeChange: (mode: ViewMode) => void;
  onRefreshPlaylist: () => void;
  onSelectChannel: (channel: Channel) => void;
  onCastChannel: (channel: Channel) => void;
  onToggleFavorite: (channelId: string) => void;
}

export function LiveTvBrowser({
  channels,
  filteredChannels,
  availableCategories,
  selectedCategory,
  searchQuery,
  viewMode,
  playlistMeta,
  favoriteIds,
  connectedTv,
  searchInputRef,
  onSelectCategory,
  onSearchChange,
  onViewModeChange,
  onRefreshPlaylist,
  onSelectChannel,
  onCastChannel,
  onToggleFavorite,
}: LiveTvBrowserProps) {
  const [actionChannel, setActionChannel] = useState<Channel | null>(null);
  const longPressTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const filterChips: CategoryName[] = ['All', ...availableCategories];

  const startLongPress = (channel: Channel) => {
    if (longPressTimerRef.current) clearTimeout(longPressTimerRef.current);
    longPressTimerRef.current = setTimeout(() => {
      setActionChannel(channel);
    }, 500);
  };

  const cancelLongPress = () => {
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Live TV
          </h1>
          <p className="text-sm text-[#A7A9B2] mt-0.5">
            Explore Indian television ·{' '}
            <span className="tabular-nums text-white font-medium">{channels.length}</span> channels
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <div className="inline-flex items-center p-1 rounded-xl bg-[#1B1D23] border border-[#2E323E]">
            <button
              type="button"
              onClick={() => onViewModeChange('grid')}
              aria-label="Grid view"
              aria-pressed={viewMode === 'grid'}
              className={`min-h-[38px] min-w-[40px] px-2.5 rounded-lg flex items-center justify-center transition-colors ${
                viewMode === 'grid'
                  ? 'bg-[#7657FF] text-white'
                  : 'text-[#A7A9B2] hover:text-white'
              }`}
            >
              <LayoutGrid className="w-4 h-4" aria-hidden="true" />
            </button>
            <button
              type="button"
              onClick={() => onViewModeChange('list')}
              aria-label="List view"
              aria-pressed={viewMode === 'list'}
              className={`min-h-[38px] min-w-[40px] px-2.5 rounded-lg flex items-center justify-center transition-colors ${
                viewMode === 'list'
                  ? 'bg-[#7657FF] text-white'
                  : 'text-[#A7A9B2] hover:text-white'
              }`}
            >
              <List className="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          <button
            type="button"
            onClick={onRefreshPlaylist}
            disabled={playlistMeta.status === 'loading'}
            className="min-h-[44px] px-4 py-2 rounded-xl bg-[#1B1D23] hover:bg-[#242731] border border-[#2E323E] text-sm font-medium text-white flex items-center gap-2 transition-colors disabled:opacity-50 whitespace-nowrap"
            aria-label="Refresh playlist"
          >
            <RefreshCw
              className={`w-4 h-4 text-[#35C9E8] ${
                playlistMeta.status === 'loading' ? 'animate-spin' : ''
              }`}
              aria-hidden="true"
            />
            <span className="hidden sm:inline">Refresh</span>
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative">
        <label htmlFor="channel-search-input" className="sr-only">
          Search Indian TV channels
        </label>
        <Search
          className="w-4 h-4 text-[#A7A9B2] absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none"
          aria-hidden="true"
        />
        <input
          id="channel-search-input"
          ref={searchInputRef}
          type="search"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search channels by name, category, or language…"
          className="w-full h-12 pl-11 pr-12 rounded-xl bg-[#1B1D23] border border-[#2E323E] focus:border-[#35C9E8] focus:outline-none text-sm text-white placeholder:text-[#A7A9B2] transition-colors"
        />
        {searchQuery && (
          <button
            type="button"
            onClick={() => onSearchChange('')}
            aria-label="Clear search"
            className="w-10 h-10 rounded-lg flex items-center justify-center text-[#A7A9B2] hover:text-white absolute right-1 top-1/2 -translate-y-1/2"
          >
            <X className="w-4 h-4" aria-hidden="true" />
          </button>
        )}
      </div>

      {/* Category Filter Chips */}
      <div
        className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1"
        role="tablist"
        aria-label="Channel Categories"
      >
        {filterChips.map((cat) => {
          const isSelected = selectedCategory === cat;
          return (
            <button
              key={cat}
              type="button"
              role="tab"
              aria-selected={isSelected}
              onClick={() => onSelectCategory(cat)}
              className={`min-h-[40px] px-4 py-2 rounded-full text-xs font-display font-semibold tracking-wide transition-colors whitespace-nowrap shrink-0 border focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] ${
                isSelected
                  ? 'bg-[#7657FF] border-[#7657FF] text-white shadow-[0_4px_16px_rgba(118,87,255,0.3)]'
                  : 'bg-[#1B1D23] border-[#2E323E] text-[#A7A9B2] hover:text-white hover:border-[#484555]'
              }`}
            >
              {cat}
            </button>
          );
        })}
      </div>

      {/* Playlist Error / Offline Source Banner */}
      {playlistMeta.status === 'error' && (
        <div
          role="alert"
          className="rounded-2xl bg-[#93000A]/25 border border-[#FFB4AB]/40 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
        >
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-[#FFB4AB] shrink-0 mt-0.5" aria-hidden="true" />
            <div>
              <p className="text-sm font-semibold text-white">Unable to refresh IPTV playlist</p>
              <p className="text-xs text-[#FFDBD6]/85 mt-0.5">
                {playlistMeta.errorMessage || 'Check your internet connection and try again.'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onRefreshPlaylist}
            className="min-h-[40px] px-4 py-2 rounded-xl bg-[#FFB4AB] text-[#690005] text-xs font-bold shrink-0"
          >
            Retry
          </button>
        </div>
      )}

      {/* Loading Skeletons */}
      {playlistMeta.status === 'loading' && channels.length === 0 ? (
        <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-3.5" aria-label="Loading channels">
          {Array.from({ length: 8 }).map((_, i) => (
            <div
              key={i}
              className="h-36 rounded-2xl bg-[#1B1D23] border border-[#2E323E] animate-pulse p-4"
            />
          ))}
        </div>
      ) : filteredChannels.length === 0 ? (
        /* Empty Search / No Channels State */
        <div className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-10 text-center flex flex-col items-center">
          <div className="w-14 h-14 rounded-2xl bg-[#242731] border border-[#2E323E] flex items-center justify-center mb-4">
            <Tv className="w-7 h-7 text-[#A7A9B2]" aria-hidden="true" />
          </div>
          <h2 className="font-display text-lg font-semibold text-white">
            {searchQuery ? 'No matching channels' : 'No channels available'}
          </h2>
          <p className="text-sm text-[#A7A9B2] max-w-sm mt-1">
            {searchQuery
              ? `No Indian TV channels matched "${searchQuery}". Try another keyword or clear your filters.`
              : 'No channels were found in the current playlist.'}
          </p>
          <div className="flex items-center gap-2.5 mt-5">
            {searchQuery && (
              <button
                type="button"
                onClick={() => onSearchChange('')}
                className="min-h-[44px] px-5 py-2.5 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white text-sm font-semibold"
              >
                Clear Search
              </button>
            )}
            {selectedCategory !== 'All' && (
              <button
                type="button"
                onClick={() => onSelectCategory('All')}
                className="min-h-[44px] px-5 py-2.5 rounded-xl bg-[#242731] border border-[#2E323E] text-white text-sm font-medium"
              >
                Show All Categories
              </button>
            )}
          </div>
        </div>
      ) : viewMode === 'grid' ? (
        /* 2-Column Mobile -> 4-Column Desktop Grid */
        <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3.5">
          {filteredChannels.map((channel) => {
            const isFav = favoriteIds.has(channel.id);
            return (
              <div
                key={channel.id}
                onPointerDown={() => startLongPress(channel)}
                onPointerUp={cancelLongPress}
                onPointerLeave={cancelLongPress}
                onContextMenu={(e) => {
                  e.preventDefault();
                  setActionChannel(channel);
                }}
                className="group rounded-2xl bg-[#242731] border border-[#2E323E] hover:border-[#7657FF]/70 p-3.5 flex flex-col justify-between gap-3 transition-colors"
              >
                <div className="flex items-start justify-between gap-2">
                  <button
                    type="button"
                    onClick={() => onSelectChannel(channel)}
                    className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-xl"
                    aria-label={`Play ${channel.name}`}
                  >
                    <ChannelLogo
                      name={channel.name}
                      logoUrl={channel.logoUrl}
                      category={channel.category}
                      size="lg"
                    />
                  </button>

                  <div className="flex items-center gap-0.5">
                    <button
                      type="button"
                      onClick={() => onToggleFavorite(channel.id)}
                      className="w-10 h-10 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white hover:bg-[#1B1D23] transition-colors"
                      aria-label={
                        isFav
                          ? `Remove ${channel.name} from favorites`
                          : `Add ${channel.name} to favorites`
                      }
                    >
                      <Heart
                        className={`w-4 h-4 ${isFav ? 'fill-[#7657FF] text-[#7657FF]' : ''}`}
                        aria-hidden="true"
                      />
                    </button>
                    <button
                      type="button"
                      onClick={() => setActionChannel(channel)}
                      className="w-9 h-10 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white hover:bg-[#1B1D23] transition-colors"
                      aria-label={`More actions for ${channel.name}`}
                    >
                      <MoreVertical className="w-4 h-4" aria-hidden="true" />
                    </button>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => onSelectChannel(channel)}
                  className="text-left space-y-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-lg"
                >
                  <div className="flex items-center justify-between gap-2">
                    <p className="font-display font-semibold text-sm text-white truncate">
                      {channel.name}
                    </p>
                  </div>
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs text-[#A7A9B2] truncate">
                      {channel.category}
                      {channel.language ? ` · ${channel.language}` : ''}
                    </span>
                    {channel.isLive && <LiveBadge compact />}
                  </div>
                </button>
              </div>
            );
          })}
        </div>
      ) : (
        /* Compact List Layout */
        <div className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] divide-y divide-[#2E323E] overflow-hidden">
          {filteredChannels.map((channel) => {
            const isFav = favoriteIds.has(channel.id);
            return (
              <div
                key={channel.id}
                onPointerDown={() => startLongPress(channel)}
                onPointerUp={cancelLongPress}
                onPointerLeave={cancelLongPress}
                className="px-4 py-3 flex items-center justify-between gap-3 hover:bg-[#242731]/70 transition-colors"
              >
                <button
                  type="button"
                  onClick={() => onSelectChannel(channel)}
                  className="flex items-center gap-3.5 min-w-0 flex-1 text-left min-h-[48px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-xl"
                >
                  <ChannelLogo
                    name={channel.name}
                    logoUrl={channel.logoUrl}
                    category={channel.category}
                    size="md"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-display font-semibold text-sm text-white truncate">
                        {channel.name}
                      </p>
                      {channel.isLive && <LiveBadge compact />}
                    </div>
                    <p className="text-xs text-[#A7A9B2] truncate mt-0.5">
                      {channel.category}
                      {channel.language ? ` · ${channel.language}` : ''}
                      {channel.quality ? ` · ${channel.quality}` : ''}
                    </p>
                  </div>
                </button>

                <div className="flex items-center gap-1 shrink-0">
                  <button
                    type="button"
                    onClick={() => onToggleFavorite(channel.id)}
                    className="w-11 h-11 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white hover:bg-[#242731]"
                    aria-label={
                      isFav
                        ? `Remove ${channel.name} from favorites`
                        : `Add ${channel.name} to favorites`
                    }
                  >
                    <Heart
                      className={`w-4 h-4 ${isFav ? 'fill-[#7657FF] text-[#7657FF]' : ''}`}
                      aria-hidden="true"
                    />
                  </button>
                  <button
                    type="button"
                    onClick={() => setActionChannel(channel)}
                    className="w-11 h-11 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white hover:bg-[#242731]"
                    aria-label={`More actions for ${channel.name}`}
                  >
                    <MoreVertical className="w-4 h-4" aria-hidden="true" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Channel Action Bottom Sheet (Long press or Options) */}
      {actionChannel && (
        <div
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
          role="dialog"
          aria-modal="true"
          aria-label={`Actions for ${actionChannel.name}`}
          onClick={() => setActionChannel(null)}
        >
          <div
            className="w-full sm:max-w-md bg-[#1B1D23] border border-[#2E323E] rounded-t-3xl sm:rounded-3xl p-5 space-y-4 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="w-10 h-1 bg-[#2E323E] rounded-full mx-auto sm:hidden" />

            <div className="flex items-center gap-3">
              <ChannelLogo
                name={actionChannel.name}
                logoUrl={actionChannel.logoUrl}
                category={actionChannel.category}
                size="lg"
              />
              <div className="min-w-0 flex-1">
                <h3 className="font-display font-bold text-base text-white truncate">
                  {actionChannel.name}
                </h3>
                <p className="text-xs text-[#A7A9B2]">
                  {actionChannel.category}
                  {actionChannel.language ? ` · ${actionChannel.language}` : ''}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setActionChannel(null)}
                className="w-10 h-10 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white"
                aria-label="Close channel actions"
              >
                <X className="w-5 h-5" aria-hidden="true" />
              </button>
            </div>

            <div className="space-y-2">
              <button
                type="button"
                onClick={() => {
                  const target = actionChannel;
                  setActionChannel(null);
                  onSelectChannel(target);
                }}
                className="w-full min-h-[48px] px-4 py-3 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white font-display font-semibold text-sm flex items-center gap-3"
              >
                <Play className="w-4 h-4 fill-current" aria-hidden="true" />
                <span>Play on This Device</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  const target = actionChannel;
                  setActionChannel(null);
                  onCastChannel(target);
                }}
                className="w-full min-h-[48px] px-4 py-3 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white font-medium text-sm flex items-center gap-3"
              >
                <Cast className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
                <span>
                  {connectedTv ? `Cast to ${connectedTv.name}` : 'Connect & Cast to TV'}
                </span>
              </button>

              <button
                type="button"
                onClick={() => {
                  onToggleFavorite(actionChannel.id);
                  setActionChannel(null);
                }}
                className="w-full min-h-[48px] px-4 py-3 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white font-medium text-sm flex items-center gap-3"
              >
                <Heart
                  className={`w-4 h-4 ${
                    favoriteIds.has(actionChannel.id) ? 'fill-[#7657FF] text-[#7657FF]' : ''
                  }`}
                  aria-hidden="true"
                />
                <span>
                  {favoriteIds.has(actionChannel.id)
                    ? 'Remove from Favorites'
                    : 'Add to Favorites'}
                </span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
