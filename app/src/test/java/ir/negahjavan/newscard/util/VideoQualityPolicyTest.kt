package ir.negahjavan.newscard.util

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoQualityPolicyTest {
    @Test
    fun fullHdUsesHighAdaptiveBitrate() {
        assertEquals(
            16_000_000,
            VideoPostExporter.targetVideoBitrate(null, VideoExportPreset.FULL_HD)
        )
        assertEquals(
            12_000_000,
            VideoPostExporter.targetVideoBitrate(6_000_000, VideoExportPreset.FULL_HD)
        )
        assertEquals(
            20_000_000,
            VideoPostExporter.targetVideoBitrate(16_000_000, VideoExportPreset.FULL_HD)
        )
        assertEquals(
            24_000_000,
            VideoPostExporter.targetVideoBitrate(40_000_000, VideoExportPreset.FULL_HD)
        )
    }

    @Test
    fun fastPresetStaysWithinPracticalSocialBitrate() {
        assertEquals(
            6_000_000,
            VideoPostExporter.targetVideoBitrate(null, VideoExportPreset.FAST)
        )
        assertEquals(
            4_500_000,
            VideoPostExporter.targetVideoBitrate(4_000_000, VideoExportPreset.FAST)
        )
        assertEquals(
            7_500_000,
            VideoPostExporter.targetVideoBitrate(10_000_000, VideoExportPreset.FAST)
        )
        assertEquals(
            8_000_000,
            VideoPostExporter.targetVideoBitrate(30_000_000, VideoExportPreset.FAST)
        )
    }
}
