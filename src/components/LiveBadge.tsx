interface LiveBadgeProps {
  label?: string;
  compact?: boolean;
}

export function LiveBadge({ label = 'LIVE', compact = false }: LiveBadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full bg-[#101114]/85 border border-[#35D07F]/40 text-[#35D07F] font-display font-bold tracking-wider uppercase whitespace-nowrap shrink-0 ${
        compact ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-0.5 text-[11px]'
      }`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-[#35D07F]" aria-hidden="true" />
      {label}
    </span>
  );
}
