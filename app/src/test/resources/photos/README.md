# Real photograph review fixtures

These are unchanged NASA photographs, used only in tests and archived design previews. They are not bundled as app content. Their cards are labeled as design samples and archival imagery, not current news.

- `webb-portrait.jpg`: [Webb Space Telescope mirror in the clean room](https://science.nasa.gov/image-detail/webb_4/). Credit: NASA/Chris Gunn. 2557 × 3833.
- `webb-landscape.png`: original landscape background, “Webb mirror in Goddard cleanroom with Webb project personnel,” from [NASA's downloadable backgrounds](https://science.nasa.gov/mission/webb/downloadable-backgrounds/). Credit: NASA. 1920 × 1080. The official downloaded file is kept unchanged.

`tools/fetch_review_photos.py` records original download URLs and SHA-256 checksums. The build workflow preserves the verified bytes in this repository. Native Android rendering scales them into their card frames without cropping, except when a project explicitly enables crop.

NASA usage guidance: https://www.nasa.gov/nasa-brand-center/images-and-media/ . No NASA endorsement is implied.
