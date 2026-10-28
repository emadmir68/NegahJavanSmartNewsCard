# Brand and typography source assets

The user supplied the original نگاه جوان logo on 2026-10-28. The PNG is copied without editing to `app/src/main/res/drawable-nodpi/negahjavan_logo.png`.

Original SHA-256: `1eb0f41ca1b62953566f78999e855587177208cae31a64924ac5de48ceb1e552`.

Since 1.7.1, the native renderer uses a transparent vector wordmark instead of drawing this bitmap. The principal lettering and two red accents were traced from source bounds (140, 606)–(1404, 1000), excluding the circle, white background, tagline and its horizontal rules. The letter silhouettes are retained; their colors adapt to the card palette. Enclosed counters remain transparent through even-odd fills. The optional tagline is independently typeset in Estedad Regular.

The two Android layers are `res/drawable/negahjavan_mark_ink.xml` and `negahjavan_mark_accent.xml`. A reusable transparent SVG is at `docs/design/assets/negahjavan-wordmark.svg`. `tools/trace_brand_mark.py` reproduces these vector assets from the unchanged reference PNG with Python, Pillow and NumPy. It emits no edited raster images. Small pixel contours are excluded and the outlines simplified at less than one source pixel tolerance. Runtime colors and lockup geometry are in `StudioBrandMark`.

Estedad Black (900) and Regular (400) are unmodified upstream TTF files from [Amin Abedi's Estedad repository](https://github.com/aminabedi68/Estedad), commit `0dbe689787b8c2ea302373cb601d0f352f9f98e5`. Black source Git blob: `2efa43f5c681a303a5bb414e8b75d523ba7fe07d`; Regular: `450fe89141fa068d1111f86e4d8d5d644e297bdd`.

The complete SIL OFL license and copyright notice are in `docs/licenses/Estedad-OFL.txt`, the APK's `assets/licenses/Estedad-OFL.txt`, and the install ZIP. Since 1.8.0 the existing variable Cairo font is the default independent body face at weight 500; variable Kufi also uses 500 for body text. Other selected body families retain their bundled regular face.
