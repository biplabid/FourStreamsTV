import type { CategoryName, Channel } from '../types/iptv';

const POPULAR_CHANNEL_PRIORITY = [
  'dd national',
  'dd news',
  'sansad tv',
  'aaj tak',
  'ndtv india',
  'india today',
  'abp news',
  'republic bharat',
];

export function filterChannels(
  channels: Channel[],
  category: CategoryName,
  searchQuery: string
): Channel[] {
  const query = searchQuery.trim().toLowerCase();

  return channels.filter((channel) => {
    const matchesCategory = category === 'All' || channel.category === category;
    if (!matchesCategory) return false;

    if (!query) return true;

    return (
      channel.name.toLowerCase().includes(query) ||
      channel.category.toLowerCase().includes(query) ||
      (channel.groupRaw?.toLowerCase().includes(query) ?? false) ||
      (channel.language?.toLowerCase().includes(query) ?? false)
    );
  });
}

export function getPopularChannels(channels: Channel[], limit = 10): Channel[] {
  const scored = [...channels].sort((a, b) => {
    const aName = a.name.toLowerCase();
    const bName = b.name.toLowerCase();
    const aIdx = POPULAR_CHANNEL_PRIORITY.findIndex((p) => aName.includes(p));
    const bIdx = POPULAR_CHANNEL_PRIORITY.findIndex((p) => bName.includes(p));

    if (aIdx !== -1 && bIdx !== -1) return aIdx - bIdx;
    if (aIdx !== -1) return -1;
    if (bIdx !== -1) return 1;
    if (Boolean(a.logoUrl) !== Boolean(b.logoUrl)) return a.logoUrl ? -1 : 1;
    return a.name.localeCompare(b.name);
  });

  return scored.slice(0, limit);
}

export function getRelatedChannels(
  channels: Channel[],
  currentChannel: Channel | null,
  limit = 10
): Channel[] {
  if (!currentChannel) return channels.slice(0, limit);

  const sameCategory = channels.filter(
    (ch) => ch.id !== currentChannel.id && ch.category === currentChannel.category
  );
  const others = channels.filter(
    (ch) => ch.id !== currentChannel.id && ch.category !== currentChannel.category
  );

  return [...sameCategory, ...others].slice(0, limit);
}

export function getAdjacentChannel(
  channels: Channel[],
  currentChannel: Channel | null,
  direction: 'prev' | 'next'
): Channel | null {
  if (channels.length === 0) return null;
  if (!currentChannel) return channels[0];

  const currentIndex = channels.findIndex((ch) => ch.id === currentChannel.id);
  if (currentIndex === -1) return channels[0];

  const offset = direction === 'next' ? 1 : -1;
  const targetIndex = (currentIndex + offset + channels.length) % channels.length;
  return channels[targetIndex];
}
