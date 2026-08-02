# Hǎo Shield — Midjourney art direction

The app already owns a visual language, set by the two journal pieces
(`ill_open_window.png`, `ill_mountains.png`): **loose, confident hand-drawn ink line
sketches** — a single dark line on white, sparse cross-hatching only where a shadow is
needed, and a great deal of air. Everything below extends that language. No washes, no
gradients, no colour: the app's palette does the colouring (lines are tinted Ink
`#3E4A3D` on paper, Ink-lifted `#A8B5A5` at dusk).

## The base suffix

Append this, unchanged, to every prompt. Consistency comes from the suffix, not luck:

```
minimal hand-drawn ink line illustration, loose confident pen strokes, thin even line
weight, sparse cross-hatching only where shadow is needed, single black ink on plain
white background, generous negative space, quiet wabi-sabi restraint, in the spirit of
Chinese literati sketching --style raw --stylize 50 --v 6.1
--no color, gray fill, gradients, watercolor, shading, texture, border, frame, text, signature
```

Add the `--ar` per piece as noted. If a batch renders too ornate, lower `--stylize` to 25;
if lines come out heavy, lead the prompt with "fine pen sketch". When one image in a batch
lands the feel, note its `--seed` and reuse it across the rest of the set so the whole app
reads as one hand.

---

## Intro cards (one small piece above each title, `--ar 4:3`)

**1 · The Nature of Hǎo** — the character 好 is literally 女 + 子, mother and child.
Draw the etymology, not a symbol:

> a mother and child sitting together on a low wooden bench beneath a blossoming plum
> branch, seen from behind, [base suffix] --ar 4:3

**2 · What Pulls Us Away** — the true thing stays whole; only its reflection is disturbed:

> a full moon above calm water, its reflection broken into fragments by wind ripples,
> nothing else, [base suffix] --ar 4:3

**3 · The Need for Space** — a threshold onto emptiness, rhyming with the journal's window:

> a round moon gate in a plain garden wall, opening onto mist and one distant mountain
> ridge, empty stone path leading through, [base suffix] --ar 4:3

**4 · What Hǎo Shield Offers** — shelter and invitation; the teacup already lives on the
journal's windowsill, so let it recur:

> a small open-sided thatched pavilion sheltering a stone table with a teapot and two
> cups, pine branch overhead, [base suffix] --ar 4:3

**5 · How It Works** — the ritual object, the deliberate gesture:

> a hand gently placing a small round stone token on a plain wooden table, seen from
> above at a slight angle, [base suffix] --ar 4:3

## The other rooms of the app

**Home** (the anchor piece, small, beneath the glyph) —

> a distant temple roofline among pine trees rising above a band of mist, far away and
> small, [base suffix] --ar 3:2

**Session / protected screen** (tall and thin, beside or below the clock — time passing
without being counted) —

> a single stick of incense in a small holder, one unbroken thread of smoke rising and
> curling once, [base suffix] --ar 2:3

**Blocking overlay** ("this app is resting" — stillness that can be woken) —

> a temple bell hanging perfectly still from a wooden beam, its striker at rest beside
> it, [base suffix] --ar 1:1

**Unblock / write an intention** —

> a calligraphy brush resting across an inkstone beside a blank sheet of paper, seen
> from above, [base suffix] --ar 3:2

**End-of-session reflection** —

> a single crane standing in still shallow water, one leg raised, reeds at the edge,
> [base suffix] --ar 3:2

**Make Your Shield guide** (craft, made by hand) —

> a pair of hands folding a square of paper on a plain wooden table, seen from above,
> [base suffix] --ar 3:2

**Settings** (tending, small adjustments) —

> a small potted pine on a wooden shelf, pruning scissors lying beside it, [base suffix]
> --ar 3:2

## Wide dividers (the `ill_mountains` register, `--ar 8:3`)

Interchangeable horizontal bands for list headers, section breaks, empty states:

> a receding ridgeline of pine-covered hills drawn in one continuous contour line,
> [base suffix] --ar 8:3

> a loose skein of cranes flying across an empty sky, drawn small, [base suffix] --ar 8:3

> a band of wind-bent reeds along a riverbank, drawn sparse, [base suffix] --ar 8:3

---

## From Midjourney to the app

1. **Choose from one batch.** Generate 4–8 per subject, pick the ones that share a hand;
   family resemblance beats individual beauty.
2. **Post-process:** threshold to pure black on white → remove the white to transparency →
   export PNG at 3× the dp size (the mountains are 240×85dp → 720×255px in
   `drawable-xxhdpi`). For crisp scaling, vectorize with potrace and ship a tinted
   `VectorDrawable` instead.
3. **Tint in the app, not in the file.** Keep source art pure black; render with the
   theme's ink so dusk mode works for free (the existing pieces sit at `inkSoft`/`inkFaint`
   emphasis — art should whisper).
4. **Placement discipline — the "without going overboard" rule:** at most one piece per
   screen; sized like the existing two (roughly 200–240dp wide); never behind text; never
   competing with the 好 glyph on screens the glyph anchors. If a screen already feels
   calm, it does not need a picture.

---

## The renders — accepted set, names, and sizes

Fourteen renders were accepted (2026-08-02). Process each with `scripts/prepare_art.py`
(`SRC ill_name WIDTH_PX --trim`); widths are 3× the placement dp for `drawable-xxhdpi`.

| # | Render | Asset name | dp (w) | px (w) | Placement | Tint |
|---|--------|-----------|--------|--------|-----------|------|
| 1 | Mother & child under plum branch | `ill_intro_nature` | 220 | 660 | Intro 1 | inkSoft |
| 2 | Moon over rippled water | `ill_intro_pulled` | 220 | 660 | Intro 2 | inkSoft |
| 3 | Moon gate onto misty ridge | `ill_intro_space` | 220 | 660 | Intro 3 | inkSoft |
| 4 | Thatched pavilion, teapot, two cups | `ill_intro_offers` | 220 | 660 | Intro 4 | inkSoft |
| 5 | Hand placing the token | `ill_intro_works` | 220 | 660 | Intro 5 | inkSoft |
| 6 | Temple roof among pines over mist | `ill_home_temple` | 240 | 720 | Home, beneath content | inkFaint |
| 7 | Incense stick, thread of smoke | `ill_session_incense` | 120 | 360 | Session screen | inkFaint |
| 8 | Temple bell hanging still | `ill_overlay_bell` | 160 | 480 | Blocking overlay | values/values-night colour |
| 9 | Hands folding paper | `ill_guide_folding` | 220 | 660 | Make Shield guide | inkSoft |
| 10 | Brush, inkstone, blank sheet | `ill_unblock_brush` | 200 | 600 | Unblock / intention | inkSoft |
| 11 | Skein of red-crowned cranes | `ill_div_cranes` | 300 | 900 | Wide divider | **--keep-color**, no tint |
| 12 | Pine ridgeline | `ill_div_pines` | 300 | 900 | Wide divider / journal alt | stone |
| 13 | Crane in still water | `ill_reflection_crane` | 220 | 660 | End-of-session reflection | inkSoft |
| 14 | Wind-bent reeds by water | `ill_div_reeds` | 300 | 900 | Wide divider | stone |

Notes:
- **The cranes (#11)** carry their red crowns — the palette's one accent, arrived by
  itself. Keep the colour (`--keep-color`), never tint, and if it reads too dark at dusk
  give it a `drawable-night` variant (recolour ink → `#A8B5A5`, crowns → `#C0705C`).
- **The bell (#8)** sits on the blocking overlay, which is a plain View layout — tint via
  `app:tint`/ImageView colour resource backed by `values` + `values-night`, like the
  overlay's other colours.
- Tint emphasis follows the journal precedent: content-adjacent pieces at `inkSoft`,
  ambient/background pieces at `inkFaint` or `stone`. Art whispers.
