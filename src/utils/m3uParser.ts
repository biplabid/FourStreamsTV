import type { CategoryName, Channel } from '../types/iptv';

const CATEGORY_ORDER: Exclude<CategoryName, 'All'>[] = [
  'News',
  'Entertainment',
  'Movies',
  'Sports',
  'Music',
  'Regional',
  'Kids',
  'Devotional',
  'Others',
];

export function normalizeCategory(
  groupRaw?: string,
  channelName?: string
): Exclude<CategoryName, 'All'> {
  const combined = `${groupRaw ?? ''} ${channelName ?? ''}`.toLowerCase();

  if (/\b(news|samachar|aaj tak|ndtv|dd news|sansad|republic|abp|india today|cnbc|wion|times now|cnn)\b/.test(combined)) {
    return 'News';
  }
  if (/\b(movie|cinema|film|gold|pictures|plex|bollywood|max|action)\b/.test(combined)) {
    return 'Movies';
  }
  if (/\b(sport|cricket|football|ten|sony six|dd sports|espn|euro|star sports)\b/.test(combined)) {
    return 'Sports';
  }
  if (/\b(music|mtv|9xm|b4u music|mastiii|zoom|songs|hits|vh1)\b/.test(combined)) {
    return 'Music';
  }
  if (/\b(kid|cartoon|pogo|nick|disney|hungama|sonic|chutti|bal)\b/.test(combined)) {
    return 'Kids';
  }
  if (/\b(devotional|religious|aastha|sanskar|bhakti|spirit|god|peace|sharnam|mh1 shraddha)\b/.test(combined)) {
    return 'Devotional';
  }
  if (/\b(regional|tamil|telugu|malayalam|kannada|marathi|bengali|punjabi|gujarati|odia|assamese|bhojpuri|sun tv|etv|zeetelugu|kairali)\b/.test(combined)) {
    return 'Regional';
  }
  if (/\b(entertainment|general|sab|sony|star plus|colors|zee tv|dd national|dd bharati|comedy|lifestyle)\b/.test(combined)) {
    return 'Entertainment';
  }
  return 'Others';
}

function extractAttribute(extinfLine: string, attrName: string): string | undefined {
  const regex = new RegExp(`${attrName}="([^"]*)"`, 'i');
  const match = extinfLine.match(regex);
  const value = match?.[1]?.trim();
  return value && value.length > 0 ? value : undefined;
}

function extractQualityAndCleanName(rawName: string): {
  cleanName: string;
  quality?: string;
  isGeoRestricted: boolean;
} {
  let cleanName = rawName.trim();
  let quality: string | undefined;
  let isGeoRestricted = false;

  if (/\[geo-blocked\]|\[not 24\/7\]/i.test(cleanName)) {
    isGeoRestricted = /\[geo-blocked\]/i.test(cleanName);
    cleanName = cleanName.replace(/\[(geo-blocked|not 24\/7)\]/gi, '').trim();
  }

  const qualityMatch = cleanName.match(/\((?:\d{3,4}p|4K|HD|SD)\)/i);
  if (qualityMatch) {
    quality = qualityMatch[0].slice(1, -1).toUpperCase();
    cleanName = cleanName.replace(/\((?:\d{3,4}p|4K|HD|SD)\)/gi, '').replace(/\(\)/g, '').trim();
  }

  // Clean trailing empty parentheses or multiple spaces
  cleanName = cleanName.replace(/\s{2,}/g, ' ').trim();

  return { cleanName: cleanName || 'Untitled Channel', quality, isGeoRestricted };
}

function createStableId(name: string, streamUrl: string, index: number): string {
  const slug = name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 32);
  let hash = 0;
  for (let i = 0; i < streamUrl.length; i += 1) {
    hash = (hash * 31 + streamUrl.charCodeAt(i)) >>> 0;
  }
  return `${slug || 'ch'}-${hash.toString(36)}-${index}`;
}

function findExtinfTitleCommaIndex(extinf: string): number {
  let inQuotes = false;
  for (let i = 0; i < extinf.length; i += 1) {
    const ch = extinf[i];
    if (ch === '"' && extinf[i - 1] !== '\\') {
      inQuotes = !inQuotes;
    } else if (ch === ',' && !inQuotes) {
      return i;
    }
  }
  return extinf.lastIndexOf(',');
}

export function parseM3U(rawPlaylist: string): Channel[] {
  if (!rawPlaylist || typeof rawPlaylist !== 'string') {
    return [];
  }

  const lines = rawPlaylist
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  const channels: Channel[] = [];
  const seenUrls = new Set<string>();

  let pendingExtinf: string | null = null;
  let pendingGroupOverride: string | undefined;

  for (const line of lines) {
    if (line.startsWith('#EXTM3U')) {
      continue;
    }

    if (line.startsWith('#EXTINF:')) {
      pendingExtinf = line;
      pendingGroupOverride = undefined;
      continue;
    }

    if (line.startsWith('#EXTGRP:')) {
      pendingGroupOverride = line.slice('#EXTGRP:'.length).trim() || undefined;
      continue;
    }

    if (line.startsWith('#')) {
      continue;
    }

    // Stream URL line
    if (/^https?:\/\//i.test(line)) {
      if (seenUrls.has(line)) {
        pendingExtinf = null;
        pendingGroupOverride = undefined;
        continue;
      }
      seenUrls.add(line);

      const extinf = pendingExtinf ?? '#EXTINF:-1,Unknown Channel';
      const commaIndex = findExtinfTitleCommaIndex(extinf);
      const rawDisplayName = commaIndex >= 0 ? extinf.slice(commaIndex + 1).trim() : '';

      const tvgName = extractAttribute(extinf, 'tvg-name');
      const tvgLogo = extractAttribute(extinf, 'tvg-logo');
      const groupTitle = pendingGroupOverride ?? extractAttribute(extinf, 'group-title');
      const language = extractAttribute(extinf, 'tvg-language');

      const { cleanName, quality, isGeoRestricted } = extractQualityAndCleanName(
        rawDisplayName || tvgName || 'Indian Live Channel'
      );

      const category = normalizeCategory(groupTitle, cleanName);

      channels.push({
        id: createStableId(cleanName, line, channels.length),
        name: cleanName,
        streamUrl: line,
        logoUrl: tvgLogo && /^https?:\/\//i.test(tvgLogo) ? tvgLogo : undefined,
        groupRaw: groupTitle,
        category,
        language,
        quality,
        isLive: true,
        isGeoRestricted,
        epgAvailable: false,
      });

      pendingExtinf = null;
      pendingGroupOverride = undefined;
    }
  }

  return channels;
}

export function getSortedAvailableCategories(
  channels: Channel[]
): Exclude<CategoryName, 'All'>[] {
  const present = new Set(channels.map((ch) => ch.category));
  return CATEGORY_ORDER.filter((cat) => present.has(cat));
}
