import type { TvDevice } from '../types/iptv';

export const DISCOVERABLE_TV_DEVICES: TvDevice[] = [
  {
    id: 'tv-samsung-living-room',
    name: 'Samsung Living Room TV',
    deviceType: 'Samsung Smart TV',
    protocol: 'Google Cast v2',
    ipAddress: '192.168.1.42',
    signalStrength: 'Strong',
    model: 'QN90D Neo QLED 65" (Cast Receiver)',
    capabilities: {
      power: true,
      voiceSearch: false,
      volumeControl: true,
      numericKeypad: true,
      textInput: true,
    },
  },
  {
    id: 'tv-android-bedroom',
    name: 'Android TV – Bedroom',
    deviceType: 'Android TV',
    protocol: 'Android TV Remote v2',
    ipAddress: '192.168.1.67',
    signalStrength: 'Good',
    model: 'Sony BRAVIA XR 55" (Android TV 14)',
    capabilities: {
      power: true,
      voiceSearch: true,
      volumeControl: true,
      numericKeypad: true,
      textInput: true,
    },
  },
  {
    id: 'tv-google-family-room',
    name: 'Google TV – Family Room',
    deviceType: 'Google TV',
    protocol: 'Google TV Cast',
    ipAddress: '192.168.1.104',
    signalStrength: 'Strong',
    model: 'Google TV Streamer 4K',
    capabilities: {
      power: false,
      voiceSearch: true,
      volumeControl: true,
      numericKeypad: false,
      textInput: true,
    },
  },
];
