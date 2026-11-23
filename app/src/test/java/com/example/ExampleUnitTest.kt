package com.example

import com.example.data.device.CuratedKnowledgeBank
import com.example.data.network.NetworkRepository
import com.example.domain.model.GlassMaterialConfig
import com.example.domain.model.GlassPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun crystalPresetDefaultsToNinetyNinePercentTransparency() {
        val crystal = GlassMaterialConfig.fromPreset(GlassPreset.CRYSTAL)
        assertEquals(0.99f, crystal.transparency, 0.001f)
        assertTrue(crystal.refractionStrength > 0.5f)
    }

    @Test
    fun moonPhaseCalculationReturnsValidIllumination() {
        val (name, illumination) = NetworkRepository.calculateMoonPhase(1700000000000L)
        assertTrue(name.isNotBlank())
        assertTrue(illumination in 0..100)
    }

    @Test
    fun knowledgeBankWrapsSafely() {
        val entry = CuratedKnowledgeBank.pick(25)
        assertTrue(entry.text.isNotBlank())
        assertTrue(entry.attribution.isNotBlank())
    }
}
