import test from 'node:test';
import assert from 'node:assert/strict';
import { parseM3U } from './m3uParser';
import {
  filterChannels,
  getAdjacentChannel,
  getPopularChannels,
  getRelatedChannels,
} from './channelFilter';

const SAMPLE_M3U = `#EXTM3U
#EXTINF:-1 group-title="News" tvg-language="Hindi",Aaj Tak
https://example.com/aajtak.m3u8
#EXTINF:-1 group-title="Entertainment" tvg-language="Hindi",DD National
https://example.com/ddnational.m3u8
#EXTINF:-1 group-title="News" tvg-language="Hindi",DD News
https://example.com/ddnews.m3u8
#EXTINF:-1 group-title="Music" tvg-language="Hindi",9XM
https://example.com/9xm.m3u8
`;

test('filterChannels filters by category and search query case-insensitively', () => {
  const channels = parseM3U(SAMPLE_M3U);

  assert.equal(filterChannels(channels, 'All', '').length, 4);
  assert.equal(filterChannels(channels, 'News', '').length, 2);
  assert.equal(filterChannels(channels, 'All', 'dd').length, 2);
  assert.equal(filterChannels(channels, 'News', 'dd').length, 1);
  assert.equal(filterChannels(channels, 'Sports', '').length, 0);
});

test('getPopularChannels prioritizes representative Indian public and news broadcasters', () => {
  const channels = parseM3U(SAMPLE_M3U);
  const popular = getPopularChannels(channels, 4);
  assert.equal(popular[0].name, 'DD National');
  assert.equal(popular[1].name, 'DD News');
  assert.equal(popular[2].name, 'Aaj Tak');
});

test('getRelatedChannels prioritizes same-category channels and excludes current channel', () => {
  const channels = parseM3U(SAMPLE_M3U);
  const aajTak = channels[0];
  const related = getRelatedChannels(channels, aajTak, 3);
  assert.equal(related.some((ch) => ch.id === aajTak.id), false);
  assert.equal(related[0].name, 'DD News');
});

test('getAdjacentChannel wraps around circularly for next and prev', () => {
  const channels = parseM3U(SAMPLE_M3U);
  const first = channels[0];
  const last = channels[channels.length - 1];

  assert.equal(getAdjacentChannel(channels, first, 'prev')?.id, last.id);
  assert.equal(getAdjacentChannel(channels, last, 'next')?.id, first.id);
});
