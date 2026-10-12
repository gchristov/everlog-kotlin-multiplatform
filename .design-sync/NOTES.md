# Claude Design sync notes

- **Off-script, tokens-only sync.** The design system (`ui/design`) is Jetpack Compose, which can't run
  in Claude Design, so no components are uploaded. Decided 2026-10-11: ship tokens + a written element
  spec rather than React copies of the elements (a second implementation that would drift).
- **Sources are hand-written** in `.design-sync/bundle/` (tokens, `guidelines/elements.md`, preview cards)
  and `.design-sync/conventions.md` (becomes README.md, the design agent's prompt header).
  `.design-sync/build.sh` assembles `ds-bundle/`. The skill's converter (`package-build.mjs`) is not used.
- **Keep it in step with the code.** When `ui/design/theme` or an element changes, update the matching
  token or section in `guidelines/elements.md`, rebuild and re-sync. Values come from `theme/*.kt` and
  `res/values/colors.xml` (ARGB colours become `rgba()`); element sizes from each element's source
  (e.g. AppDialog uses Material's 28dp corners, not `--radius-surface`).
- Onboarding's selected-answer styling (`--background-selected` + `--content-action-pressed` outline)
  is local to onboarding, not a design system element; the tokens exist, the element spec omits it.
- **No Node on this Mac.** Run the validator in Docker:
  `docker run --rm -v <skill-dir>:/skill:ro -v "$PWD/ds-bundle":/work/ds-bundle -w /work node:22-slim node /skill/package-validate.mjs ds-bundle --no-render-check`.
  Expected warnings: `_ds_sync.json absent` (off-script, no anchor, so every sync is a full upload) and
  `[RENDER_SKIPPED]` (no component previews). Check the cards with headless Chrome
  (`--headless=new --screenshot`) instead.
- No font files ship: the app uses the system Roboto, and designs fall back to the system sans-serif.
