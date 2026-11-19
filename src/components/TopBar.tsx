import { Cast, Search, Tv } from 'lucide-react';
import type { TvDevice } from '../types/iptv';

interface TopBarProps {
  connectedTv: TvDevice | null;
  onOpenTvDiscovery: () => void;
  onOpenSearch: () => void;
  onGoHome: () => void;
}

export function TopBar({
  connectedTv,
  onOpenTvDiscovery,
  onOpenSearch,
  onGoHome,
}: TopBarProps) {
  return (
    <header className="lg:hidden sticky top-0 z-30 h-[52px] px-4 bg-[#101114]/90 backdrop-blur-xl border-b border-[#2E323E] flex items-center justify-between gap-3">
      <button
        type="button"
        onClick={onGoHome}
        className="flex items-center gap-2.5 min-h-[44px] text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-lg"
        aria-label="StreamCast Home"
      >
        <div className="w-8 h-8 rounded-lg bg-[#7657FF] flex items-center justify-center shadow-[0_4px_16px_rgba(118,87,255,0.35)] shrink-0">
          <Tv className="w-4 h-4 text-white" aria-hidden="true" />
        </div>
        <span className="font-display font-bold text-lg tracking-tight text-white whitespace-nowrap shrink-0">
          StreamCast
        </span>
      </button>

      <div className="flex items-center gap-1.5 shrink-0">
        <button
          type="button"
          onClick={onOpenTvDiscovery}
          className={`min-h-[38px] px-3 rounded-full border text-xs font-medium flex items-center gap-1.5 transition-colors whitespace-nowrap shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] ${
            connectedTv
              ? 'bg-[#35D07F]/15 border-[#35D07F]/40 text-[#35D07F]'
              : 'bg-[#1B1D23] border-[#2E323E] text-[#A7A9B2] hover:text-white'
          }`}
          aria-label={
            connectedTv
              ? `Connected to ${connectedTv.name}. Manage TV connection`
              : 'Connect to TV'
          }
        >
          <Cast className="w-3.5 h-3.5 shrink-0" aria-hidden="true" />
          <span className="max-w-[110px] truncate">
            {connectedTv ? connectedTv.name : 'Connect TV'}
          </span>
        </button>

        <button
          type="button"
          onClick={onOpenSearch}
          className="w-11 h-11 rounded-full flex items-center justify-center text-[#A7A9B2] hover:text-white hover:bg-[#1B1D23] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF]"
          aria-label="Search channels"
        >
          <Search className="w-5 h-5" aria-hidden="true" />
        </button>
      </div>
    </header>
  );
}
