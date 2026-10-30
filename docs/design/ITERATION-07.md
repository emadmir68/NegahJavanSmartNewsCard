# Complete-copy fitting — 1.8.1

The reported quote-style story contained only the Binance paragraph, while the pasted input had four paragraphs. The preparation pipeline selected one sentence with `StudioText.summary` and stored the complete text only in `article`. This was a content-selection defect as well as a layout limitation: the renderer could not display text it never received.

Preparation now preserves the complete body on the card. Summarization is an explicit action, with a separate full-text restore action. A narrow migration restores only legacy text matching the original raw input, original title and exact generated summary. New saves carry `cardTextVersion=1`, so intentional summaries remain unchanged.

Automatic layout measures the complete headline, body and quote attribution first. It searches for the least compression that fits, jointly adjusting headline size, body size, body leading (1.06 down to 1.0) and inter-block spacing (12 down to 6 logical units). Body reduction stops at the larger of 11 units or 80% of the chosen maximum, without increasing an explicitly smaller maximum. If even that cannot fit, ordinary pagination retains the chosen body size. The resolved typography belongs to the render plan and is shared by measurement, drawing, preview and export; saved user settings remain unchanged.

Regression coverage includes the exact four-paragraph report, all six story templates, fonts and aspect ratios, video-overlay geometry, manually chosen sizes, long-copy preservation, legacy migration and carousel duplication. Native outputs `smart-fit-user-text.png` and `smart-fit-before-after.png` document the reported case. The supplied financial claims are unverified layout fixtures, not reporting or factual assertions.
