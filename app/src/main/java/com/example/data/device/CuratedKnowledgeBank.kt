package com.example.data.device

enum class KnowledgeCategory(val label: String) {
    QUOTE("Reflection"),
    SCIENCE("Science"),
    TECHNOLOGY("Technology")
}

data class KnowledgeEntry(
    val category: KnowledgeCategory,
    val text: String,
    val attribution: String
)

object CuratedKnowledgeBank {
    val entries: List<KnowledgeEntry> = listOf(
        KnowledgeEntry(
            KnowledgeCategory.QUOTE,
            "Simplicity is about subtracting the obvious and adding the meaningful.",
            "John Maeda"
        ),
        KnowledgeEntry(
            KnowledgeCategory.QUOTE,
            "Look at everything as though you were seeing it either for the first or last time.",
            "Betty Smith"
        ),
        KnowledgeEntry(
            KnowledgeCategory.QUOTE,
            "Calmness is the cradle of power.",
            "Josiah Gilbert Holland"
        ),
        KnowledgeEntry(
            KnowledgeCategory.QUOTE,
            "Details are not the details. They make the design.",
            "Charles Eames"
        ),
        KnowledgeEntry(
            KnowledgeCategory.SCIENCE,
            "A photon takes roughly 8 minutes and 20 seconds to travel from the Sun to Earth, yet can spend over 100,000 years scattering inside the Sun's radiative zone first.",
            "Solar Astrophysics"
        ),
        KnowledgeEntry(
            KnowledgeCategory.SCIENCE,
            "Liquid glass behaves optically as an amorphous solid: its molecules lack long-range crystalline order, allowing light to refract smoothly without grain-boundary scattering.",
            "Condensed Matter Physics"
        ),
        KnowledgeEntry(
            KnowledgeCategory.SCIENCE,
            "Ocean tides slow Earth's rotation by roughly 1.7 milliseconds per century while pushing the Moon outward by about 3.8 centimeters every year.",
            "Lunar Laser Ranging"
        ),
        KnowledgeEntry(
            KnowledgeCategory.SCIENCE,
            "Blue light scatters nearly ten times more strongly than red light in Earth's atmosphere due to Rayleigh scattering, giving twilight skies their amber horizon.",
            "Atmospheric Optics"
        ),
        KnowledgeEntry(
            KnowledgeCategory.TECHNOLOGY,
            "The MQTT protocol header can be as small as 2 bytes, allowing battery-powered ESP32 microcontrollers to publish telemetry over weak Wi-Fi links.",
            "Embedded Telemetry"
        ),
        KnowledgeEntry(
            KnowledgeCategory.TECHNOLOGY,
            "AGSL (Android Graphics Shading Language) executes fragment shaders directly on the device GPU, enabling real-time optical refraction and lens dispersion.",
            "Android Graphics"
        ),
        KnowledgeEntry(
            KnowledgeCategory.TECHNOLOGY,
            "Voyager 1's flight computers still execute telemetry commands with under 70 kilobytes of magnetic-core and CMOS memory across 24 billion kilometers.",
            "Deep Space Network"
        ),
        KnowledgeEntry(
            KnowledgeCategory.TECHNOLOGY,
            "Fast Fourier Transforms (FFT) reduce discrete spectral decomposition from O(N^2) to O(N log N), making 60 fps real-time audio spectrum analysis possible on mobile DSPs.",
            "Digital Signal Processing"
        )
    )

    fun pick(index: Int): KnowledgeEntry = entries[index.mod(entries.size)]
}
