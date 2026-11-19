import {
  AlertCircle,
  Cast,
  CheckCircle2,
  Loader2,
  PowerOff,
  RefreshCw,
  Smartphone,
  Tv,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';
import type { TvDevice, TvDiscoveryState } from '../types/iptv';

interface DeviceDiscoveryModalProps {
  isOpen: boolean;
  discoveryState: TvDiscoveryState;
  devices: TvDevice[];
  connectedTv: TvDevice | null;
  connectingDeviceId: string | null;
  errorMessage?: string;
  onClose: () => void;
  onRescan: (simulateEmpty?: boolean) => void;
  onConnectDevice: (device: TvDevice, simulateFailure?: boolean) => void;
  onDisconnect: () => void;
  onLaunchRemote: () => void;
}

export function DeviceDiscoveryModal({
  isOpen,
  discoveryState,
  devices,
  connectedTv,
  connectingDeviceId,
  errorMessage,
  onClose,
  onRescan,
  onConnectDevice,
  onDisconnect,
  onLaunchRemote,
}: DeviceDiscoveryModalProps) {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tv-discovery-title"
      onClick={onClose}
    >
      <div
        className="w-full sm:max-w-lg max-h-[90vh] overflow-y-auto bg-[#1B1D23] border border-[#2E323E] rounded-t-3xl sm:rounded-3xl p-5 sm:p-6 space-y-5 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-10 h-1 bg-[#2E323E] rounded-full mx-auto sm:hidden" />

        {/* Header */}
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 id="tv-discovery-title" className="font-display text-xl font-bold text-white">
              Connect to TV
            </h2>
            <p className="text-sm text-[#A7A9B2] mt-0.5">
              Find devices on your Wi-Fi network
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close TV discovery"
            className="w-11 h-11 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white hover:bg-[#242731] transition-colors"
          >
            <X className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        {/* Protocol Transparency Note */}
        <div className="rounded-xl bg-[#242731]/70 border border-[#2E323E] px-3.5 py-2.5 text-xs text-[#A7A9B2] leading-relaxed">
          Discovery scans for receivers supporting <span className="text-white font-medium">Google Cast</span> or <span className="text-white font-medium">Android TV Remote v2</span> pairing on the same Wi-Fi LAN.
        </div>

        {/* Connected State Card */}
        {connectedTv && (
          <div className="rounded-2xl bg-[#35D07F]/10 border border-[#35D07F]/40 p-4 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-11 h-11 rounded-xl bg-[#35D07F]/20 flex items-center justify-center shrink-0">
                  <Tv className="w-5 h-5 text-[#35D07F]" aria-hidden="true" />
                </div>
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-display font-bold text-base text-white truncate">
                      {connectedTv.name}
                    </p>
                    <CheckCircle2 className="w-4 h-4 text-[#35D07F] shrink-0" aria-hidden="true" />
                  </div>
                  <p className="text-xs text-[#35D07F] font-medium">
                    Connected · {connectedTv.protocol}
                  </p>
                  <p className="text-xs text-[#A7A9B2] mt-0.5 truncate">
                    {connectedTv.model} · {connectedTv.ipAddress}
                  </p>
                </div>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-2.5">
              <button
                type="button"
                onClick={onLaunchRemote}
                className="flex-1 min-h-[44px] px-4 py-2.5 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white text-sm font-display font-semibold flex items-center justify-center gap-2 transition-colors"
              >
                <Smartphone className="w-4 h-4" aria-hidden="true" />
                <span>Launch Remote</span>
              </button>
              <button
                type="button"
                onClick={onDisconnect}
                className="min-h-[44px] px-4 py-2.5 rounded-xl bg-[#242731] hover:bg-[#93000A]/40 border border-[#2E323E] text-sm font-medium text-white flex items-center justify-center gap-2 transition-colors"
              >
                <PowerOff className="w-4 h-4 text-[#FFB4AB]" aria-hidden="true" />
                <span>Disconnect</span>
              </button>
            </div>
          </div>
        )}

        {/* State 1: Searching */}
        {discoveryState === 'searching' && (
          <div className="rounded-2xl bg-[#242731] border border-[#2E323E] p-8 flex flex-col items-center text-center">
            <div className="relative w-16 h-16 flex items-center justify-center mb-4">
              <span className="absolute inset-0 rounded-full border-2 border-[#35C9E8]/30 animate-ping" />
              <div className="w-14 h-14 rounded-full bg-[#1B1D23] border border-[#35C9E8]/50 flex items-center justify-center">
                <Wifi className="w-6 h-6 text-[#35C9E8]" aria-hidden="true" />
              </div>
            </div>
            <p className="font-display font-semibold text-base text-white">
              Searching for devices…
            </p>
            <p className="text-xs text-[#A7A9B2] mt-1">
              Listening for mDNS / Cast receivers on your local Wi-Fi network
            </p>
          </div>
        )}

        {/* State 5: Connection Failed Banner */}
        {discoveryState === 'failed' && (
          <div
            role="alert"
            className="rounded-2xl bg-[#93000A]/25 border border-[#FFB4AB]/40 p-4 flex items-start gap-3"
          >
            <AlertCircle className="w-5 h-5 text-[#FFB4AB] shrink-0 mt-0.5" aria-hidden="true" />
            <div className="flex-1">
              <p className="text-sm font-semibold text-white">TV Connection Failed</p>
              <p className="text-xs text-[#FFDBD6]/90 mt-0.5">
                {errorMessage ||
                  'Could not complete pairing handshake. Ensure the TV is powered on and on the same Wi-Fi band.'}
              </p>
            </div>
          </div>
        )}

        {/* State 6: No Devices Found */}
        {discoveryState === 'empty' && (
          <div className="rounded-2xl bg-[#242731] border border-[#2E323E] p-8 flex flex-col items-center text-center">
            <div className="w-14 h-14 rounded-2xl bg-[#1B1D23] border border-[#2E323E] flex items-center justify-center mb-3">
              <WifiOff className="w-6 h-6 text-[#A7A9B2]" aria-hidden="true" />
            </div>
            <h3 className="font-display font-semibold text-base text-white">
              No devices found
            </h3>
            <p className="text-xs text-[#A7A9B2] max-w-xs mt-1 leading-relaxed">
              No compatible Android TV, Google TV, or Cast devices responded on your Wi-Fi network.
            </p>
            <button
              type="button"
              onClick={() => onRescan(false)}
              className="mt-4 min-h-[44px] px-5 py-2.5 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white text-sm font-display font-semibold flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" aria-hidden="true" />
              <span>Scan Again</span>
            </button>
          </div>
        )}

        {/* State 2/3/4: Discovered Devices List */}
        {discoveryState !== 'searching' && discoveryState !== 'empty' && (
          <div className="space-y-2.5">
            <div className="flex items-center justify-between">
              <p className="text-xs font-display font-semibold uppercase tracking-wider text-[#A7A9B2]">
                Available Receivers ({devices.length})
              </p>
              <button
                type="button"
                onClick={() => onRescan(false)}
                className="text-xs font-medium text-[#35C9E8] hover:text-white flex items-center gap-1 min-h-[36px] px-2"
              >
                <RefreshCw className="w-3.5 h-3.5" aria-hidden="true" />
                <span>Rescan</span>
              </button>
            </div>

            {devices.map((device) => {
              const isConnected = connectedTv?.id === device.id;
              const isConnecting = connectingDeviceId === device.id;

              return (
                <div
                  key={device.id}
                  className={`rounded-2xl border p-3.5 flex items-center justify-between gap-3 transition-colors ${
                    isConnected
                      ? 'bg-[#35D07F]/10 border-[#35D07F]/40'
                      : 'bg-[#242731] border-[#2E323E]'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-10 h-10 rounded-xl bg-[#1B1D23] border border-[#2E323E] flex items-center justify-center shrink-0">
                      <Cast
                        className={`w-4 h-4 ${
                          isConnected ? 'text-[#35D07F]' : 'text-[#35C9E8]'
                        }`}
                        aria-hidden="true"
                      />
                    </div>
                    <div className="min-w-0">
                      <p className="font-display font-semibold text-sm text-white truncate">
                        {device.name}
                      </p>
                      <p className="text-xs text-[#A7A9B2] truncate">
                        {device.deviceType} · {device.protocol} · Wi-Fi {device.signalStrength}
                      </p>
                    </div>
                  </div>

                  {isConnected ? (
                    <span className="px-3 py-1.5 rounded-xl bg-[#35D07F]/15 text-[#35D07F] text-xs font-semibold whitespace-nowrap shrink-0">
                      Connected
                    </span>
                  ) : (
                    <button
                      type="button"
                      disabled={Boolean(connectingDeviceId)}
                      onClick={() => onConnectDevice(device)}
                      aria-label={`Connect to ${device.name}`}
                      className="min-h-[40px] px-4 py-2 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] disabled:opacity-60 text-white text-xs font-display font-semibold flex items-center gap-1.5 whitespace-nowrap shrink-0 transition-colors"
                    >
                      {isConnecting ? (
                        <>
                          <Loader2 className="w-3.5 h-3.5 animate-spin" aria-hidden="true" />
                          <span>Connecting…</span>
                        </>
                      ) : (
                        <span>Connect</span>
                      )}
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
