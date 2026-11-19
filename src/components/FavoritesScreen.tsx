import { Cast, Heart, Play, Radio, Trash2 } from 'lucide-react';
import type { Channel, TvDevice } from '../types/iptv';
import { ChannelLogo } from './ChannelLogo';
import { LiveBadge } from './LiveBadge';

interface FavoritesScreenProps {
  favoriteChannels: Channel[];
  connectedTv: TvDevice | null;
  onSelectChannel: (channel: Channel) => void;
  onCastChannel: (channel: Channel) => void;
  onRemoveFavorite: (channelId: string) => void;
  onExploreChannels: () => void;
}

export function FavoritesScreen({
  favoriteChannels,
  connectedTv,
  onSelectChannel,
  onCastChannel,
  onRemoveFavorite,
  onExploreChannels,
}: FavoritesScreenProps) {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Favorites
          </h1>
          <p className="text-sm text-[#A7A9B2] mt-0.5">
            Quick access to your saved channels ·{' '}
            <span className="tabular-nums text-white font-medium">{favoriteChannels.length}</span> saved
          </p>
        </div>
      </div>

      {favoriteChannels.length === 0 ? (
        <div className="rounded-3xl bg-[#1B1D23] border border-[#2E323E] p-10 text-center max-w-lg mx-auto space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-[#242731] border border-[#2E323E] flex items-center justify-center mx-auto">
            <Heart className="w-8 h-8 text-[#7657FF]" aria-hidden="true" />
          </div>
          <div className="space-y-1.5">
            <h2 className="font-display text-xl font-bold text-white">No favourites yet</h2>
            <p className="text-sm text-[#A7A9B2] leading-relaxed">
              Tap the heart icon on any Indian live television channel to save it here for instant playback or casting.
            </p>
          </div>
          <button
            type="button"
            onClick={onExploreChannels}
            className="min-h-[48px] px-6 py-3 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white font-display font-semibold text-sm inline-flex items-center gap-2 shadow-[0_4px_20px_rgba(118,87,255,0.35)] transition-colors"
          >
            <Radio className="w-4 h-4" aria-hidden="true" />
            <span>Explore Channels</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {favoriteChannels.map((channel) => (
            <div
              key={channel.id}
              className="rounded-2xl bg-[#242731] border border-[#2E323E] hover:border-[#7657FF]/60 p-4 flex flex-col justify-between gap-4 transition-colors"
            >
              <div className="flex items-start justify-between gap-3">
                <button
                  type="button"
                  onClick={() => onSelectChannel(channel)}
                  className="flex items-center gap-3 min-w-0 flex-1 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-xl"
                >
                  <ChannelLogo
                    name={channel.name}
                    logoUrl={channel.logoUrl}
                    category={channel.category}
                    size="lg"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-display font-bold text-base text-white truncate">
                        {channel.name}
                      </p>
                      <LiveBadge compact />
                    </div>
                    <p className="text-xs text-[#A7A9B2] mt-0.5 truncate">
                      {channel.category}
                      {channel.language ? ` · ${channel.language}` : ''}
                    </p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => onRemoveFavorite(channel.id)}
                  className="w-10 h-10 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-[#FFB4AB] hover:bg-[#1B1D23] transition-colors shrink-0"
                  aria-label={`Remove ${channel.name} from favorites`}
                >
                  <Trash2 className="w-4 h-4" aria-hidden="true" />
                </button>
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                <button
                  type="button"
                  onClick={() => onSelectChannel(channel)}
                  className="min-h-[42px] px-3 py-2 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white text-xs font-display font-semibold flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Play className="w-3.5 h-3.5 fill-current" aria-hidden="true" />
                  <span>Play</span>
                </button>
                <button
                  type="button"
                  onClick={() => onCastChannel(channel)}
                  className="min-h-[42px] px-3 py-2 rounded-xl bg-[#1B1D23] hover:bg-[#2E323E] border border-[#2E323E] text-white text-xs font-medium flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Cast className="w-3.5 h-3.5 text-[#35C9E8]" aria-hidden="true" />
                  <span className="truncate">
                    {connectedTv ? 'Cast Now' : 'Cast TV'}
                  </span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
