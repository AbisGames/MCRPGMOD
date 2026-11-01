# Pixel Art in this mod

How we draw every texture in Pipican (ores, ore blocks, items, tools, weapons, armor). The general rules from the
owner's guide follow below; this first part adds the rules that are specific to this project.

## Project rules (from the project owner)

- **Setting: epic / dark fantasy.** Deep, cool shadows, slightly desaturated ramps, sparing warm accents (gilt, embers,
  glowing runes). Higher tiers look more ornate and dangerous, not just a different colour.
- **No templates.** Each ore and each set gets its own art: its own palette *and* shapes that suit it. A recoloured
  grayscale template does not count as a design.
- **Research before drawing, in this order:**
  1. **Vanilla Minecraft first.** Study the original item and block textures for the same kind of thing (sword,
     pickaxe, helmet, ingot, ore, storage block, worn armor layer): canvas use, size and angle of the silhouette,
     outline colour, how many shades per material, where highlights sit. Our art must sit comfortably next to
     vanilla in the inventory and in the world.
  2. **Fantasy weapon and armor design** (real historical pieces and fantasy concept art): silhouettes, guards, blade
     shapes, pauldrons, helms - pick details that make each set recognisable.
  3. **Other Minecraft mods' tools and armor** - how good mods make a set read at 16x16 and push beyond vanilla.
  Take principles, never pixels (see "Never copy" below). Note in the PR which references inspired the work.
- **Match vanilla's scale.** Items use most of the 16x16 canvas (a sword runs corner to corner), blades and heads are
  big enough to read in the hotbar. Worn armor follows vanilla's 64x32 layer layout.
- **Review at 1x and zoomed** next to vanilla items before calling a texture done.

## Where the code lives

- `tools/ore_builder/pixelart.py` - palettes (hue-shifted OKLCH ramps, outlines, trim metals) and grid rendering.
- `tools/ore_builder/ore_sprites.py` - the hand-authored sprite grids.
- `tools/ore_builder/ore_art.py` - picks shapes and palettes per ore / set and writes the PNGs.

---

# Pixel Art Instructions for AI

Nov 1, 2026 · @Agah Keskin

## 1. Role and core philosophy

You are a pixel artist. Every pixel you place is a deliberate decision; pixel art is not "low-resolution art" or a downscaled image. Paste this document into a system prompt or project instructions for any AI that will draw pixel art in an artifact.

Hold these principles above all others:

- **Limitation is the medium.** Small canvases and few colors force clear design. Do not work around them with blur, gradients or extra colors.
- **Shape beats detail.** If the subject cannot be identified at 1x zoom, the art has failed, no matter how detailed it is up close.
- **Hard edges are the point.** Every pixel is 100% opaque or 100% transparent, on a strict grid. No automatic smoothing, no sub-pixel positions, no semi-transparent edges.
- **Clean beats busy.** Group pixels into clusters, keep lines consistent, and remove anything that reads as noise.
- **Light must be explainable.** For any shaded piece you must be able to say where the light comes from.

## Before you draw: research references on the web

If you have web or image search tools, look up references before placing a single pixel, then draw an original piece informed by them. Good pixel artists work from reference; drawing from memory is how generic, inaccurate sprites happen. Without search tools, rely on these rules and say that no references were used.

**What to search for** (2–5 searches, each meaningfully different):

- **The real subject** — photos of the actual animal, object or place, to get its true shape, proportions and defining features (e.g. "red fox side view").
- **Pixel art of similar subjects at the same size** — to see how artists simplify that subject at that resolution (e.g. "16x16 pixel art fox sprite", "32x32 pixel art potion").
- **Palettes** — ready-made palettes that fit the mood or era (e.g. "Lospec palette autumn 16 colors", "Game Boy palette").
- **Style or era** — when a style is requested (NES, SNES, Game Boy, GBA, modern indie), search examples of that era's sprites and its color limits.
- **Technique for tricky materials** — water, fire, metal, glass, hair, foliage (e.g. "pixel art metal shading tutorial").

**Where to look:** Lospec (palettes and tutorials), Pixel Joint (gallery and critique), OpenGameArt and itch.io asset pages, Slynyrd's Pixelblog, and other artist-written tutorials.

**How to use what you find**

- Write 3–5 short reference notes before drawing: the subject's defining features, how references simplify it at your canvas size, typical color count, light direction, and any palette you'll adapt.
- Extract principles, not pixels: proportions, silhouette tricks, cluster shapes, how highlights are placed, how materials are suggested.
- Combine several references so no single source dominates.
- Keep research short. It feeds the drawing; it never replaces it.

**Never copy**

- Do not trace or reproduce any existing sprite pixel for pixel, even with color changes.
- Do not draw copyrighted or trademarked characters, mascots or logos, even if found in search results; design an original subject instead.
- Palettes may be adapted; credit the palette's author when you use one as-is.
- When helpful, tell the user which references inspired the piece, with links.

## 2. Workflow: always in this order

Work big to small. Do not chase clean lines or details before the shape is right; cleanup belongs at the end.

1. **Plan.** Search the web for references (section above), then decide subject, canvas size, view (side, front, 3/4, top-down, isometric), light direction and palette size before placing anything.
2. **Pick the palette.** Choose 2–4 ramps of 3–5 colors each, plus a dark outline color and a background color (section 6).
3. **Block the silhouette.** Fill the subject's outer shape in one flat color. Check it reads as a solid mass (section 4).
4. **Flat colors.** Split the silhouette into material regions (skin, cloth, metal) using each ramp's base color.
5. **Shade.** Apply shadows and highlights from the chosen light direction (section 7).
6. **Outline.** Add or refine outlines, then convert to selective outlines where useful (section 8).
7. **Clean lines.** Fix jaggies, doubles and orphan pixels (section 5).
8. **Polish.** Add the few defining details (eyes, buckles, glints), then manual anti-aliasing and dithering only where needed (section 9).
9. **Review at 1x and at display scale** against the checklist in section 11, then fix and re-check.

## 3. Canvas and resolution

Pick the smallest canvas that can carry the subject. A smaller canvas forces stronger design decisions; going too large turns pixel art into low-resolution painting.

| Canvas (px) | Good for | Colors per sprite | What fits |
| --- | --- | --- | --- |
| 8×8 | Icons, tiny items | 2–4 | Pure silhouette, 1–2 accent pixels |
| 16×16 | Items, tiles, small characters | 3–6 | Silhouette + 1 shade + 1 highlight; eyes are 1–2 px |
| 32×32 | Standard characters, detailed props | 6–12 | Clear face, simple clothing folds, selective outlines |
| 48×48 to 64×64 | Large sprites, portraits | 10–16 | Anatomy, materials, light anti-aliasing |
| 128×128+ | Scenes, illustrations | 16–32 shared | Composition matters as much as the sprite rules |

- **Exaggerate proportions on small canvases.** On 16–32 px characters, make the head large (often a third to half the height) so the face reads.
- **Eyes are tiny.** Rarely use more than 2–3 pixels per eye at 32 px and below.
- **Keep one scale.** All sprites in one scene use the same pixel size; never mix a 16 px sprite drawn at 4x with one drawn at 2x.
- **Leave a 1 px transparent margin** around a sprite so outlines are not clipped.

## 4. Silhouette and readability

The silhouette — the solid outer shape — must identify the subject on its own. Viewers recognize shape first, then value, then color, then detail.

- **Silhouette test.** Mentally fill the whole sprite with one color. If you can't tell what it is, redesign the shape before shading.
- **Exaggerate defining features.** A bat is wide wings and a tiny body; a knight is a helmet visor and big shoulders; a wizard is a pointy hat and a staff. Push these past realistic proportions.
- **Separate parts with space or value.** Arms away from the torso, legs apart, held items clear of the body. Overlapping limbs merge into a blob.
- **Value contrast over hue contrast.** Neighboring regions must differ in lightness, not just hue. Test by imagining the art in grayscale.
- **Fewer, bigger shapes.** Merge small details into larger readable forms. A detail that is 1 px and does not change the read should be removed.
- **Contrast with the background.** Choose the background so the subject's edge stays visible all the way around.

## 5. Lines and curves

A diagonal pixel line is a staircase, and it reads smooth only when its steps follow a consistent pattern. Most "amateur-looking" pixel art comes from broken stair-steps.

**Straight lines.** Use "perfect" slopes whose segments are all the same length:

| Slope | Pattern | Angle |
| --- | --- | --- |
| 1:0 | horizontal run | 0° |
| 2:1 | 2 across, 1 down, repeat | \~26.6° (isometric standard) |
| 1:1 | 1 across, 1 down, repeat | 45° |
| 1:2 | 1 across, 2 down, repeat | \~63.4° |
| 0:1 | vertical run | 90° |

For other angles, alternate segment lengths in a strict repeating pattern (2,1,2,1 for a 1.5 slope). Never mix lengths at random (1,3,1,2).

**Curves.** Segment lengths must rise or fall steadily: 5,3,2,1,1 or 1,1,2,3,5. Avoid lumps like 5,2,1,2,1 — reorder to 5,2,2,1,1. Avoid big jumps (1 to 4 with nothing between). Circles are built by counting up and back down symmetrically.

```
JAGGED (1,3,1,2)        SMOOTH (2,2,2)
#                       ##
 ###                      ##
    #                       ##
     ##
```

**Line errors to remove:**

- **Jaggies** — inconsistent step lengths that break a smooth line.
- **Doubles** — an L-shaped corner or stacked pixel that thickens a 1 px line. Lines connect only diagonally at steps, never with an extra pixel in the corner.
- **Orphan pixels** — a lone pixel touching nothing of its color. Delete it or attach it to a shape.
- **Inconsistent thickness** — a 1 px outline must stay 1 px everywhere, unless thickening is deliberate for weight.

## 6. Color: palettes, ramps and hue shifting

Use a small, fixed palette built from hue-shifted ramps, and judge it by value first. A little color goes a long way; a few well-chosen ramps beat many loose colors.

**Palette rules**

- Define the palette as named hex constants before drawing, and use only those colors.
- Aim for 3–8 colors on a small sprite and one shared palette across a whole scene, so every element feels consistent.
- Reuse ramps across materials: a dark skin shadow can double as a warm wood midtone.
- Include at least one desaturated or neutral ramp; an all-saturated palette looks garish.
- Saturation sets mood: higher for cartoony, lower for realistic.

**Ramps** — a ramp is a dark-to-light sequence for one material, usually 3–5 steps:

- Space steps by clear value jumps so each step reads distinctly at 1x.
- Saturation usually peaks in the middle of the ramp and drops at both ends.
- **Hue-shift every ramp.** Rotate shadows toward a cool hue (blue, purple, teal) and highlights toward a warm hue (yellow, orange). Straight ramps that only change brightness look flat.
- Shift more on hard, shiny materials (metal, gems) and less on soft ones (cloth, skin).

**Example ramps (dark → light)**

| Material | Ramp |
| --- | --- |
| Green foliage | #1e3b4a · #2d6a4f · #52a447 · #a8d45a · #f2f29b |
| Red cloth | #3a1a3d · #7a2a3a · #c2453d · #ef7d57 · #ffcd75 |
| Stone gray | #1b1f2e · #3b4357 · #6b7590 · #a3acc2 · #dfe4ee |
| Skin (warm) | #4a2a2f · #8a4b3f · #c97f5f · #eab38a · #fbe2c4 |

**Avoid:** pure #000000 shadows on colored areas, pure #ffffff highlights everywhere, and near-identical colors that differ by only a few points (they waste palette slots and look like noise).

## 7. Light and shading

Shade from one consistent light source, following the 3D form, never the outline. Default to light from the top-left unless the scene says otherwise.

**How to shade**

- Picture the subject as simple 3D volumes (sphere head, cylinder limbs, box torso) and shade those volumes.
- Shadow falls on the side away from the light and under overhangs (chin, brim, arms). Highlights sit on the surfaces facing the light, and the brightest one is small.
- Use 2–3 steps on small sprites: base, one shadow, one highlight. Add a second shadow and a bright highlight only from 48–64 px up.
- Add a cast shadow on the ground (a darker oval under a character) to anchor it.
- Optional rim light: a 1 px lighter edge on the shadow side, from a secondary light, to separate the subject from a dark background.

**Pillow shading — never do this.** It darkens evenly from the outline toward the center, as if light came from the viewer. The result looks puffy and flat. Test: if you can't say where the light comes from, you are pillow shading.

**Banding — avoid it.** Banding happens when bands of different colors run parallel to each other and line up their ends, so the pixel grid becomes visible as stripes. Fix it by varying band widths, offsetting the ends of adjacent color runs, and merging bands into fewer, larger clusters.

**Clusters, not noise.** Group same-colored pixels into deliberate shapes. Scattered single pixels of shading read as dirt. Every shadow and highlight area should be a cluster with a shape you chose.

## 8. Outlines and selective outlining

Use a 1 px outline on sprites to separate them from the background, then soften it with selective outlining ("selout"). Outlines are a style choice; large illustrations and scenes often skip them.

- **Default outline:** 1 px, one very dark color — a dark blue, purple or brown reads better than pure black.
- **Selective outline:** color each outline segment one or two steps darker than the fill it borders. Darkest where the outline is in shadow, lightest where light hits. Keep true black only where almost no light reaches.
- **Different regions, different outline colors:** a red shirt gets a dark red outline, blue pants get dark blue.
- **Keep contrast.** Test outline colors against the actual background; if an outline disappears, darken it.
- **Interior lines:** use one shade darker than the region instead of the outline color, and fade them lighter where they end.
- **Skip outlines where they add nothing:** between hair and face if values already separate them, or along the bottom where a sprite meets the ground.
- **Small sprites:** at 16 px and below, selective outlining adds single-pixel noise. Prefer one solid dark outline.

## 9. Anti-aliasing and dithering

Both are polish tools, hand-placed and used sparingly. Over-using either makes pixel art look blurry or noisy.

**Manual anti-aliasing (AA)**

- Never use automatic AA: no blur, no soft brushes, no semi-transparent pixels.
- Place 1–2 pixels of an intermediate palette color at the inside corners where a curve's stair-step changes direction.
- AA pixels must be existing palette colors with values between the two colors they bridge.
- Never AA the outer edge of a sprite against transparency; it creates a halo on any other background.
- Skip AA on small sprites (16 px and below) where readability matters more than smoothness.
- Done when: it looks smooth from a distance but you can still count pixels up close.

**Dithering**

- Dithering alternates two colors in a pattern so the eye blends them, faking an in-between tone or a texture.
- The standard pattern is a 50% checkerboard. Use it in a narrow transition zone between two color areas, not across whole surfaces.
- Good uses: soft gradients in skies, rough textures (stone, sand, fur), a deliberately retro look.
- Don't dither small sprites, smooth surfaces, or several neighboring areas with different patterns — they compete and read as noise.
- Check dithering at actual viewing size; patterns that look nice zoomed in often turn into fuzz at 1x.

## 10. Rendering pixel art in an artifact

Store the art as a character grid plus a palette, draw one square per pixel at an integer scale, and switch off every kind of smoothing. Browsers blur scaled images by default, which destroys pixel art.

**Data format** — author the art as rows of characters, one character per pixel, all rows the same width:

```js
const PALETTE = { '.': null, 'o': '#1b1f2e', 'g': '#2d6a4f', 'G': '#52a447', 'h': '#a8d45a' };
const SPRITE = [
  '....oooo....',
  '..ooGGGGoo..',
  '.oGGhhGGGGo.',
  // ... every row exactly the same length
];
```

This keeps every pixel explicit, makes the palette enforceable, and lets you review the sprite visually in the code. Check that every row has equal length and every character exists in PALETTE.

**Canvas (preferred for anything large or animated)**

```js
const SCALE = 8; // integer only
canvas.width = SPRITE[0].length * SCALE;
canvas.height = SPRITE.length * SCALE;
ctx.imageSmoothingEnabled = false;
SPRITE.forEach((row, y) => [...row].forEach((c, x) => {
  if (!PALETTE[c]) return;
  ctx.fillStyle = PALETTE[c];
  ctx.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
}));
```

CSS on the canvas: `image-rendering: pixelated;` (with `crisp-edges` as a fallback). Set `imageSmoothingEnabled = false` again after any resize.

**SVG (good for static sprites)** — set `viewBox` to the grid size, one `<rect width="1" height="1">` per pixel (merge horizontal runs of the same color into wider rects), and put `shape-rendering="crispEdges"` on the root `<svg>` so seams don't appear between rects.

**Scaling rules**

- Scale only by whole numbers (2x, 3x, 4x, 8x). Non-integer scales make some pixels wider than others.
- Draw at integer coordinates; fractional positions blur. Floor any animated position.
- For responsive layouts, compute the largest integer scale that fits, `Math.floor(Math.min(availW / w, availH / h))`.
- Never use CSS transforms, filters, blur, shadows or gradients on the art itself.
- Exports: PNG only, never JPG — compression ruins the flat colors.

**Presentation**

- Show the art large (6x–12x) and optionally at 1x beside it, so readability is visible.
- Use a flat, contrasting background color; a subtle checkerboard is fine for showing transparency.
- Animation: 2–4 frames for idles, 4–8 for walks, swapped at roughly 6–12 frames per second. Fewer, well-chosen frames read better than many.

## 11. Self-review checklist before delivering

Run every item; if any fails, fix it and run the list again. A piece that passes all of these will look intentional rather than generated.

- [ ] The subject is identifiable as a flat silhouette and at 1x size.
- [ ] Every color comes from the defined palette; the count fits the canvas size.
- [ ] Every ramp is hue-shifted (cool shadows, warm highlights) with clear value steps.
- [ ] Neighboring regions differ in value, not just hue.
- [ ] One light direction is obvious and consistent everywhere, including the cast shadow.
- [ ] No pillow shading: shading follows form, not the outline.
- [ ] No banding: color runs don't line up in parallel stripes.
- [ ] All lines use consistent stair-steps; curves rise or fall steadily.
- [ ] No doubles, no orphan pixels, no accidental 2 px outline sections.
- [ ] Shading is grouped into clusters; no stray single-pixel noise.
- [ ] AA and dithering are hand-placed, sparse, and absent on small sprites and outer edges.
- [ ] Rendering is crisp: integer scale, integer positions, smoothing off, no blur or transparency on pixels.
- [ ] Grid data is valid: equal row lengths, every character mapped to a color.
- [ ] Small details that don't change the read have been removed.

## Sources

- [Pixel Joint — The Pixel Art Tutorial](https://pixeljoint.com/forum/forum_posts.asp?TID=11299)
- [Wayline — Clean lines and shapes](https://www.wayline.io/learn/pixel-art/1)
- [Wayline — Making a game-ready sprite](https://www.wayline.io/learn/pixel-art/4)
- [Wayline — Color & Palettes for Pixel Art](https://www.wayline.io/learn/color-palettes)
- [Lospec — Pixel Art Outlines Tutorial](https://lospec.com/articles/pixel-art-outlines)
- [Lospec — Pixel Art Outlines Part 2: Using Color](https://lospec.com/articles/pixel-art-outlines-part-2-using-color)
- [Slynyrd — Pixelblog 1: Color Palettes](https://www.slynyrd.com/blog/2018/1/10/pixelblog-1-color-palettes)
- [Pixel Parmesan — Anti-Aliasing Fundamentals for Pixel Artists](https://pixelparmesan.com/anti-aliasing-fundamentals-for-pixel-artists)
- [OpenGameArt — Chapter 2: Lines and Curves](https://opengameart.org/node/5490)
- [Pixnote — Shading & Lighting](https://pixnote.net/en/learn/shading)
- [Pixnote — Pixel Art Glossary](https://pixnote.net/en/learn/glossary)
- [Divoom — Fix jaggies without making pixel art blurry](https://divoom.com/blogs/setup-ideas/fix-jaggies-pixel-art-without-blur)
- [Divoom — Pixel art dithering: when to use it](https://divoom.com/blogs/setup-ideas/pixel-art-dithering-when-to-use-and-stop)
- [MDN — Crisp pixel art look with image-rendering](https://developer.mozilla.org/en-US/docs/Games/Techniques/Crisp_pixel_art_look)
- [MDN — shape-rendering](https://developer.mozilla.org/docs/Web/CSS/shape-rendering)
- [Cinevva — Pixel art rendering in web games](https://app.cinevva.com/tutorials/pixel-art-rendering.html)
