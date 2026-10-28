# Studio 1.8.0 — editorial hierarchy and adaptive composition

Implements all six issues from the review of 1.7.1:

1. Header: default wordmark width 104 rather than 132 logical units; earlier header origin and shorter masthead. No header folio. A small footer page count appears only for multiple output pages.
2. Headlines: tighter measured leading (0.92), balanced RTL wrapping and same-length nonbreaking spaces for units and short Persian function-word phrases. Auto fitting budgets for a complete short summary; explicit manual sizes remain unchanged. Phrase grouping is conservative, not semantic rewriting.
3. Body: Cairo and Kufi use their existing variable fonts at weight 500. Body leading is 1.06; body contrast is increased. Other selected font families remain available.
4. Composition: text-only automatic covers center their actual measured content. Photo covers budget text before allocating image height; a short stacked photo-and-copy group centers together, with taller allocations for portrait photos. Sentence-aware splitting prefers a complete sentence when it fits; oversized sentences still paginate without deleting text. If a balanced line reflows when it becomes a page, earlier word boundaries are measured before declaring the space insufficient. A short square-card summary is covered by a specific regression test.
5. Decoration: remove header/photo/footer slashes, filled category badges, repeated red separators and the quote shadow card. Keep the adaptive brand accents, a restrained category cue, and the content's dominant feature.
6. Specialized covers: statistical values are extracted once from a short source title into a prominent feature, retaining every other word in the display heading and the original title in captions. The numerical phrase includes its units and sign. No numeric feature is fabricated if the title lacks one. Quote bylines are separately measured, drawn and paginated. Breaking news prioritizes the headline above optional photography on a red field.

Native draws share exact measurements with pagination. Attribution reading time is included in video page timing. Source photos are contained without altering their colors or adding blurred duplicate backdrops; explicit crop mode remains user controlled.

Review evidence includes before/after against 1.7.1 using identical synthetic content, distinct sample content for specialized templates, and hash-pinned NASA photographs in landscape and portrait orientations across all four ratios. Photo credits and provenance live in `app/src/test/resources/photos/README.md`. Real-device video and Instagram delivery remain outside this environment's validation.
