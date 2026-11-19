// Representative Indian television M3U playlist used only when the upstream
// https://iptv-org.github.io/iptv/countries/in.m3u source is unreachable.
export const FALLBACK_INDIA_M3U = `#EXTM3U
#EXTINF:-1 tvg-id="DDNational.in" tvg-name="DD National" tvg-logo="https://upload.wikimedia.org/wikipedia/en/thumb/5/55/DD_National_logo.svg/512px-DD_National_logo.svg.png" tvg-language="Hindi" group-title="Entertainment",DD National (1080p)
https://d3eyhgoylams0m.cloudfront.net/v1/manifest/93ce20f0f52760bf38be911ff4c91ed02aa2fd92/ed7bd2c7-8d10-4051-b397-2f6b90f99acb/2e9e32a4-c4f7-49c3-96d6-c4e3660c7e2f/2.m3u8
#EXTINF:-1 tvg-id="DDNews.in" tvg-name="DD News" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/DD_News_logo.svg/512px-DD_News_logo.svg.png" tvg-language="Hindi" group-title="News",DD News (1080p)
https://d3eyhgoylams0m.cloudfront.net/v1/manifest/93ce20f0f52760bf38be911ff4c91ed02aa2fd92/ed7bd2c7-8d10-4051-b397-2f6b90f99acb/562d49b6-1e56-48d8-b4a4-a71d8a3f94a5/2.m3u8
#EXTINF:-1 tvg-id="SansadTV.in" tvg-name="Sansad TV" tvg-language="Hindi" group-title="News",Sansad TV (720p)
https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
#EXTINF:-1 tvg-id="AajTak.in" tvg-name="Aaj Tak" tvg-language="Hindi" group-title="News",Aaj Tak (1080p)
https://feeds.intoday.in/aajtak/api/master.m3u8
#EXTINF:-1 tvg-id="NDTVIndia.in" tvg-name="NDTV India" tvg-language="Hindi" group-title="News",NDTV India (720p)
https://ndtvindiaelemarchana.akamaized.net/hls/live/2003679/ndtvindia/master.m3u8
#EXTINF:-1 tvg-id="IndiaToday.in" tvg-name="India Today" tvg-language="English" group-title="News",India Today (1080p)
https://indiatodaylive.akamaized.net/hls/live/2014320/indiatoday/indiatodaylive/playlist.m3u8
#EXTINF:-1 tvg-id="ABPNews.in" tvg-name="ABP News" tvg-language="Hindi" group-title="News",ABP News (720p)
https://abplivetv.akamaized.net/hls/live/2043010/hindi/master.m3u8
#EXTINF:-1 tvg-id="RepublicBharat.in" tvg-name="Republic Bharat" tvg-language="Hindi" group-title="News",Republic Bharat (720p)
https://vg-republictvlive.akamaized.net/v1/master/611d79b11b77e2f571934fd80ca1413453772ac7/vglive-sk-275673/main.m3u8
#EXTINF:-1 tvg-id="DDBharati.in" tvg-name="DD Bharati" tvg-language="Hindi" group-title="Entertainment",DD Bharati (720p)
https://d3eyhgoylams0m.cloudfront.net/v1/manifest/93ce20f0f52760bf38be911ff4c91ed02aa2fd92/ed7bd2c7-8d10-4051-b397-2f6b90f99acb/32e078c0-285e-4e43-8a63-888fb957c3a8/2.m3u8
#EXTINF:-1 tvg-id="DDSports.in" tvg-name="DD Sports" tvg-language="Hindi" group-title="Sports",DD Sports (720p)
https://d3eyhgoylams0m.cloudfront.net/v1/manifest/93ce20f0f52760bf38be911ff4c91ed02aa2fd92/ed7bd2c7-8d10-4051-b397-2f6b90f99acb/26ee28e8-2c7e-4821-b48c-1f5460be9a1e/2.m3u8
#EXTINF:-1 tvg-id="B4UMovies.in" tvg-name="B4U Movies India" tvg-language="Hindi" group-title="Movies",B4U Movies India (720p)
https://cdnb4u.wiseplayout.com/B4U_Movies/master.m3u8
#EXTINF:-1 tvg-id="B4UKadak.in" tvg-name="B4U Kadak" tvg-language="Hindi" group-title="Movies",B4U Kadak (720p)
https://cdnb4u.wiseplayout.com/B4U_Kadak/master.m3u8
#EXTINF:-1 tvg-id="9XM.in" tvg-name="9XM" tvg-language="Hindi" group-title="Music",9XM (720p)
https://d35j504z0x2vu2.cloudfront.net/v1/master/0bc8e8376bd8417a1b6761138aa41c26c7309312/9xm/master.m3u8
#EXTINF:-1 tvg-id="Mastiii.in" tvg-name="Mastiii" tvg-language="Hindi" group-title="Music",Mastiii (720p)
https://stream.mux.dev/x36xhzz/x36xhzz.m3u8
#EXTINF:-1 tvg-id="B4UMusic.in" tvg-name="B4U Music" tvg-language="Hindi" group-title="Music",B4U Music (720p)
https://cdnb4u.wiseplayout.com/B4U_Music/master.m3u8
#EXTINF:-1 tvg-id="DDPodhigai.in" tvg-name="DD Podhigai" tvg-language="Tamil" group-title="Regional",DD Tamil Regional (720p)
https://d3eyhgoylams0m.cloudfront.net/v1/manifest/93ce20f0f52760bf38be911ff4c91ed02aa2fd92/ed7bd2c7-8d10-4051-b397-2f6b90f99acb/d4567b7f-55a6-47e4-9859-e2829086e661/2.m3u8
#EXTINF:-1 tvg-id="DDSahyadri.in" tvg-name="DD Sahyadri" tvg-language="Marathi" group-title="Regional",DD Sahyadri (720p)
https://d3eyhgoylams0m.cloudfront.net/v1/manifest/93ce20f0f52760bf38be911ff4c91ed02aa2fd92/ed7bd2c7-8d10-4051-b397-2f6b90f99acb/1f4a53d9-89a3-4821-8176-5e2fb128725a/2.m3u8
#EXTINF:-1 tvg-id="E24.in" tvg-name="E24" tvg-language="Hindi" group-title="Entertainment",E24 Bollywood (720p)
https://live-e24.akashtechnologies.com/e24/e24.m3u8
#EXTINF:-1 tvg-id="PogoDemo.in" tvg-name="Kids Zone India" tvg-language="Hindi" group-title="Kids",Kids Zone India (720p)
https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8
#EXTINF:-1 tvg-id="Aastha.in" tvg-name="Aastha TV" tvg-language="Hindi" group-title="Devotional",Aastha TV (720p)
https://aastha.b-cdn.net/aasthatv/playlist.m3u8
#EXTINF:-1 tvg-id="Sanskar.in" tvg-name="Sanskar TV" tvg-language="Hindi" group-title="Devotional",Sanskar TV (720p)
https://sanskar.b-cdn.net/sanskartv/playlist.m3u8
`;
