# Everlog design system

Everlog is an Android workout tracker. Its design system is Jetpack Compose, so this project has
**no importable components**: it gives you the tokens (`styles.css`) and a spec for every element
(`guidelines/elements.md`). Read both before designing. Build each element from plain markup and
the tokens, exactly as specified there, so every design maps 1:1 onto the app's Compose elements.

## Setup

- Link `styles.css`. It sets the dark background and primary text on `body`. The app is **dark only**:
  never design a light variant.
- Design for a phone frame 412px wide (1px = 1dp). Tablets cap content at `--content-max-width`.
- Fonts: the system font (Roboto). Use the `--font-*` shorthands, never set sizes by hand.

## Styling idiom: CSS variables only

No utility classes, no component library. Style with these variables (all defined in `tokens/`):

| Family | Variables |
|---|---|
| Backgrounds | `--background-primary`, `--background-surface`, `--background-surface-raised`, `--background-blur-overlay`, `--background-separator`, `--background-selected`, `--background-action-muted` |
| Content | `--content-action` (the green accent), `--content-action-pressed`, `--content-on-action`, `--content-primary`, `--content-secondary`, `--content-destructive` |
| Type (`font:`) | `--font-title` 30, `--font-heading` 20, `--font-subtitle` 15, `--font-body` 16, `--font-body-bold`, `--font-caption` 13, `--font-small` 12, `--font-button` 14 |
| Spacing | `--space-xs` 4, `--space-sm` 8, `--space-md` 12, `--space-lg` 16, `--space-xl` 32 |
| Shapes | `--radius-surface` 24, `--radius-button` (pill) |

Never hardcode a colour, size or spacing a variable covers.

## Layout rules

- 16px (`--space-lg`) screen margin on both sides.
- Content lives in cards (`--background-surface`, `--radius-surface`). Lists are **groups**: rows
  inside one card joined by 1px `--background-separator` dividers, never separate cards with gaps.
- Groups and cards are `--space-lg` apart, the same as the side margin. A section header
  (`--font-subtitle`, `--content-secondary`) sits `--space-lg` above its group.
- Primary actions go in the floating `AppFooter` at the bottom, never inline mid-page. Every button
  is a pill.
- Use the element names (`AppScreen`, `AppBar`, `AppListGroup`, `AppListItem`, `AppFooter`,
  `AppButton`...) in comments or layer names. If a design needs something not in
  `guidelines/elements.md`, label it as new so the team can decide whether it joins the design system.

## Example

```html
<!-- AppListGroup -->
<section style="padding: var(--space-lg)">
  <h2 style="font: var(--font-subtitle); color: var(--content-secondary); margin: 0 0 var(--space-lg)">General</h2>
  <div style="background: var(--background-surface); border-radius: var(--radius-surface); overflow: hidden">
    <!-- AppListItem -->
    <div style="padding: var(--space-lg); min-height: var(--list-row-min-height)">
      <div style="font: var(--font-body)">Weight unit</div>
      <div style="font: var(--font-caption); color: var(--content-secondary); margin-top: var(--space-xs)">Kilograms</div>
    </div>
    <div style="height: 1px; background: var(--background-separator)"></div>
    <!-- AppListItem -->
    <div style="padding: var(--space-lg); min-height: var(--list-row-min-height)">
      <div style="font: var(--font-body)">Rest timer</div>
      <div style="font: var(--font-caption); color: var(--content-secondary); margin-top: var(--space-xs)">90 seconds</div>
    </div>
  </div>
</section>
<!-- AppFooter -->
<footer style="position: fixed; left: var(--space-lg); right: var(--space-lg); bottom: var(--space-sm);
  padding: var(--space-lg); border-radius: var(--radius-surface); border: 1px solid var(--background-separator);
  background: var(--background-blur-overlay); backdrop-filter: blur(20px)">
  <!-- AppButton -->
  <button style="width: 100%; min-height: var(--button-min-height); padding: var(--space-sm) var(--space-xl);
    border: 0; border-radius: var(--radius-button); background: var(--content-action);
    color: var(--content-on-action); font: var(--font-button)">Save</button>
</footer>
```
