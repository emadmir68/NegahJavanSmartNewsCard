# Aligned Persian paragraphs — 1.8.2

The complete-copy fit fixed omission but still used headline-style balanced wrapping for body text. The resulting ragged lines and inline emoji markers made wrapped paragraphs appear to drift. Body text now uses Android high-quality breaking and inter-word justification. Final paragraph lines retain RTL normal alignment; headings retain their own wrapping.

Leading bullet markers are displayed in a constant RTL gutter through a paragraph margin span. The first and subsequent lines receive the same margin. A zero-width replacement span hides only the displayed source prefix; it never removes source characters or changes pagination indices. The gutter is selected from the whole project's body content so continuation pages retain the same text column even without a new marker on their first line. Markers inside prose and negative numbers are not treated as bullets. A measured paragraph gap separates consecutive paragraphs.

Measurement, full-copy fitting, pagination and rendering use the same styled body layout. Numeric units remain non-breaking; short prose function words no longer create artificial unbreakable groups. The original source, bullets and caption are unchanged.

Regression checks cover both text edges, unstretched last lines, marker positions counted from native rendered pixels, continuation alignment and source preservation. The exact user report remains a one-story regression across all six templates.

Platform references: https://developer.android.com/reference/android/text/StaticLayout.Builder#setJustificationMode(int) and https://developer.android.com/reference/android/text/style/LeadingMarginSpan .
