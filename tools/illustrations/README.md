# Harvana illustrations

The pictures on the group contract pages (`app/src/main/res/drawable-nodpi/contract_*.webp`) are drawn in code so new ones match the old ones.

- `gen.py`: the shared parts. People with several skin tones, hair styles and poses; HARV coins; jars; plants; the soft backdrop; a light paper grain.
- `scenes.py`: one scene per contract page. Run it to write the SVGs.
- `sheet.js`: renders each SVG to PNG with Playwright. Then crop and save as 600×400 WebP (see the commit that added them).

To add a picture: write a scene in `scenes.py`, render it, copy the WebP into `drawable-nodpi`, and point the screen at it. Colours come from the app palette: ink `#053D3A`, teal `#0B6B61`, gold `#F4B63D`, coral `#F27C5C`, cream `#FFF4E2`.
