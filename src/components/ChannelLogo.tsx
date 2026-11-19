import { useState } from 'react';
import { Tv } from 'lucide-react';
import type { CategoryName } from '../types/iptv';

interface ChannelLogoProps {
  name: string;
  logoUrl?: string;
  category: Exclude<CategoryName, 'All'>;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const CATEGORY_GRADIENTS: Record<Exclude<CategoryName, 'All'>, string> = {
  News: 'from-[#7657FF]/30 to-[#35C9E8]/20 text-[#C9BEFF]',
  Entertainment: 'from-[#7657FF]/35 to-[#242731] text-[#E6DEFF]',
  Movies: 'from-[#35C9E8]/30 to-[#7657FF]/25 text-[#ACEDFF]',
  Sports: 'from-[#35D07F]/30 to-[#35C9E8]/20 text-[#6BFDA7]',
  Music: 'from-[#7657FF]/30 to-[#35D07F]/20 text-[#C9BEFF]',
  Regional: 'from-[#35C9E8]/25 to-[#242731] text-[#ACEDFF]',
  Kids: 'from-[#35C9E8]/35 to-[#35D07F]/25 text-[#ACEDFF]',
  Devotional: 'from-[#7657FF]/25 to-[#2E323E] text-[#E6DEFF]',
  Others: 'from-[#2E323E] to-[#1B1D23] text-[#A7A9B2]',
};

const SIZE_CLASSES: Record<NonNullable<ChannelLogoProps['size']>, string> = {
  sm: 'w-10 h-10 rounded-lg text-xs',
  md: 'w-12 h-12 rounded-xl text-sm',
  lg: 'w-14 h-14 rounded-xl text-base',
  xl: 'w-16 h-16 rounded-2xl text-lg',
};

function getInitials(name: string): string {
  const words = name
    .replace(/[^a-zA-Z0-9\s]/g, ' ')
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (words.length === 0) return 'TV';
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return `${words[0][0]}${words[1][0]}`.toUpperCase();
}

export function ChannelLogo({
  name,
  logoUrl,
  category,
  size = 'md',
  className = '',
}: ChannelLogoProps) {
  const [hasError, setHasError] = useState(false);
  const showFallback = !logoUrl || hasError;
  const gradientClass = CATEGORY_GRADIENTS[category] ?? CATEGORY_GRADIENTS.Others;

  return (
    <div
      className={`relative shrink-0 overflow-hidden border border-[#2E323E] bg-[#1B1D23] flex items-center justify-center ${SIZE_CLASSES[size]} ${className}`}
    >
      {showFallback ? (
        <div
          className={`w-full h-full bg-gradient-to-br ${gradientClass} flex flex-col items-center justify-center font-display font-bold tracking-tight select-none`}
          aria-hidden="true"
        >
          <span>{getInitials(name)}</span>
          <Tv className="w-2.5 h-2.5 opacity-60 mt-0.5" />
        </div>
      ) : (
        <img
          src={logoUrl}
          alt={`${name} logo`}
          referrerPolicy="no-referrer"
          loading="lazy"
          onError={() => setHasError(true)}
          className="w-full h-full object-contain p-1.5 bg-[#1B1D23]"
        />
      )}
    </div>
  );
}
