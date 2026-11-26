# PomoZone: design tokens and screen layouts

The visual reference is `docs/design-preview.html`. This file is the text version for UI tickets. Where they disagree, this file wins.
Colors, type and shapes are already in `ui/theme` (Color.kt, Type.kt, Theme.kt). Use them. Do not invent colors, sizes or copy.

## Colors

| Token | Light | Dark | Used for |
|---|---|---|---|
| panel (screen background, `colorScheme.background` and `surface`) | #FFFDF9 | #221B17 | screen background, bottom bar |
| canvas (`surfaceVariant`) | #F6F1EA | #17120F | cards and chart panels |
| ink (`onSurface`) | #2A211C | #F3EBE3 | main text |
| mute (`onSurfaceVariant`) | #7B6E65 | #A1948A | secondary text, inactive icons |
| line (`outline`) | #E6DDD2 | #3A2F29 | ring track, dividers, empty dots |
| focus (`primary`) | #E5533D | #E5533D | Focus mode |
| short break (`secondary`) | #3F9B6E | #3F9B6E | Short break mode |
| long break (`tertiary`) | #4A7BD0 | #4A7BD0 | Long break mode |
| nav pill (`primaryContainer` / `onPrimaryContainer`) | #F6C9BF / #B8321F | #5A2D25 / #F07A62 | selected tab pill and icon |

Heatmap levels (`PomoTheme.colors.heat[0..4]`):

| Level | Minutes that day | Light | Dark |
|---|---|---|---|
| 0 | 0 | #EFE7DC | #2E2520 |
| 1 | 1 to 25 | #F6C9BF | #5A2D25 |
| 2 | 26 to 50 | #EF9A87 | #8F3D2E |
| 3 | 51 to 100 | #E5533D | #D2503A |
| 4 | more than 100 | #B8321F | #F07A62 |

The current mode's color is used for the ring, the selected mode chip and the main button.
Today's bar in the weekly chart uses heat level 4. Other bars use focus.

## Type (system font, no custom fonts)

| Use | Size | Weight | Notes |
|---|---|---|---|
| Time in the ring | 46sp | Bold | tabular digits, letter spacing -0.02em |
| Card value, goal label | 20sp | Bold | |
| Body | 14sp | Normal | |
| Tab label, mode chip | 12sp / 11sp | Medium (Bold when selected) | |
| Card title, panel title | 10sp / 11sp | Medium | UPPERCASE, letter spacing 0.06em, mute color |
| Chart axis labels | 9sp | Normal | mute color |

## Shape and spacing (dp)

- Screen horizontal padding 18. Gap between cards and panels 8.
- Cards and panels: radius 14, padding 8 vertical and 12 horizontal (panels 10 vertical).
- Mode chip: pill shape, 5 vertical and 11 horizontal padding, 1 outline in line color; selected chip is filled with the mode color and white text.
- Progress ring: 216 across, stroke 12, round caps, track in line color, starts at the top and drains clockwise as time passes (it shows the time remaining).
- Session dots: 10 across, 8 apart. Completed is filled in focus color, the current block is an outline of 2, the rest are line color.
- Round buttons: 46 across with a 1 outline in line color; the main button is 72 across filled with the mode color, white icon 26.
- Bottom bar: solid panel color, 1 top line in line color. Selected tab: 56 by 30 pill (radius 15) in nav pill colors, icon 22, label bold in ink. Unselected: mute. Keep clear of the gesture bar.
- Goal ring: 56 across, stroke 6, round caps.
- Weekly bars: chart area 78 high, 7 bars 7 apart, top corners radius 5, labels (M T W T F S S) 9sp below.
- Heatmap: 12 columns by 7 rows, cells 11 high, 3 gap, radius 3. Columns are weeks (current week last), rows are Monday to Sunday. Legend below, right aligned: "Less", five 9 by 9 swatches radius 2, "More".

## Timer screen, top to bottom

1. Mode chips row, centered: Focus, Short break, Long break.
2. Progress ring with the remaining time (mm:ss) and under it the small label "Session 2 of 4" (during a break: sessions completed in the cycle, for example "2 of 4 done").
3. Session dots, 4 of them.
4. Controls at the bottom: reset (left), main button (center, Start / Pause / Resume), skip (right).

## Stats screen, top to bottom

Content scrolls if it does not fit. It never draws behind the bottom bar.
1. Four cards in 2 columns: Today, This week, All time, Streak. Values look like "1h 40m"; the streak looks like "5 days".
2. Goal panel: goal ring on the left, on the right a bold "3 of 4" and below it "sessions today".
3. Weekly panel, title "Last 7 days (minutes)": seven bars, oldest left, today right and highlighted.
4. Heatmap panel, title "Last 12 weeks", with the legend.
5. Debug builds only: a "Load demo data" text button at the very bottom.

## Empty state (no sessions yet)

All cards show 0 (streak "0 days"), the bars are empty, the heatmap is all level 0, and one line of mute text under the heatmap says: "Complete a focus session to see your stats."

## Motion

- Ring progress: animate smoothly between updates (linear, about 1 second).
- Weekly bars: grow up from the baseline when the Stats screen first appears, about 0.9 seconds, ease-out. They do not replay on later updates.
- No other animation.

## Notification copy

- Focus complete, short break next: title "Focus session complete", text "Nice work. Take a 5 minute break."
- Focus complete, long break next: title "Focus session complete", text "Great cycle. Take a 15 minute break."
- Break over: title "Break over", text "Ready for your next focus session?"
- Channel name: "Timer alerts".
