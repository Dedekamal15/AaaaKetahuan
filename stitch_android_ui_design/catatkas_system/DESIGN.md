---
name: CatatKas System
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#40493d'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#707a6c'
  outline-variant: '#C4C7C5'
  surface-tint: '#1b6d24'
  primary: '#0d631b'
  on-primary: '#ffffff'
  primary-container: '#2e7d32'
  on-primary-container: '#cbffc2'
  inverse-primary: '#88d982'
  secondary: '#48626e'
  on-secondary: '#ffffff'
  secondary-container: '#cbe7f5'
  on-secondary-container: '#4e6874'
  tertiary: '#1f6223'
  on-tertiary: '#ffffff'
  tertiary-container: '#3a7b39'
  on-tertiary-container: '#c8ffbf'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#a3f69c'
  primary-fixed-dim: '#88d982'
  on-primary-fixed: '#002204'
  on-primary-fixed-variant: '#005312'
  secondary-fixed: '#cbe7f5'
  secondary-fixed-dim: '#afcbd8'
  on-secondary-fixed: '#021f29'
  on-secondary-fixed-variant: '#304a55'
  tertiary-fixed: '#acf4a4'
  tertiary-fixed-dim: '#91d78a'
  on-tertiary-fixed: '#002203'
  on-tertiary-fixed-variant: '#0c5216'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
  expense-red: '#D32F2F'
  surface-white: '#FFFFFF'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-mobile: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  gutter: 1rem
  stack-sm: 0.5rem
  stack-md: 1rem
  stack-lg: 1.5rem
---

## Brand & Style

The design system for CatatKas is built upon the **Corporate / Modern** movement, specifically following **Material 3 (M3)** principles. The brand personality is professional, reliable, and strictly utility-focused, aimed at individuals seeking a private and efficient financial tool.

The UI evokes a sense of **trust and clarity** through high legibility and a structured layout. By prioritizing a "local-first" philosophy, the design remains clean and unobstructed by the marketing fluff typical of cloud-based fintech. The aesthetic is defined by white surfaces, subtle shadows for depth, and the strategic use of "Success Green" to signify financial health and task completion.

- **Minimalism:** Heavy focus on whitespace to reduce cognitive load during data entry.
- **Material 3:** Use of standard M3 tonal palettes and component behaviors for instant familiarity on Android.
- **Tactile feedback:** Subtle elevation and clear container boundaries help the user distinguish between different financial categories and transaction types.

## Colors

The palette is anchored by **Success Green** (#2E7D32), reinforcing positive financial habits. **Blue Grey** (#546E7A) provides a professional, grounded secondary tone for navigation and utility elements.

- **Primary:** Used for the main action buttons (FAB), active states, and "Inflow" transactions.
- **Secondary:** Applied to secondary UI elements, filter chips, and inactive navigation icons.
- **Expense Red:** A specialized named color used exclusively for "Outflow" amounts and destructive actions (e.g., delete transaction).
- **Neutral:** A very light grey (#F8F9FA) is used for the background to distinguish it from the pure white (#FFFFFF) used for transaction cards and input surfaces.

## Typography

The typography system uses **Inter** across all levels to ensure maximum legibility and a contemporary, "app-first" feel. The scale follows Material 3 standards but leans into heavier weights for currency displays.

- **Headlines:** Reserved for dashboard totals (Balance) and screen titles. Bold weights (700) are used for primary financial figures.
- **Body:** Used for transaction descriptions and notes. Standard weight (400) ensures readability in long lists.
- **Labels:** Used for category chips, button text, and small metadata (dates). Medium weights (500) provide structural clarity.
- **Mobile Scaling:** Headline Large is reduced on mobile to prevent text wrapping on smaller devices.

## Layout & Spacing

This design system utilizes a **Fluid Grid** approach within Material 3's responsive layout grid. 

- **Mobile (0-599dp):** 4-column grid with 16px (1rem) side margins.
- **Tablet (600-839dp):** 8-column grid with 24px (1.5rem) side margins.
- **Desktop (840dp+):** 12-column grid with a maximum content width of 1200px.

Spacing follows an 8dp (0.5rem) base unit. Transaction cards should be separated by `stack-md` (16px), while internal card padding should utilize `stack-sm` (8px) for labels and `stack-md` for primary content.

## Elevation & Depth

To maintain a clean, professional aesthetic, this design system uses **Tonal Layers** combined with **Ambient Shadows**.

- **Level 0 (Surface):** The neutral background (#F8F9FA) sits at the lowest level.
- **Level 1 (Cards):** Transaction cards and the input form use a pure white surface (#FFFFFF) with a soft, 4dp blur shadow (alpha 0.08) to separate them from the background.
- **Level 2 (Navigation):** The bottom navigation bar uses a tonal surface (Primary at 5% opacity) to provide a persistent anchor.
- **Level 3 (Modals/Pickers):** Date pickers and category selectors use a distinct shadow (8dp blur) to pull focus.

Avoid bold borders; use `outline-variant` (#C4C7C5) sparingly for divider lines in long transaction histories.

## Shapes

The design system adopts a **Rounded** (Level 2) shape language to balance professional structure with modern softness.

- **Small Components (Chips):** 8px (0.5rem) radius for category selection chips.
- **Medium Components (Cards/Inputs):** 16px (1rem) radius for transaction cards and text fields.
- **Large Components (Modals):** 24px (1.5rem) radius for bottom sheets and the main input container.
- **FAB:** The floating action button should remain a rounded square (16px) or a full circle to indicate its primary importance.

## Components

### Transaction Cards
White surfaces with 16px corner radius. Left-aligned `body-lg` for the item name, right-aligned bold `body-lg` for the amount. Use "Success Green" for inflows and "Expense Red" for outflows.

### Input Fields
Filled text field style with a 16px top corner radius and a subtle bottom indicator line. Labels should use `label-md` in the Secondary color. Autocomplete suggestions appear in a Level 2 elevation menu directly below the field.

### Category Chips
Filterable chips using 8px radius. Active state uses a Primary green container with white text. Inactive state uses a neutral grey container with Secondary text.

### Summary Cards (Dashboard)
High-contrast containers at the top of the dashboard. Total Balance uses `headline-lg`. Use an 8px vertical stack for "Income" and "Outcome" sub-metrics.

### Lists
Transaction lists in the "Riwayat" screen should use `stack-md` spacing between cards. Each item supports a swipe-to-delete gesture, revealing an "Expense Red" background with a trash icon.