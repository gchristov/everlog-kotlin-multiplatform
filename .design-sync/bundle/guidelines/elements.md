# Everlog elements

The app's design system is Jetpack Compose (`ui/design/elements` in the Android repo), so there are no
web components to import. Build each element below from plain markup and the tokens in `styles.css`,
using exactly these colours, fonts, paddings and radii, and call it by its element name in comments
or layer names (e.g. `<!-- AppListGroup -->`) so the design maps 1:1 onto the Compose screen.

Every measurement is in dp; in designs 1px = 1dp.

## AppScreen

The whole screen: an optional `AppBar` at the top, scrolling content, and an optional `AppFooter`
pinned to the bottom. Background `--background-primary`. Content scrolls behind the footer, so leave
bottom padding equal to the footer's height. On tablets the content and footer are capped at
`--content-max-width` and centred; the app bar stays full width.

## AppBar

Row at the top, background `--background-primary`, 64px tall: an optional back arrow (24px icon,
`--content-primary`) on the left, an optional title in `--font-heading`, and optional actions on the
right (icon buttons or `AppTertiaryButton`s). No shadow, no divider.

Optional header under the row (`AppBarHeader`): padding `--space-lg`, items `--space-sm` apart:
- eyebrow (optional): `--font-caption`, `--content-action`, e.g. "Welcome to Everlog"
- title: `--font-title`, `--content-primary`
- body (optional): `--font-body`, `--content-secondary`

The header can scroll away with the content; when the bar also has a title, that title fades in as
the header leaves.

## AppHeroHeader

A large title with a line of body text, at the top of the content (e.g. "Here's your week").
Without an image: fixed height `--hero-header-height`, text at the bottom, title `--font-title`, body
`--font-body` in `--content-secondary`, `--space-sm` between them, no padding of its own (it lines up
with the content's margin). With an image: the image goes edge to edge at `--hero-image-height`, then
the text under it with `--space-lg` padding at the top and sides.

## AppSectionHeader

The label above a group or card: `--font-subtitle`, `--content-secondary`, one line, e.g. "General".
`--space-lg` below it, before the card.

## AppSurface

A card: background `--background-surface`, radius `--radius-surface`, padding `--space-lg` (none
when its content pads itself, like list rows). Text inside is `--content-primary`.

## AppListGroup / AppGroupedList

Settings-style groups. Each group: an optional `AppSectionHeader`, then its rows drawn as one card,
then an optional footer note.
- Rows share `--background-surface`; only the group's outer corners are rounded (`--radius-surface`
  on the first row's top corners and the last row's bottom corners; a single row is fully rounded).
- Rows are joined by a full-width 1px divider in `--background-separator`, not by gaps.
- Footer note: `--font-caption`, `--content-secondary`, `--space-sm` above it.
- `AppGroupedList` is a scrolling list of groups: `--space-lg` padding on every side and `--space-lg`
  between groups, the same as the side margin.

## AppListItem / AppListRow

A row inside a group. `AppListRow` is the padding for any content: `--space-lg` all round, minimum
height `--list-row-min-height`, content centred vertically. `AppListItem` is the standard row:
- optional leading content (icon, image, checkbox), then the text, then optional trailing content
  (value, switch, `AppCheck`), `--space-lg` apart. No chevrons on tappable rows: that's an iOS
  pattern; a row that expands something in place (e.g. Show all) can end in a down arrow.
- text column, `--space-xs` apart: optional header (`--font-caption`, `--content-secondary`), title
  (`--font-body`, `--content-primary`), optional subtitle (`--font-caption`, `--content-secondary`)

## Buttons

All buttons are pills (`--radius-button`), minimum height `--button-min-height`, padding
`--space-sm` vertical and `--space-xl` horizontal, label in `--font-button`. Disabled: 40% opacity.
- `AppButton` (primary): filled `--content-action`, label `--content-on-action`.
- `AppSecondaryButton`: transparent with a 1px `--content-action` border, label `--content-action`.
- `AppTertiaryButton`: text only, label `--content-action` (e.g. "Skip", app bar actions; in an app bar
  use 12px horizontal padding so the text lines up with the screen margin).
- `AppIconButton`: a 48px round touch target with a 24px icon, `--content-primary`.

## AppChip

A compact pill button, e.g. a row of filters to start from: height 36px, `--radius-button`, padding
`--space-lg` (`--space-md` on the right when it has an arrow), label in `--font-button`.
- default: `--background-surface` with a 1px `--background-separator` border, label `--content-primary`
- action: transparent with a 1px `--content-action` border, label `--content-action` (e.g. Clear all)
- optional 16px down arrow after the label, `--space-xs` from it, when it opens a menu

## AppSearchBar

A search field in a card: `--background-surface`, `--radius-surface`, at least 48px tall, padding
`--space-lg` at the sides and 9px top and bottom. A 24px search icon in `--content-secondary`, then the
text in `--font-body` (placeholder in `--content-secondary`), `--space-sm` apart. No outline or colour
change when focused. The screen can put content before the text, e.g. filters as 30px tokens, which
wraps onto more lines with the text, 6px apart.

## AppOverflowMenu / AppMenu

Further options behind a ⋮ icon button, in an `AppBar`'s actions or as a row's trailing content.
Tapping it opens `AppMenu` below the button, 4px clear of it, its end 12px in from the button's end
(so in an app bar it keeps the 16px screen margin). An `AppChip` with a down arrow opens one lined up
with its start, to pick one of several options. The menu is a card in `--background-surface` with a 1px
`--background-separator` border, `--radius-surface`, a soft shadow, at least 200px wide and `--space-sm`
padding top and bottom. Each option is an `AppListItem` row (56px, `--font-body`, `--content-primary`)
with an optional 24px icon before the label, and a 24px tick in `--content-action` after the current
choice when picking one of several. Picking one closes the menu. No dividers between options.
The app's XML popup menus look the same.

## AppCheck

Whether a row is picked, as its trailing content: a 24px circle with a 2px `--background-separator`
ring, or filled `--content-action` with a 14px tick in `--content-on-action` when picked.

## AppImage / AppThumbnail

`AppImage` is an image from a URL. `AppThumbnail` is a row's 48px square image with 12px corners, as an
`AppListItem`'s leading content: the illustration on white, or without one a 24px icon in
`--content-secondary` on `--background-surface-raised`.

## Shimmer

Loading placeholders are drawn in `--background-surface-raised` with a highlight sweeping across them
(about 1.2s), all in step. Each screen lays out its own placeholders to match what's loading.

## AppFooter

Actions pinned to the bottom of an `AppScreen`, in a floating card:
- inset `--space-lg` from the sides and `--space-sm` from the bottom (above the system navigation bar)
- radius `--radius-surface`, 1px `--background-separator` border, padding `--space-lg`
- background: the content behind it blurred (20px) under `--background-blur-overlay`
  (`backdrop-filter: blur(20px)`)
- buttons stacked full width, `--space-sm` apart, in the order given: the primary `AppButton` first,
  a secondary or tertiary action under it. A single action keeps the lower slot.
- optional note above the actions in a small card: `--background-primary`, `--font-caption`,
  `--content-secondary`, padding `--space-lg`
- when the screen says its content is new (a step of a flow), the actions crossfade in place and the
  card's height follows; otherwise text changes, like a count in "Add 2 exercises", update in place

## AppDialog

An alert: background `--background-surface`, radius 28px (Material's dialog default), padding 24px, title in `--font-heading`,
an optional line of body text in `--font-body` / `--content-secondary`, optional content under it
(`--space-lg` apart), and up to two buttons side by side at the bottom right, sized to their text:
`AppSecondaryButton` (the way out) then `AppButton` (the action we want).

## AppCircularProgressIndicator

A spinner in `--content-action`.

## AppIcon

24px icons in `--content-primary` (or `--content-action` for accents), from the app's own vector
drawables.
