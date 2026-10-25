# Design iteration 01

Implemented separate portrait-story geometry: full-width image above text for PHOTO; inset magazine image for EDITORIAL; headline first with image below for BREAKING. Images and text never overlap. Fit and optional manual crop are preserved.

Removed duplicated statistic ornament, added a masthead grid and breaking badge, anchored short text higher in the frame, and varied footer rules by style. Hidden footer content no longer leaves a decorative rule. Article pages use the full content region.

Added regression checks for photo/text separation and distinct story layouts. Native render previews are committed by CI under docs/design/previews/<source SHA>. Synthetic photo samples are labelled; they are not real news photos. This remains a design review build, not an approved final design.
