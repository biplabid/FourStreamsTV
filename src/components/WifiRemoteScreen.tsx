import { useState } from 'react';
import {
  ArrowLeft,
  Cast,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Grid3X3,
  Home,
  Keyboard,
  Menu,
  Mic,
  Pause,
  Play,
  Power,
  Settings,
  SkipBack,
  SkipForward,
  Square,
  Volume2,
  VolumeX,
  WifiOff,
  X,
} from 'lucide-react';
import type { Channel, RemoteLogEntry, TvDevice } from '../types/iptv';

interface WifiRemoteScreenProps {
  connectedTv: TvDevice | null;
  activeChannel: Channel | null;
  isPlaying: boolean;
  isMuted: boolean;
  remoteLogs: RemoteLogEntry[];
  onConnectTv: () => void;
  onDisconnectTv: () => void;
  onOpenSettings: () => void;
  onSendCommand: (command: string, label: string) => void;
}

export function WifiRemoteScreen({
  connectedTv,
  activeChannel,
  isPlaying,
  isMuted,
  remoteLogs,
  onConnectTv,
  onDisconnectTv,
  onOpenSettings,
  onSendCommand,
}: WifiRemoteScreenProps) {
  const [activeKey, setActiveKey] = useState<string | null>(null);
  const [showKeypad, setShowKeypad] = useState(false);
  const [showKeyboardModal, setShowKeyboardModal] = useState(false);
  const [keyboardText, setKeyboardText] = useState('');

  const triggerCommand = (command: string, label: string) => {
    if (!connectedTv) return;
    setActiveKey(command);
    onSendCommand(command, label);
    setTimeout(() => {
      setActiveKey((prev) => (prev === command ? null : prev));
    }, 180);
  };

  const handleKeyboardSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = keyboardText.trim();
    if (!cleaned) return;
    triggerCommand(`KEYCODE_TEXT:${cleaned}`, `Typed "${cleaned}"`);
    setKeyboardText('');
    setShowKeyboardModal(false);
  };

  if (!connectedTv) {
    return (
      <div className="max-w-lg mx-auto py-8">
        <div className="rounded-3xl bg-[#1B1D23] border border-[#2E323E] p-8 text-center space-y-5">
          <div className="w-16 h-16 rounded-2xl bg-[#242731] border border-[#2E323E] flex items-center justify-center mx-auto">
            <WifiOff className="w-8 h-8 text-[#A7A9B2]" aria-hidden="true" />
          </div>
          <div className="space-y-2">
            <h1 className="font-display text-2xl font-bold text-white">No TV Connected</h1>
            <p className="text-sm text-[#A7A9B2] leading-relaxed">
              Pair with a compatible Android TV, Google TV, or Cast receiver on your Wi-Fi network to enable the virtual remote.
            </p>
          </div>
          <button
            type="button"
            onClick={onConnectTv}
            className="w-full min-h-[48px] px-5 py-3 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white font-display font-semibold text-sm flex items-center justify-center gap-2 shadow-[0_4px_20px_rgba(118,87,255,0.35)] transition-colors"
          >
            <Cast className="w-4 h-4" aria-hidden="true" />
            <span>Find My TV</span>
          </button>
        </div>
      </div>
    );
  }

  const latestFeedback = remoteLogs[0];

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Connected TV Top Header */}
      <div className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-4 flex items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-10 h-10 rounded-xl bg-[#35D07F]/15 border border-[#35D07F]/35 flex items-center justify-center shrink-0">
            <Cast className="w-5 h-5 text-[#35D07F]" aria-hidden="true" />
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="font-display font-bold text-base text-white truncate">
                {connectedTv.name}
              </h1>
              <span className="w-2 h-2 rounded-full bg-[#35D07F] shrink-0" aria-hidden="true" />
            </div>
            <p className="text-xs text-[#A7A9B2] truncate">
              {connectedTv.protocol}
              {activeChannel ? ` · Casting ${activeChannel.name}` : ' · Paired & ready'}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onDisconnectTv}
          className="min-h-[40px] px-3.5 py-2 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-xs font-medium text-[#A7A9B2] hover:text-white whitespace-nowrap shrink-0"
        >
          Disconnect
        </button>
      </div>

      {/* Main Ergonomic Remote Body */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-start">
        <div className="md:col-span-7 rounded-3xl bg-[#1B1D23] border border-[#2E323E] p-6 space-y-6">
          {/* Top Utility Row: Power (capability gated), Voice (capability gated), Settings */}
          <div className="flex items-center justify-between gap-3">
            <button
              type="button"
              disabled={!connectedTv.capabilities.power}
              onClick={() => triggerCommand('KEYCODE_POWER', 'Power toggle')}
              aria-label={
                connectedTv.capabilities.power
                  ? 'TV Power'
                  : 'Power button unsupported by this TV protocol'
              }
              className={`w-12 h-12 rounded-full border flex items-center justify-center transition-all ${
                !connectedTv.capabilities.power
                  ? 'bg-[#242731]/40 border-[#2E323E]/50 text-[#A7A9B2]/35 cursor-not-allowed'
                  : activeKey === 'KEYCODE_POWER'
                  ? 'bg-[#FFB4AB] border-[#FFB4AB] text-[#690005] scale-95'
                  : 'bg-[#242731] border-[#2E323E] text-[#FFB4AB] hover:border-[#FFB4AB]/60'
              }`}
            >
              <Power className="w-5 h-5" aria-hidden="true" />
            </button>

            <div className="text-center min-w-0">
              <p className="text-[11px] font-display font-semibold uppercase tracking-widest text-[#A7A9B2]">
                Wi-Fi Remote
              </p>
              <p className="text-xs text-[#35C9E8] truncate mt-0.5 min-h-[16px]">
                {latestFeedback ? latestFeedback.label : 'Tap any control'}
              </p>
            </div>

            {connectedTv.capabilities.voiceSearch ? (
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_VOICE_ASSIST', 'Voice search triggered')}
                aria-label="Voice search"
                className={`w-12 h-12 rounded-full border flex items-center justify-center transition-all ${
                  activeKey === 'KEYCODE_VOICE_ASSIST'
                    ? 'bg-[#35C9E8] border-[#35C9E8] text-[#003640] scale-95'
                    : 'bg-[#242731] border-[#2E323E] text-[#35C9E8] hover:border-[#35C9E8]/60'
                }`}
              >
                <Mic className="w-5 h-5" aria-hidden="true" />
              </button>
            ) : (
              <button
                type="button"
                onClick={onOpenSettings}
                aria-label="Remote settings"
                className="w-12 h-12 rounded-full bg-[#242731] border border-[#2E323E] text-[#A7A9B2] hover:text-white flex items-center justify-center transition-colors"
              >
                <Settings className="w-5 h-5" aria-hidden="true" />
              </button>
            )}
          </div>

          {/* Centerpiece Circular Directional Pad (D-Pad) */}
          <div className="flex justify-center py-2">
            <div
              className="relative w-60 h-60 rounded-full bg-[#242731] border border-[#2E323E] shadow-[inset_0_1px_0_rgba(255,255,255,0.06)] flex items-center justify-center"
              role="group"
              aria-label="Directional navigation pad"
            >
              {/* Up */}
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_DPAD_UP', 'Navigate Up')}
                aria-label="Navigate Up"
                className={`absolute top-2 left-1/2 -translate-x-1/2 w-16 h-12 rounded-2xl flex items-center justify-center transition-all ${
                  activeKey === 'KEYCODE_DPAD_UP'
                    ? 'bg-[#7657FF]/30 text-white scale-95'
                    : 'text-[#A7A9B2] hover:text-white hover:bg-white/5'
                }`}
              >
                <ChevronUp className="w-7 h-7" aria-hidden="true" />
              </button>

              {/* Down */}
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_DPAD_DOWN', 'Navigate Down')}
                aria-label="Navigate Down"
                className={`absolute bottom-2 left-1/2 -translate-x-1/2 w-16 h-12 rounded-2xl flex items-center justify-center transition-all ${
                  activeKey === 'KEYCODE_DPAD_DOWN'
                    ? 'bg-[#7657FF]/30 text-white scale-95'
                    : 'text-[#A7A9B2] hover:text-white hover:bg-white/5'
                }`}
              >
                <ChevronDown className="w-7 h-7" aria-hidden="true" />
              </button>

              {/* Left */}
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_DPAD_LEFT', 'Navigate Left')}
                aria-label="Navigate Left"
                className={`absolute left-2 top-1/2 -translate-y-1/2 w-12 h-16 rounded-2xl flex items-center justify-center transition-all ${
                  activeKey === 'KEYCODE_DPAD_LEFT'
                    ? 'bg-[#7657FF]/30 text-white scale-95'
                    : 'text-[#A7A9B2] hover:text-white hover:bg-white/5'
                }`}
              >
                <ChevronLeft className="w-7 h-7" aria-hidden="true" />
              </button>

              {/* Right */}
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_DPAD_RIGHT', 'Navigate Right')}
                aria-label="Navigate Right"
                className={`absolute right-2 top-1/2 -translate-y-1/2 w-12 h-16 rounded-2xl flex items-center justify-center transition-all ${
                  activeKey === 'KEYCODE_DPAD_RIGHT'
                    ? 'bg-[#7657FF]/30 text-white scale-95'
                    : 'text-[#A7A9B2] hover:text-white hover:bg-white/5'
                }`}
              >
                <ChevronRight className="w-7 h-7" aria-hidden="true" />
              </button>

              {/* Central OK / Select Button */}
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_DPAD_CENTER', 'Select / OK')}
                aria-label="OK Select"
                className={`w-24 h-24 rounded-full font-display font-bold text-base tracking-wide border transition-all ${
                  activeKey === 'KEYCODE_DPAD_CENTER'
                    ? 'bg-[#6646F5] border-[#C9BEFF] scale-95 shadow-[0_0_24px_rgba(118,87,255,0.65)]'
                    : 'bg-[#7657FF] hover:bg-[#6646F5] border-[#C9BEFF]/30 shadow-[0_4px_20px_rgba(118,87,255,0.4)]'
                } text-white`}
              >
                OK
              </button>
            </div>
          </div>

          {/* Navigation Action Row: Back, Home, Menu */}
          <div className="grid grid-cols-3 gap-3">
            <button
              type="button"
              onClick={() => triggerCommand('KEYCODE_BACK', 'Back')}
              aria-label="Back"
              className={`min-h-[50px] rounded-2xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all ${
                activeKey === 'KEYCODE_BACK'
                  ? 'bg-[#7657FF] border-[#7657FF] text-white scale-95'
                  : 'bg-[#242731] border-[#2E323E] text-white hover:border-[#484555]'
              }`}
            >
              <ArrowLeft className="w-4 h-4" aria-hidden="true" />
              <span>Back</span>
            </button>
            <button
              type="button"
              onClick={() => triggerCommand('KEYCODE_HOME', 'TV Home')}
              aria-label="TV Home"
              className={`min-h-[50px] rounded-2xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all ${
                activeKey === 'KEYCODE_HOME'
                  ? 'bg-[#7657FF] border-[#7657FF] text-white scale-95'
                  : 'bg-[#242731] border-[#2E323E] text-white hover:border-[#484555]'
              }`}
            >
              <Home className="w-4 h-4" aria-hidden="true" />
              <span>Home</span>
            </button>
            <button
              type="button"
              onClick={() => triggerCommand('KEYCODE_MENU', 'Menu')}
              aria-label="Menu"
              className={`min-h-[50px] rounded-2xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all ${
                activeKey === 'KEYCODE_MENU'
                  ? 'bg-[#7657FF] border-[#7657FF] text-white scale-95'
                  : 'bg-[#242731] border-[#2E323E] text-white hover:border-[#484555]'
              }`}
            >
              <Menu className="w-4 h-4" aria-hidden="true" />
              <span>Menu</span>
            </button>
          </div>

          {/* Volume & Channel Rockers + Media Controls */}
          <div className="grid grid-cols-3 gap-3 items-stretch">
            {/* Volume Rocker */}
            <div className="rounded-2xl bg-[#242731] border border-[#2E323E] p-1.5 flex flex-col justify-between gap-1">
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_VOLUME_UP', 'Volume Up')}
                aria-label="Volume Up"
                className="min-h-[44px] rounded-xl hover:bg-[#2E323E] text-white font-display font-bold text-sm flex items-center justify-center transition-colors"
              >
                +
              </button>
              <span className="text-[10px] font-display uppercase tracking-wider text-[#A7A9B2] text-center">
                Vol
              </span>
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_VOLUME_DOWN', 'Volume Down')}
                aria-label="Volume Down"
                className="min-h-[44px] rounded-xl hover:bg-[#2E323E] text-white font-display font-bold text-sm flex items-center justify-center transition-colors"
              >
                −
              </button>
            </div>

            {/* Center Playback & Mute Stack */}
            <div className="flex flex-col justify-between gap-2">
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_MEDIA_PLAY_PAUSE', isPlaying ? 'Pause' : 'Play')}
                aria-label={isPlaying ? 'Pause TV playback' : 'Play TV playback'}
                className="min-h-[48px] rounded-2xl bg-[#7657FF] hover:bg-[#6646F5] text-white flex items-center justify-center gap-1.5 text-xs font-semibold transition-colors"
              >
                {isPlaying ? (
                  <Pause className="w-4 h-4" aria-hidden="true" />
                ) : (
                  <Play className="w-4 h-4 fill-current" aria-hidden="true" />
                )}
                <span>{isPlaying ? 'Pause' : 'Play'}</span>
              </button>

              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => triggerCommand('KEYCODE_MEDIA_STOP', 'Stop')}
                  aria-label="Stop playback"
                  className="min-h-[44px] rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-white flex items-center justify-center"
                >
                  <Square className="w-4 h-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  onClick={() => triggerCommand('KEYCODE_VOLUME_MUTE', isMuted ? 'Unmute' : 'Mute')}
                  aria-label={isMuted ? 'Unmute TV' : 'Mute TV'}
                  className={`min-h-[44px] rounded-xl border flex items-center justify-center transition-colors ${
                    isMuted
                      ? 'bg-[#FFB4AB]/15 border-[#FFB4AB]/50 text-[#FFB4AB]'
                      : 'bg-[#242731] hover:bg-[#2E323E] border-[#2E323E] text-white'
                  }`}
                >
                  {isMuted ? (
                    <VolumeX className="w-4 h-4" aria-hidden="true" />
                  ) : (
                    <Volume2 className="w-4 h-4" aria-hidden="true" />
                  )}
                </button>
              </div>
            </div>

            {/* Channel Rocker */}
            <div className="rounded-2xl bg-[#242731] border border-[#2E323E] p-1.5 flex flex-col justify-between gap-1">
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_CHANNEL_UP', 'Next Channel')}
                aria-label="Next channel"
                className="min-h-[44px] rounded-xl hover:bg-[#2E323E] text-white flex items-center justify-center transition-colors"
              >
                <SkipForward className="w-4 h-4" aria-hidden="true" />
              </button>
              <span className="text-[10px] font-display uppercase tracking-wider text-[#A7A9B2] text-center">
                CH
              </span>
              <button
                type="button"
                onClick={() => triggerCommand('KEYCODE_CHANNEL_DOWN', 'Previous Channel')}
                aria-label="Previous channel"
                className="min-h-[44px] rounded-xl hover:bg-[#2E323E] text-white flex items-center justify-center transition-colors"
              >
                <SkipBack className="w-4 h-4" aria-hidden="true" />
              </button>
            </div>
          </div>

          {/* Bottom Extra Inputs: Keyboard, Numeric 0-9, Settings */}
          <div className="grid grid-cols-3 gap-3 pt-1">
            <button
              type="button"
              onClick={() => setShowKeyboardModal(true)}
              className="min-h-[44px] rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-xs font-medium text-white flex items-center justify-center gap-1.5"
            >
              <Keyboard className="w-4 h-4 text-[#35C9E8]" aria-hidden="true" />
              <span>Keyboard</span>
            </button>

            <button
              type="button"
              disabled={!connectedTv.capabilities.numericKeypad}
              onClick={() => setShowKeypad((v) => !v)}
              className={`min-h-[44px] rounded-xl border text-xs font-medium flex items-center justify-center gap-1.5 ${
                !connectedTv.capabilities.numericKeypad
                  ? 'bg-[#242731]/40 border-[#2E323E]/50 text-[#A7A9B2]/40 cursor-not-allowed'
                  : showKeypad
                  ? 'bg-[#7657FF] border-[#7657FF] text-white'
                  : 'bg-[#242731] hover:bg-[#2E323E] border-[#2E323E] text-white'
              }`}
            >
              <Grid3X3 className="w-4 h-4" aria-hidden="true" />
              <span>123 Pad</span>
            </button>

            <button
              type="button"
              onClick={onOpenSettings}
              className="min-h-[44px] rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-xs font-medium text-white flex items-center justify-center gap-1.5"
            >
              <Settings className="w-4 h-4 text-[#A7A9B2]" aria-hidden="true" />
              <span>Settings</span>
            </button>
          </div>
        </div>

        {/* Side Column: Numeric Keypad + Recent Dispatched Commands */}
        <div className="md:col-span-5 space-y-4">
          {showKeypad && connectedTv.capabilities.numericKeypad && (
            <div className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-4 space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="font-display text-sm font-semibold text-white">
                  Channel Keypad
                </h2>
                <button
                  type="button"
                  onClick={() => setShowKeypad(false)}
                  aria-label="Hide numeric keypad"
                  className="text-[#A7A9B2] hover:text-white"
                >
                  <X className="w-4 h-4" aria-hidden="true" />
                </button>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'].map((digit) => (
                  <button
                    key={digit}
                    type="button"
                    onClick={() => triggerCommand(`KEYCODE_${digit}`, `Digit ${digit}`)}
                    className={`min-h-[46px] rounded-xl bg-[#242731] hover:bg-[#7657FF] border border-[#2E323E] font-display font-bold text-base text-white transition-colors ${
                      digit === '0' ? 'col-span-3' : ''
                    }`}
                  >
                    {digit}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-4 space-y-3">
            <h2 className="font-display text-sm font-semibold text-white">
              Receiver Command Activity
            </h2>
            {remoteLogs.length === 0 ? (
              <p className="text-xs text-[#A7A9B2] leading-relaxed">
                Remote buttons dispatch Android TV Remote v2 / Cast receiver key events to{' '}
                <span className="text-white">{connectedTv.name}</span>.
              </p>
            ) : (
              <ul className="space-y-2 max-h-64 overflow-y-auto pr-1">
                {remoteLogs.slice(0, 8).map((entry) => (
                  <li
                    key={entry.id}
                    className="flex items-center justify-between gap-2 text-xs py-1.5 border-b border-[#2E323E] last:border-0"
                  >
                    <span className="text-white font-medium truncate">{entry.label}</span>
                    <span className="text-[#A7A9B2] font-mono tabular-nums shrink-0">
                      {entry.timestamp}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>

      {/* Keyboard Text Input Modal */}
      {showKeyboardModal && (
        <div
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-label="Send text to TV"
          onClick={() => setShowKeyboardModal(false)}
        >
          <form
            onSubmit={handleKeyboardSubmit}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-2xl bg-[#1B1D23] border border-[#2E323E] p-5 space-y-4 shadow-2xl"
          >
            <div className="flex items-center justify-between">
              <h2 className="font-display font-bold text-base text-white">
                Send Text to {connectedTv.name}
              </h2>
              <button
                type="button"
                onClick={() => setShowKeyboardModal(false)}
                aria-label="Close keyboard input"
                className="w-9 h-9 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white"
              >
                <X className="w-4 h-4" aria-hidden="true" />
              </button>
            </div>
            <div>
              <label htmlFor="tv-keyboard-input" className="sr-only">
                Text to send to TV
              </label>
              <input
                id="tv-keyboard-input"
                type="text"
                autoFocus
                value={keyboardText}
                onChange={(e) => setKeyboardText(e.target.value)}
                placeholder="Type search query or channel name…"
                className="w-full h-12 px-4 rounded-xl bg-[#242731] border border-[#2E323E] focus:border-[#35C9E8] focus:outline-none text-sm text-white"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowKeyboardModal(false)}
                className="min-h-[44px] px-4 rounded-xl bg-[#242731] text-sm text-[#A7A9B2] hover:text-white"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="min-h-[44px] px-5 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white text-sm font-display font-semibold"
              >
                Send to TV
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
