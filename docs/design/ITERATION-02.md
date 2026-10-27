# Studio 1.5.9 — measured editorial layouts

The previous review build still used large default type, narrow headline columns on square portrait photos, repeated photo space on continuation pages, and an unavoidable closing slide.

- New projects use a 21-unit headline and 12-unit body; existing projects retain their saved sizes. Manual sizes are never reduced. Auto fit can reach 16 units.
- Landscape source images keep a full-width headline in portrait/square output. Tall portrait sources in contain mode get a generous separate column, avoiding a tiny subject surrounded by letterboxes; landscape output also uses two columns. Photo height responds to headline/body measurements and the source aspect ratio; contain/crop and manual positioning stay available.
- Continued titles and summaries use the full content area without repeating the cover photo. Multiline labels and quote ornaments are measured as part of pagination.
- Green/gold signature, ivory editorial, full-width photo and headline-first breaking layouts use a quieter masthead, muted summary and story inset for top/bottom branding.
- Closing copy is optional, editable, persisted and paginated without truncation.
- The editor preview has a bounded height; editing the source text no longer immediately destroys the edited headline/body. The prepare button still deliberately regenerates the suggestions.

Validation: native Android renders across every bundled font, style and output ratio; text bounds, long manual headlines/categories, continuation preservation and project round trips. CI saves screenshots and APK against the exact source SHA. Preview illustrations and headlines are synthetic demonstration content, not verified reporting. Real-device video/Instagram checks remain outside this environment.

Visual review also increased the non-story footer inset so the URL clears the signature border. The statistic style highlights the first number in-place rather than duplicating it as an ornament.
