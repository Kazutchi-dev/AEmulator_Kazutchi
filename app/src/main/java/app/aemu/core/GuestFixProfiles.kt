package app.aemu.core

/**
 * A fix is selected against the imported guest, not the phone running the emulator.
 *
 * Keep selectors narrow: a release-specific workaround must never leak into another
 * Android release or engine just because the directory layout happens to look similar.
 */
data class GuestFixTarget(
    val image: GuestImage,
    val properties: Map<String, String>,
) {
    val api: Int get() = image.api
    val release: String get() = image.release
    val brand: String get() = image.brand
    val model: String get() = image.model
    val skin: String get() = image.skin
    val engine: Engine get() = image.engine
    val abi: String get() = image.abi
    val runtime: String get() = image.runtime
    val gpuEnabled: Boolean get() = image.settings.gpu

    fun property(name: String): String = properties[name].orEmpty()
}

enum class GuestFixPhase { IMPORT, BOOT }

data class GuestFixRule(
    val id: String,
    val phase: GuestFixPhase = GuestFixPhase.BOOT,
    val description: String,
    val api: IntRange? = null,
    val releases: Regex? = null,
    val brands: Regex? = null,
    val models: Regex? = null,
    val skins: Regex? = null,
    val engines: Set<Engine>? = null,
    val abis: Set<String>? = null,
    val runtimes: Set<String>? = null,
    /** Regex selectors for guest build.prop keys such as ro.build.fingerprint or ro.product.device. */
    val properties: Map<String, Regex>? = null,
    val gpuEnabled: Boolean? = null,
    val apply: TreeFixer.() -> Unit,
) {
    fun matches(target: GuestFixTarget): Boolean =
        (api == null || target.api in api) &&
        (releases == null || releases.containsMatchIn(target.release)) &&
        (brands == null || brands.containsMatchIn(target.brand)) &&
        (models == null || models.containsMatchIn(target.model)) &&
        (skins == null || skins.containsMatchIn(target.skin)) &&
        (engines == null || target.engine in engines) &&
        (abis == null || target.abi in abis) &&
        (runtimes == null || target.runtime in runtimes) &&
        (properties == null || properties.all { (key, pattern) -> pattern.containsMatchIn(target.property(key)) }) &&
        (gpuEnabled == null || target.gpuEnabled == gpuEnabled)
}

/**
 * Central registry for guest-specific workarounds.
 *
 * Add a new rule here when a firmware/API/engine combination needs a dedicated fix.
 * Common fixes stay in TreeFixer.fixup(); rules below are intentionally scoped.
 */
object GuestFixProfiles {
    val rules: List<GuestFixRule> = listOf(
        GuestFixRule(
            id = "miui-theme-xml-comments",
            description = "Repair malformed MIUI theme compatibility XML",
            skins = Regex("(?i)miui"),
            apply = { fixThemeXml() },
        ),
        GuestFixRule(
            id = "miui-whetstone-deadlock",
            description = "Disable the MIUI Whetstone service that deadlocks ActivityManager",
            skins = Regex("(?i)miui"),
            apply = { parkWhetstone() },
        ),
        GuestFixRule(
            id = "mtk-audio-hal-compat",
            description = "Replace the MediaTek primary audio HAL with the emulator-compatible HAL",
            api = 17..Int.MAX_VALUE,
            engines = setOf(Engine.KK),
            apply = { swapMtkAudioHal() },
        ),
    )
}
