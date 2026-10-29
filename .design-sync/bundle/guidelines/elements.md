# Everlog elements

The app's design system is Jetpack Compose (`common:compose` elements in the Android repo), so there are no
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
- `AppIconButton`: a 48px round touch target with a 24px icon, `--content-primary`. Outlined: in a 40px circle
  with a 1px `--background-separator` border, for an icon button standing next to content rather than in an
  app bar (`AppOverflowMenu` takes the same option for a card's ⋮).

## AppChip

A compact pill button, e.g. a row of filters to start from: height 36px, `--radius-button`, padding
`--space-lg` (`--space-md` on the right when it has an arrow), label in `--font-button`.
- default: `--background-surface` with a 1px `--background-separator` border, label `--content-primary`
- action: transparent with a 1px `--content-action` border, label `--content-action` (e.g. Clear all)
- selected: `--background-selected` with a 1px `--content-action` border, label `--content-action`
  (e.g. the current tab in a row of chips that switch pages, 8px apart; the app's XML tabs look the same)
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
with an optional 24px icon before the label (16px from the edge and from the label; options without one
leave its space when others have one, so the labels line up), and a 24px tick in `--content-action` after the current
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

## AppKeypad

A number keypad in a floating card, docked over the bottom of a screen like `AppFooter` (same insets, blur and
tint), radius 30px, 1px `--background-separator` border, padding 14px, its parts `--space-sm` apart. Its keys,
toggle and pills sit in a darker inset, `rgba(0,0,0,0.32)`, with a 1px `--background-separator` border. Everything
inside it (keys, toggle, pills, strips) is fully rounded, `--radius-button`, like the app's buttons.
- closed: one row of controls: a 42px circle keyboard button that opens it, small pills (42px tall,
  `--font-button`, padding `--space-md`, e.g. Add exercise), and the primary `AppButton` at the end
- open: a header row (a 42px circle ⌄ that closes it; a label in `--font-small` / `--content-secondary`,
  e.g. SET 2 OF 4 · KG, over a title in `--font-body-bold`; an optional segmented toggle, e.g. Reps | Time,
  the picked segment filled `--content-action` with `--content-on-action` text), then four rows of 48px keys
  6px apart: 1 2 3 −, 4 5 6 +, 7 8 9 ⌫, . 0 and one wide main key two columns across, filled
  `--content-action`, whose label the screen sets (Next, Complete set, Done). Digits in `--font-heading` with
  tabular figures, words in `--font-button`. "." is dimmed where only whole numbers go.
- strips, above the keys or the closed row:
  - timer: 50px tall, a label in `--font-small` over the time left in `--font-heading` (tabular), then three
    36px pills (−10s, +10s, Skip). Active (something being timed): `--content-action` on
    `--background-selected` with a `--background-action-muted` border. Rest: amber `#F2B84B` on `#2A2210`
    with a `#6B5420` border (the keypad's own colours).
  - note: a line of `--font-caption`, in `--content-secondary` on `--background-primary`, or highlighted
    (something the app suggests) in `--content-action` on the inset, with an optional pill at its end (Why?)

## AppSnackbarHost

A short message over the bottom of the screen, e.g. "Set deleted" with Undo: `--background-surface-raised`,
`--radius-surface`, text `--content-primary`, the action in `--content-action`, across the screen inside the
16px margins, just above the footer.

## AppBottomSheet

A sheet that slides up over the screen from the bottom, on `--background-primary` with Material's rounded top
corners and drag handle, for a short set of options (e.g. a workout's options): an optional title in
`--font-heading`, then its content, usually `AppListGroup`s, `--space-lg` apart and from the sides. Picking an option
slides the sheet away first, then acts; tapping outside it or swiping it down closes it.

## AppDialog

An alert: background `--background-surface`, radius 28px (Material's dialog default), padding 24px, title in `--font-heading`,
an optional line of body text in `--font-body` / `--content-secondary`, optional content under it
(`--space-lg` apart), and up to two buttons side by side at the bottom right, sized to their text:
`AppSecondaryButton` (the way out) then `AppButton` (the action we want). A third, quieter choice (like the
system dialog's neutral button, e.g. Keep going) is an `AppTertiaryButton` at the start of the same row, with the
other two at the end.

## AppCircularProgressIndicator

A spinner in `--content-action`.

## AppIcon

24px icons in `--content-primary` (or `--content-action` for accents), from the app's own vector
drawables.
