---
name: StreamCast
colors:
  surface: '#121316'
  surface-dim: '#121316'
  surface-bright: '#38393c'
  surface-container-lowest: '#0d0e11'
  surface-container-low: '#1b1b1f'
  surface-container: '#1f1f23'
  surface-container-high: '#292a2d'
  surface-container-highest: '#343538'
  on-surface: '#e3e2e6'
  on-surface-variant: '#c9c4d8'
  inverse-surface: '#e3e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#938ea1'
  outline-variant: '#484555'
  surface-tint: '#c9beff'
  primary: '#c9beff'
  on-primary: '#2f009b'
  primary-container: '#7657ff'
  on-primary-container: '#fffdff'
  inverse-primary: '#5e3be7'
  secondary: '#4ad7f6'
  on-secondary: '#003640'
  secondary-container: '#00b5d4'
  on-secondary-container: '#00424e'
  tertiary: '#4ae08d'
  on-tertiary: '#00391d'
  tertiary-container: '#00874d'
  on-tertiary-container: '#fbfff8'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e6deff'
  primary-fixed-dim: '#c9beff'
  on-primary-fixed: '#1b0063'
  on-primary-fixed-variant: '#4612cf'
  secondary-fixed: '#acedff'
  secondary-fixed-dim: '#4ad7f6'
  on-secondary-fixed: '#001f26'
  on-secondary-fixed-variant: '#004e5c'
  tertiary-fixed: '#6bfda7'
  tertiary-fixed-dim: '#4ae08d'
  on-tertiary-fixed: '#00210f'
  on-tertiary-fixed-variant: '#00522c'
  background: '#121316'
  on-background: '#e3e2e6'
  surface-variant: '#343538'
typography:
  display-lg:
    fontFamily: Space Grotesk
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Space Grotesk
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Space Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Space Grotesk
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  margin-mobile: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system defines an immersive, premium IPTV streaming player and Smart TV controller tailored specifically for high-end modern mobile hardware such as the Samsung Galaxy S24. The brand merges deep cinematic theater ambiance with responsive hardware control ergonomics. It is engineered for media enthusiasts, cord-cutters, and smart-home power users who demand zero-latency visual feedback, effortless one-handed thumb reachability, and an uncluttered screen-first layout.

The visual direction combines Modern Android Material 3 foundations with Samsung One UI ergonomic principles, infused with a dark, high-performance entertainment aesthetic. It incorporates selective glassmorphic layers (specular surface tints and backdrop blur) to emphasize live overlay controls, high-contrast luminous accents to guide interactive targets, and soft pill shapes that prioritize comfortable thumb interaction.

## Colors

The palette revolves around deep pitch-blacks and charcoal tones that take full advantage of Dynamic AMOLED panels, minimizing power consumption and letting streaming artwork dominate the viewport.

- **Primary Accent (`#7657FF`):** Electric Violet serves as the key signature focus color, used for primary actions, active remote controls, focused IPTV tiles, and highlighted playback states.
- **Secondary Accent (`#35C9E8`):** Bright Cyan provides complementary high-energy feedback for secondary highlights, channel category badges, connection states, and scrub-bar indicators.
- **Tertiary Accent (`#35D07F`):** Signal Green strictly handles live broadcast markers, active stream health indicators, and connected Smart TV hardware states.
- **Surface Scale:**
  - Base Canvas (`#101114`): Deep background tone for full-bleed immersion.
  - Surface Secondary (`#1B1D23`): Shelves, side sheets, and modal bottom drawers.
  - Card & Container Surface (`#242731`): Media cards, remote button panels, and channel list items.
  - Borders & Outlines (`#2E323E`): Subtle structural dividers with high legibility without visual noise.
- **Typography & Icon Fill:**
  - Primary Text (`#FFFFFF`): Uncompromised contrast for titles, times, and channel numbers.
  - Secondary Text (`#A7A9B2`): EPG metadata, stream codec info, and passive track details.

## Typography

The typographic hierarchy utilizes two complementary typefaces to balance futuristic technical precision with comfortable reading clarity:

1. **Display & Headings (Space Grotesk):** Conveys the technical capability of modern streaming protocols, hardware remotes, and channel numbers. Its geometric construction gives headers, remote dial values, and channel titles a distinct edge.
2. **Body & Interface Details (Plus Jakarta Sans):** Delivers clean readability for compact EPG (Electronic Program Guide) grids, program descriptions, and configuration panels.
3. **Data & Metadata Labels (Space Grotesk):** Monospaced-leaning proportions ensure audio codecs, stream resolutions (4K, HDR, Dolby Vision), and timecodes remain perfectly aligned across dynamic updates.

## Layout & Spacing

The layout model is anchored in Samsung One UI ergonomic principles: the upper third of the canvas serves primarily as the viewing/visual inspection zone, while the bottom two-thirds serve as the interaction zone for thumb-friendly reach on tall flagship displays.

- **Grid Architecture:** 4-column layout on compact mobile viewports, transitioning to 8 columns on foldables and tablets, and 12 columns on large landscape surfaces.
- **Rhythm & Safe Areas:** Employs an 8-point base spacing rhythm. Bottom navigation and gesture handles strictly respect hardware navigation bars with an added `space-md` clearance.
- **Media Rails & Grids:** Content channels and EPG rows utilize horizontal snapping carousels with `space-sm` item gaps and `margin` edge offsets, preventing visual clipping on rounded device corners.

## Elevation & Depth

Visual hierarchy does not rely on traditional dropped shadows, which wash out against OLED dark backgrounds. Instead, this system applies tonal surface containment layered with subtle colored luminescence and glassmorphism.

- **Level 0 (Canvas):** `#101114` pure backdrop.
- **Level 1 (Structural Shelves & Groupings):** `#1B1D23` with a continuous 1px `#2E323E` structural border.
- **Level 2 (Interactive Cards & Remote Modules):** `#242731` paired with a directional hairline gradient (top-to-bottom white specular fade at 8% opacity).
- **Level 3 (Floating HUD & Player Overlays):** 75% opacity `#1B1D23` combined with a `20px` backdrop blur filter and a 1px border tinted with `rgba(255, 255, 255, 0.08)`.
- **Active Focus Glow:** When navigating virtual remote buttons or highlighted stream cards, elements receive an ambient diffused glow: `0px 4px 20px rgba(118, 87, 255, 0.35)`.

## Shapes

The interface adopts a cohesive curvature scale that matches modern Android hardware shells. Roundedness level `2` sets the baseline corner radius to `8px` (`0.5rem`), scaling smoothly to `16px` (`rounded-lg`) for media content tiles and modal sheets, and `24px` (`rounded-xl`) for floating remote control housings. 

All interactive action chips, live badges, and pill controls use full circular pill geometry (`9999px`) to maintain tactile affordance and clear contrast against rectangular video streams.

## Components

### Buttons & Smart TV Remote D-Pad
- **Primary Buttons:** Filled with `#7657FF`, white label (`label-lg`), with a subtle inner top specular highlight. Minimum touch target is 48px height.
- **Secondary Buttons:** Outlined with 1px `#2E323E` on `#1B1D23` background; text in `#FFFFFF`.
- **D-Pad Controller:** An ergonomic multi-directional disc styled in `#242731`, framed by `#2E323E`, with an active center "OK" button accented in `#7657FF`. Haptic feedback accompanies every boundary push.

### Chips & Status Indicators
- **Category Filter Chips:** Pill-shaped (`rounded-full`), `#1B1D23` surface, `#2E323E` border, `#A7A9B2` typography. When selected, the background turns to `#7657FF` with white text.
- **Live Stream Pill:** Ultra-compact pill featuring a `#35D07F` pulsating dot, accompanied by the text "LIVE" in `label-sm` font.

### Cards (Channels & Media Streams)
- Constructed with `#242731` card surfaces and `rounded-lg` (16px) corners.
- Video preview containers maintain a 16:9 aspect ratio with a subtle bottom-to-top gradient (`#101114` at 90% opacity to transparent) for overlaid channel logos and program titles.
- Focus border transitions to `#7657FF` on tap or navigation focus.

### Input Fields & Search Bars
- Search fields use `#1B1D23` with a 1px `#2E323E` perimeter, inset search icon in `#A7A9B2`, and active focus border switching to `#35C9E8`.
- Height set to 48px with `space-md` internal horizontal padding and `body-md` input text.

### Bottom Navigation Bar
- Frosted glassmorphic dock (`#1B1D23` at 85% opacity with `24px` backdrop blur).
- Features 4 primary destinations: Live TV, Movies & Series, TV Remote, and Settings.
- Active destination shows an illuminated pill indicator around the icon tinted in `#7657FF` at 20% opacity, with icon glyph in `#7657FF`.

### EPG (Program Guide) Matrix Row
- Channel column fixed on the left (`#1B1D23`), with program time slots flowing horizontally on a `#242731` background.
- Current timeline marker rendered as a crisp 2px vertical line in `#35C9E8`.