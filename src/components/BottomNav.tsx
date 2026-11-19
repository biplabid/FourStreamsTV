import { Cast, Heart, Home, Radio, Settings, Smartphone, Tv } from 'lucide-react';
import type { PrimaryTab, TvDevice } from '../types/iptv';

interface BottomNavProps {
  activeTab: PrimaryTab;
  onSelectTab: (tab: PrimaryTab) => void;
  connectedTv: TvDevice | null;
  favoritesCount: number;
  onOpenTvDiscovery: () => void;
}

const NAV_ITEMS: { id: PrimaryTab; label: string; icon: typeof Home }[] = [
  { id: 'home', label: 'Home', icon: Home },
  { id: 'live', label: 'Live TV', icon: Radio },
  { id: 'favorites', label: 'Favorites', icon: Heart },
  { id: 'remote', label: 'Remote', icon: Smartphone },
  { id: 'settings', label: 'Settings', icon: Settings },
];

export function NavigationShell({
  activeTab,
  onSelectTab,
  connectedTv,
  favoritesCount,
  onOpenTvDiscovery,
}: BottomNavProps) {
  return (
    <>
      {/* Desktop Left Navigation Rail */}
      <aside
        aria-label="Primary Sidebar Navigation"
        className="hidden lg:flex lg:flex-col lg:w-64 lg:shrink-0 lg:sticky lg:top-0 lg:h-screen bg-[#1B1D23] border-r border-[#2E323E] p-5 justify-between"
      >
        <div className="space-y-6">
          <div className="flex items-center gap-3 px-2">
            <div className="w-9 h-9 rounded-xl bg-[#7657FF] flex items-center justify-center shadow-[0_4px_20px_rgba(118,87,255,0.35)] shrink-0">
              <Tv className="w-5 h-5 text-white" aria-hidden="true" />
            </div>
            <span className="font-display font-bold text-xl tracking-tight text-white whitespace-nowrap">
              StreamCast
            </span>
          </div>

          <nav className="space-y-1.5" aria-label="Main Destinations">
            {NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => onSelectTab(item.id)}
                  aria-current={isActive ? 'page' : undefined}
                  className={`w-full min-h-[46px] px-3.5 py-2.5 rounded-xl flex items-center justify-between text-sm font-medium transition-colors whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] ${
                    isActive
                      ? 'bg-[#7657FF]/20 text-white border border-[#7657FF]/40'
                      : 'text-[#A7A9B2] hover:text-white hover:bg-[#242731] border border-transparent'
                  }`}
                >
                  <span className="flex items-center gap-3">
                    <Icon
                      className={`w-5 h-5 shrink-0 ${isActive ? 'text-[#7657FF]' : ''}`}
                      aria-hidden="true"
                    />
                    <span>{item.label}</span>
                  </span>
                  {item.id === 'favorites' && favoritesCount > 0 && (
                    <span className="text-xs font-mono tabular-nums text-[#A7A9B2]">
                      {favoritesCount}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>

        <div className="space-y-3 pt-4 border-t border-[#2E323E]">
          <button
            type="button"
            onClick={onOpenTvDiscovery}
            className={`w-full p-3.5 rounded-2xl border text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] ${
              connectedTv
                ? 'bg-[#35D07F]/10 border-[#35D07F]/35 hover:bg-[#35D07F]/15'
                : 'bg-[#242731] border-[#2E323E] hover:border-[#7657FF]/50'
            }`}
          >
            <div className="flex items-center justify-between gap-2 mb-1">
              <span className="text-xs font-display font-semibold uppercase tracking-wider text-[#A7A9B2]">
                {connectedTv ? 'TV Connected' : 'Smart TV'}
              </span>
              <Cast
                className={`w-4 h-4 ${connectedTv ? 'text-[#35D07F]' : 'text-[#35C9E8]'}`}
                aria-hidden="true"
              />
            </div>
            <p className="text-sm font-semibold text-white truncate">
              {connectedTv ? connectedTv.name : 'Connect to TV'}
            </p>
            <p className="text-xs text-[#A7A9B2] truncate mt-0.5">
              {connectedTv ? connectedTv.protocol : 'Scan Wi-Fi for Android / Google TV'}
            </p>
          </button>
        </div>
      </aside>

      {/* Mobile Frosted Glassmorphic Bottom Navigation Bar */}
      <nav
        aria-label="Bottom Navigation"
        className="lg:hidden fixed bottom-0 left-0 right-0 z-40 h-[60px] bg-[#1B1D23]/90 backdrop-blur-xl border-t border-[#2E323E] grid grid-cols-5 items-center px-1"
      >
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onSelectTab(item.id)}
              aria-current={isActive ? 'page' : undefined}
              className="flex flex-col items-center justify-center min-h-[48px] py-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-xl"
            >
              <span
                className={`px-3 py-1 rounded-full transition-colors ${
                  isActive ? 'bg-[#7657FF]/20 text-[#7657FF]' : 'text-[#A7A9B2]'
                }`}
              >
                <Icon className="w-5 h-5" aria-hidden="true" />
              </span>
              <span
                className={`text-[10px] font-medium tracking-tight mt-0.5 whitespace-nowrap ${
                  isActive ? 'text-white font-semibold' : 'text-[#A7A9B2]'
                }`}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </nav>
    </>
  );
}
