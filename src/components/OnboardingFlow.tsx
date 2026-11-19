import { useState } from 'react';
import { ArrowRight, Cast, Radio, Tv } from 'lucide-react';

interface OnboardingFlowProps {
  onComplete: (action: 'home' | 'find-tv') => void;
}

const STEPS = [
  {
    step: 1,
    title: 'Your TV. Your Way.',
    subtitle:
      'Explore live Indian television channels and enjoy them on your phone or TV.',
    cta: 'Get Started',
    icon: Tv,
    accent: 'from-[#7657FF]/30 via-[#1B1D23] to-[#101114]',
    accentColor: 'text-[#7657FF]',
  },
  {
    step: 2,
    title: 'Thousands of Channels. One Place.',
    subtitle:
      'Browse available Indian television channels from a curated open IPTV playlist.',
    cta: 'Continue',
    icon: Radio,
    accent: 'from-[#35C9E8]/25 via-[#1B1D23] to-[#101114]',
    accentColor: 'text-[#35C9E8]',
  },
  {
    step: 3,
    title: 'Big Screen Entertainment.',
    subtitle: 'Connect to a compatible TV and cast your favourite channels.',
    cta: 'Find My TV',
    icon: Cast,
    accent: 'from-[#35D07F]/25 via-[#1B1D23] to-[#101114]',
    accentColor: 'text-[#35D07F]',
  },
];

export function OnboardingFlow({ onComplete }: OnboardingFlowProps) {
  const [stepIndex, setStepIndex] = useState(0);
  const current = STEPS[stepIndex];
  const Icon = current.icon;
  const isLast = stepIndex === STEPS.length - 1;

  const handleNext = () => {
    if (!isLast) {
      setStepIndex((prev) => prev + 1);
    } else {
      onComplete('find-tv');
    }
  };

  return (
    <div
      className="min-h-screen bg-[#101114] text-white flex flex-col items-center justify-center p-6"
      role="dialog"
      aria-modal="true"
      aria-label="StreamCast Onboarding"
    >
      <div className="w-full max-w-md bg-[#1B1D23] border border-[#2E323E] rounded-3xl overflow-hidden shadow-2xl">
        <div className="flex items-center justify-between px-6 pt-5">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-[#7657FF] flex items-center justify-center">
              <Tv className="w-4 h-4 text-white" aria-hidden="true" />
            </div>
            <span className="font-display font-bold text-base tracking-tight">StreamCast</span>
          </div>
          <button
            type="button"
            onClick={() => onComplete('home')}
            className="min-h-[44px] px-3 text-sm font-medium text-[#A7A9B2] hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#7657FF] rounded-lg"
          >
            Skip
          </button>
        </div>

        <div className={`mx-6 mt-4 h-56 rounded-2xl bg-gradient-to-br ${current.accent} border border-[#2E323E] flex flex-col items-center justify-center p-6 text-center`}>
          <div className="w-20 h-20 rounded-2xl bg-[#242731] border border-[#2E323E] flex items-center justify-center shadow-[0_8px_32px_rgba(0,0,0,0.4)] mb-4">
            <Icon className={`w-10 h-10 ${current.accentColor}`} aria-hidden="true" />
          </div>
          <p className="text-xs font-display font-semibold uppercase tracking-widest text-[#A7A9B2]">
            Step {current.step} of {STEPS.length}
          </p>
        </div>

        <div className="p-6 space-y-6">
          <div className="space-y-2.5 text-center">
            <h1 className="font-display text-2xl font-bold tracking-tight text-white text-balance">
              {current.title}
            </h1>
            <p className="text-sm text-[#A7A9B2] leading-relaxed">
              {current.subtitle}
            </p>
          </div>

          <div className="flex items-center justify-center gap-2" aria-label="Onboarding progress">
            {STEPS.map((s, idx) => (
              <button
                key={s.step}
                type="button"
                onClick={() => setStepIndex(idx)}
                aria-label={`Go to step ${s.step}`}
                className={`h-2 rounded-full transition-all ${
                  idx === stepIndex ? 'w-7 bg-[#7657FF]' : 'w-2 bg-[#2E323E] hover:bg-[#484555]'
                }`}
              />
            ))}
          </div>

          <div className="flex flex-col gap-2.5">
            <button
              type="button"
              onClick={handleNext}
              className="w-full min-h-[48px] px-5 py-3 rounded-xl bg-[#7657FF] hover:bg-[#6646F5] text-white font-display font-semibold text-sm flex items-center justify-center gap-2 shadow-[0_4px_20px_rgba(118,87,255,0.35)] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#35C9E8]"
            >
              <span>{current.cta}</span>
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </button>
            {isLast && (
              <button
                type="button"
                onClick={() => onComplete('home')}
                className="w-full min-h-[44px] px-4 py-2.5 rounded-xl bg-[#242731] hover:bg-[#2E323E] border border-[#2E323E] text-sm font-medium text-[#A7A9B2] hover:text-white transition-colors"
              >
                Browse Channels First
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
