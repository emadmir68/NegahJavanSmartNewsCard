# Studio 1.7.1 — a wordmark integrated into the card

Version 1.7.0 placed the supplied image on an opaque white support rectangle (or in its original circular frame). The user asked for a logo that belongs to the card design instead of placing the same photo on it.

The renderer now uses two independent Android vector paths derived from the principal wordmark's outlines: lettering and accent strokes. There is no bitmap draw or background plate in the brand component. Letter counters use even-odd fill and remain transparent. The reference image stays unchanged in the source tree for provenance.

On the black masthead and dark cover, the lettering is near white with red accents. Light templates use graphite with red. The red bulletin uses a white wordmark with graphite accents so the strokes remain visible. These colors also apply to video overlays. The compact wordmark's wider aspect ratio reduces the header's vertical footprint.

The existing `fullLogo` persisted flag now selects a complete wordmark lockup with a native typeset «پایگاه خبری» tagline, instead of the original circle. The settings labels reflect this change. Other saved fields retain their meanings. The renderer version invalidates cached export references without deleting Gallery files.

Native regression coverage verifies transparency inside both letter counters, transparent outside space, actual ink/accent pixels across all six palettes, and the absence of the white support area in the final card. Existing tests continue to cover all headline fonts, styles, ratios, pagination and video-overlay positions. Before/after previews compare 1.7.0 and 1.7.1 with the same synthetic photo and news text.

The native renders and APK are stored in this branch by the build workflow. These checks do not constitute real-device video or Instagram validation.
