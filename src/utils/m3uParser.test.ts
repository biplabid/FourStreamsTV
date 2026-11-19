import test from 'node:test';
import assert from 'node:assert/strict';
import { getSortedAvailableCategories, normalizeCategory, parseM3U } from './m3uParser';

test('parseM3U extracts channel attributes, logos, language, quality, and deduplicates URLs', () => {
  const sample = `#EXTM3U
#EXTINF:-1 tvg-id="DDNews.in" tvg-name="DD News" tvg-logo="https://example.com/ddnews.png" tvg-language="Hindi" group-title="News",DD News (1080p) [Not 24/7]
https://stream.example.com/ddnews.m3u8
#EXTINF:-1 tvg-name="Duplicate" group-title="News",Duplicate
https://stream.example.com/ddnews.m3u8
#EXTINF:-1 tvg-name="9XM" group-title="Music",9XM (720p)
https://stream.example.com/9xm.m3u8
#EXTINF:-1,No Metadata Channel
https://stream.example.com/unknown.m3u8
`;

  const channels = parseM3U(sample);
  assert.equal(channels.length, 3);

  assert.equal(channels[0].name, 'DD News');
  assert.equal(channels[0].quality, '1080P');
  assert.equal(channels[0].logoUrl, 'https://example.com/ddnews.png');
  assert.equal(channels[0].language, 'Hindi');
  assert.equal(channels[0].category, 'News');
  assert.equal(channels[0].epgAvailable, false);

  assert.equal(channels[1].name, '9XM');
  assert.equal(channels[1].category, 'Music');

  assert.equal(channels[2].name, 'No Metadata Channel');
  assert.equal(channels[2].logoUrl, undefined);
  assert.equal(channels[2].category, 'Others');
});

test('parseM3U handles #EXTGRP directives and invalid/empty input safely', () => {
  assert.deepEqual(parseM3U(''), []);

  const extgrpSample = `#EXTM3U
#EXTINF:-1,DD Sports
#EXTGRP:Sports
https://stream.example.com/sports.m3u8
#EXTINF:-1 tvg-id="4TVNews.in" http-user-agent="Mozilla/5.0 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36" group-title="News",4TV News
#EXTVLCOPT:http-user-agent=Mozilla/5.0 (KHTML, like Gecko)
https://stream.example.com/4tv.m3u8
`;
  const parsed = parseM3U(extgrpSample);
  assert.equal(parsed.length, 2);
  assert.equal(parsed[0].category, 'Sports');
  assert.equal(parsed[1].name, '4TV News');
  assert.equal(parsed[1].category, 'News');
});

test('normalizeCategory maps Indian channel names and groups accurately', () => {
  assert.equal(normalizeCategory('News', 'Aaj Tak'), 'News');
  assert.equal(normalizeCategory(undefined, 'DD National'), 'Entertainment');
  assert.equal(normalizeCategory('Cinema', 'Star Gold'), 'Movies');
  assert.equal(normalizeCategory('Devotional', 'Aastha TV'), 'Devotional');
  assert.equal(normalizeCategory('Tamil', 'Sun TV'), 'Regional');
});

test('getSortedAvailableCategories returns only categories present in playlist in canonical order', () => {
  const sample = `#EXTM3U
#EXTINF:-1 group-title="Music",9XM
https://a.example.com/1.m3u8
#EXTINF:-1 group-title="News",DD News
https://a.example.com/2.m3u8
`;
  const channels = parseM3U(sample);
  assert.deepEqual(getSortedAvailableCategories(channels), ['News', 'Music']);
});
