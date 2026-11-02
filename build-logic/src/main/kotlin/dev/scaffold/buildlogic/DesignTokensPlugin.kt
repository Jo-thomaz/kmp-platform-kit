package dev.scaffold.buildlogic

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

internal data class TypographyValue(
    val fontSizeSp: Int,
    val lineHeightSp: Int,
    val fontWeight: Int,
)

internal data class DualModeColorRef(
    val lightPrimitiveKey: String,
    val darkPrimitiveKey: String,
)

internal data class ParsedTokenModel(
    val primitiveColors: Map<String, String>,
    val primitiveSpacingDp: Map<String, Int>,
    val primitiveRadiusDp: Map<String, Int>,
    val primitiveElevationDp: Map<String, Int>,
    val primitiveTypography: Map<String, TypographyValue>,
    val primitiveMotionMs: Map<String, Int>,
    val semanticColors: Map<String, DualModeColorRef>,
    val semanticSpacingRefs: Map<String, String>,
    val semanticRadiusRefs: Map<String, String>,
    val semanticElevationRefs: Map<String, String>,
    val semanticTypographyRefs: Map<String, String>,
    val semanticMotionRefs: Map<String, String>,
)

internal object DesignTokensGenerator {
    @Suppress("UNCHECKED_CAST")
    fun parse(jsonText: String): ParsedTokenModel {
        val root = JsonSlurper().parseText(jsonText) as Map<String, Any?>
        val primitive = root["primitive"] as Map<String, Any?>
        val semantic = root["semantic"] as Map<String, Any?>

        fun extractScalarMap(section: Map<String, Any?>, group: String): Map<String, String> {
            val entries = section[group] as Map<String, Map<String, Any?>>
            return entries.mapValues { (_, v) -> v["\$value"].toString() }
        }

        fun parseDp(raw: String): Int = raw.removeSuffix("dp").trim().toInt()
        fun parseSp(raw: String): Int = raw.removeSuffix("sp").trim().toInt()
        fun parseMs(raw: String): Int = raw.removeSuffix("ms").trim().toInt()
        fun refKey(ref: String, expectedPrefix: String): String {
            val clean = ref.removePrefix("{").removeSuffix("}")
            require(clean.startsWith("$expectedPrefix.")) {
                "Expected token reference starting with '$expectedPrefix.', got '$ref'"
            }
            return clean.removePrefix("$expectedPrefix.")
        }

        val primitiveColors = extractScalarMap(primitive, "color")
        val primitiveSpacingDp = extractScalarMap(primitive, "spacing").mapValues { parseDp(it.value) }
        val primitiveRadiusDp = extractScalarMap(primitive, "radius").mapValues { parseDp(it.value) }
        val primitiveElevationDp = extractScalarMap(primitive, "elevation").mapValues { parseDp(it.value) }
        val primitiveMotionMs = extractScalarMap(primitive, "motion").mapValues { parseMs(it.value) }

        val typoRaw = primitive["typography"] as Map<String, Map<String, Any?>>
        val primitiveTypography = typoRaw.mapValues { (_, token) ->
            val valueMap = token["\$value"] as Map<String, Any?>
            TypographyValue(
                fontSizeSp = parseSp(valueMap["fontSize"].toString()),
                lineHeightSp = parseSp(valueMap["lineHeight"].toString()),
                fontWeight = (valueMap["fontWeight"] as Number).toInt(),
            )
        }

        val semColorsRaw = semantic["color"] as Map<String, Map<String, Any?>>
        val semanticColors = semColorsRaw.mapValues { (_, token) ->
            val modes = token["\$value"] as Map<String, Any?>
            val lightKey = refKey(modes["light"].toString(), "primitive.color")
            val darkKey = refKey(modes["dark"].toString(), "primitive.color")
            require(primitiveColors.containsKey(lightKey)) { "Unknown primitive color '$lightKey'" }
            require(primitiveColors.containsKey(darkKey)) { "Unknown primitive color '$darkKey'" }
            DualModeColorRef(lightPrimitiveKey = lightKey, darkPrimitiveKey = darkKey)
        }

        val semanticSpacingRefs = extractScalarMap(semantic, "spacing")
            .mapValues { refKey(it.value, "primitive.spacing") }
        val semanticRadiusRefs = extractScalarMap(semantic, "radius")
            .mapValues { refKey(it.value, "primitive.radius") }
        val semanticElevationRefs = extractScalarMap(semantic, "elevation")
            .mapValues { refKey(it.value, "primitive.elevation") }
        val semanticTypographyRefs = extractScalarMap(semantic, "typography")
            .mapValues { refKey(it.value, "primitive.typography") }
        val semanticMotionRefs = extractScalarMap(semantic, "motion")
            .mapValues { refKey(it.value, "primitive.motion") }

        return ParsedTokenModel(
            primitiveColors = primitiveColors,
            primitiveSpacingDp = primitiveSpacingDp,
            primitiveRadiusDp = primitiveRadiusDp,
            primitiveElevationDp = primitiveElevationDp,
            primitiveTypography = primitiveTypography,
            primitiveMotionMs = primitiveMotionMs,
            semanticColors = semanticColors,
            semanticSpacingRefs = semanticSpacingRefs,
            semanticRadiusRefs = semanticRadiusRefs,
            semanticElevationRefs = semanticElevationRefs,
            semanticTypographyRefs = semanticTypographyRefs,
            semanticMotionRefs = semanticMotionRefs,
        )
    }

    fun renderKotlin(packageId: String, model: ParsedTokenModel): String {
        val sb = StringBuilder()
        sb.appendLine("// AUTO-GENERATED FROM design-tokens/tokens.json — DO NOT EDIT BY HAND.")
        sb.appendLine("package $packageId.designsystem.tokens")
        sb.appendLine()
        sb.appendLine("import androidx.compose.runtime.Immutable")
        sb.appendLine("import androidx.compose.runtime.staticCompositionLocalOf")
        sb.appendLine("import androidx.compose.ui.graphics.Color")
        sb.appendLine("import androidx.compose.ui.text.TextStyle")
        sb.appendLine("import androidx.compose.ui.text.font.FontWeight")
        sb.appendLine("import androidx.compose.ui.unit.Dp")
        sb.appendLine("import androidx.compose.ui.unit.dp")
        sb.appendLine("import androidx.compose.ui.unit.sp")
        sb.appendLine()
        sb.appendLine("internal object PrimitiveTokens {")
        sb.appendLine("    object Colors {")
        for ((name, hex) in model.primitiveColors) {
            val cleanHex = hex.removePrefix("#").uppercase()
            sb.appendLine("        val $name = Color(0xFF$cleanHex)")
        }
        sb.appendLine("    }")
        sb.appendLine("    object Spacing {")
        for ((name, dpVal) in model.primitiveSpacingDp) {
            sb.appendLine("        val $name: Dp = ${dpVal}.dp")
        }
        sb.appendLine("    }")
        sb.appendLine("    object Radius {")
        for ((name, dpVal) in model.primitiveRadiusDp) {
            sb.appendLine("        val $name: Dp = ${dpVal}.dp")
        }
        sb.appendLine("    }")
        sb.appendLine("    object Elevation {")
        for ((name, dpVal) in model.primitiveElevationDp) {
            sb.appendLine("        val $name: Dp = ${dpVal}.dp")
        }
        sb.appendLine("    }")
        sb.appendLine("    object Typography {")
        for ((name, typo) in model.primitiveTypography) {
            sb.appendLine(
                "        val $name = TextStyle(fontSize = ${typo.fontSizeSp}.sp, lineHeight = ${typo.lineHeightSp}.sp, fontWeight = FontWeight(${typo.fontWeight}))",
            )
        }
        sb.appendLine("    }")
        sb.appendLine("    object Motion {")
        for ((name, ms) in model.primitiveMotionMs) {
            sb.appendLine("        const val ${name}Millis: Int = $ms")
        }
        sb.appendLine("    }")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("@Immutable")
        sb.appendLine("data class SemanticColorTokens(")
        for (name in model.semanticColors.keys) {
            sb.appendLine("    val $name: Color,")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("val LightSemanticColors = SemanticColorTokens(")
        for ((name, ref) in model.semanticColors) {
            sb.appendLine("    $name = PrimitiveTokens.Colors.${ref.lightPrimitiveKey},")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("val DarkSemanticColors = SemanticColorTokens(")
        for ((name, ref) in model.semanticColors) {
            sb.appendLine("    $name = PrimitiveTokens.Colors.${ref.darkPrimitiveKey},")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("@Immutable")
        sb.appendLine("data class SemanticSpacingTokens(")
        for ((name, ref) in model.semanticSpacingRefs) {
            sb.appendLine("    val $name: Dp = PrimitiveTokens.Spacing.$ref,")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("@Immutable")
        sb.appendLine("data class SemanticRadiusTokens(")
        for ((name, ref) in model.semanticRadiusRefs) {
            sb.appendLine("    val $name: Dp = PrimitiveTokens.Radius.$ref,")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("@Immutable")
        sb.appendLine("data class SemanticElevationTokens(")
        for ((name, ref) in model.semanticElevationRefs) {
            sb.appendLine("    val $name: Dp = PrimitiveTokens.Elevation.$ref,")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("@Immutable")
        sb.appendLine("data class SemanticTypographyTokens(")
        for ((name, ref) in model.semanticTypographyRefs) {
            sb.appendLine("    val $name: TextStyle = PrimitiveTokens.Typography.$ref,")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("@Immutable")
        sb.appendLine("data class SemanticMotionTokens(")
        for ((name, ref) in model.semanticMotionRefs) {
            sb.appendLine("    val ${name}Millis: Int = PrimitiveTokens.Motion.${ref}Millis,")
        }
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("@Immutable")
        sb.appendLine("data class ScaffoldTokens(")
        sb.appendLine("    val colors: SemanticColorTokens,")
        sb.appendLine("    val spacing: SemanticSpacingTokens = SemanticSpacingTokens(),")
        sb.appendLine("    val radius: SemanticRadiusTokens = SemanticRadiusTokens(),")
        sb.appendLine("    val elevation: SemanticElevationTokens = SemanticElevationTokens(),")
        sb.appendLine("    val typography: SemanticTypographyTokens = SemanticTypographyTokens(),")
        sb.appendLine("    val motion: SemanticMotionTokens = SemanticMotionTokens(),")
        sb.appendLine(")")
        sb.appendLine()
        sb.appendLine("val LocalScaffoldTokens = staticCompositionLocalOf { ScaffoldTokens(colors = LightSemanticColors) }")
        return sb.toString().replace("\r\n", "\n")
    }

    fun renderSwift(model: ParsedTokenModel): String {
        fun hexToRgbDoubles(hex: String): Triple<String, String, String> {
            val clean = hex.removePrefix("#")
            val r = clean.substring(0, 2).toInt(16) / 255.0
            val g = clean.substring(2, 4).toInt(16) / 255.0
            val b = clean.substring(4, 6).toInt(16) / 255.0
            return Triple(
                String.format(java.util.Locale.US, "%.4f", r),
                String.format(java.util.Locale.US, "%.4f", g),
                String.format(java.util.Locale.US, "%.4f", b),
            )
        }

        fun swiftFontWeight(weight: Int): String = when {
            weight >= 700 -> ".bold"
            weight >= 600 -> ".semibold"
            weight >= 500 -> ".medium"
            else -> ".regular"
        }

        val sb = StringBuilder()
        sb.appendLine("// AUTO-GENERATED FROM design-tokens/tokens.json — DO NOT EDIT BY HAND.")
        sb.appendLine("import SwiftUI")
        sb.appendLine()
        sb.appendLine("enum PrimitiveTokens {")
        sb.appendLine("    enum Colors {")
        for ((name, hex) in model.primitiveColors) {
            val (r, g, b) = hexToRgbDoubles(hex)
            sb.appendLine("        static let $name = Color(.sRGB, red: $r, green: $g, blue: $b, opacity: 1.0)")
        }
        sb.appendLine("    }")
        sb.appendLine("    enum Spacing {")
        for ((name, dpVal) in model.primitiveSpacingDp) {
            sb.appendLine("        static let $name: CGFloat = $dpVal")
        }
        sb.appendLine("    }")
        sb.appendLine("    enum Radius {")
        for ((name, dpVal) in model.primitiveRadiusDp) {
            sb.appendLine("        static let $name: CGFloat = $dpVal")
        }
        sb.appendLine("    }")
        sb.appendLine("    enum Elevation {")
        for ((name, dpVal) in model.primitiveElevationDp) {
            sb.appendLine("        static let $name: CGFloat = $dpVal")
        }
        sb.appendLine("    }")
        sb.appendLine("    enum Typography {")
        for ((name, typo) in model.primitiveTypography) {
            sb.appendLine(
                "        static let $name = Font.system(size: ${typo.fontSizeSp}, weight: ${swiftFontWeight(typo.fontWeight)})",
            )
        }
        sb.appendLine("    }")
        sb.appendLine("    enum Motion {")
        for ((name, ms) in model.primitiveMotionMs) {
            val seconds = String.format(java.util.Locale.US, "%.3f", ms / 1000.0)
            sb.appendLine("        static let $name: Double = $seconds")
        }
        sb.appendLine("    }")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("public struct SemanticColorTokens: Sendable {")
        for (name in model.semanticColors.keys) {
            sb.appendLine("    public let $name: Color")
        }
        sb.appendLine()
        sb.appendLine("    public static let light = SemanticColorTokens(")
        model.semanticColors.entries.forEachIndexed { index, (name, ref) ->
            val comma = if (index < model.semanticColors.size - 1) "," else ""
            sb.appendLine("        $name: PrimitiveTokens.Colors.${ref.lightPrimitiveKey}$comma")
        }
        sb.appendLine("    )")
        sb.appendLine()
        sb.appendLine("    public static let dark = SemanticColorTokens(")
        model.semanticColors.entries.forEachIndexed { index, (name, ref) ->
            val comma = if (index < model.semanticColors.size - 1) "," else ""
            sb.appendLine("        $name: PrimitiveTokens.Colors.${ref.darkPrimitiveKey}$comma")
        }
        sb.appendLine("    )")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("public struct SemanticSpacingTokens: Sendable {")
        for ((name, ref) in model.semanticSpacingRefs) {
            sb.appendLine("    public let $name: CGFloat = PrimitiveTokens.Spacing.$ref")
        }
        sb.appendLine("    public init() {}")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("public struct SemanticRadiusTokens: Sendable {")
        for ((name, ref) in model.semanticRadiusRefs) {
            sb.appendLine("    public let $name: CGFloat = PrimitiveTokens.Radius.$ref")
        }
        sb.appendLine("    public init() {}")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("public struct SemanticElevationTokens: Sendable {")
        for ((name, ref) in model.semanticElevationRefs) {
            sb.appendLine("    public let $name: CGFloat = PrimitiveTokens.Elevation.$ref")
        }
        sb.appendLine("    public init() {}")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("public struct SemanticTypographyTokens: Sendable {")
        for ((name, ref) in model.semanticTypographyRefs) {
            sb.appendLine("    public let $name: Font = PrimitiveTokens.Typography.$ref")
        }
        sb.appendLine("    public init() {}")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("public struct SemanticMotionTokens: Sendable {")
        for ((name, ref) in model.semanticMotionRefs) {
            sb.appendLine("    public let $name: Double = PrimitiveTokens.Motion.$ref")
        }
        sb.appendLine("    public init() {}")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("public struct ScaffoldTokens: Sendable {")
        sb.appendLine("    public let colors: SemanticColorTokens")
        sb.appendLine("    public let spacing = SemanticSpacingTokens()")
        sb.appendLine("    public let radius = SemanticRadiusTokens()")
        sb.appendLine("    public let elevation = SemanticElevationTokens()")
        sb.appendLine("    public let typography = SemanticTypographyTokens()")
        sb.appendLine("    public let motion = SemanticMotionTokens()")
        sb.appendLine()
        sb.appendLine("    public init(colors: SemanticColorTokens = .light) {")
        sb.appendLine("        self.colors = colors")
        sb.appendLine("    }")
        sb.appendLine("}")
        return sb.toString().replace("\r\n", "\n")
    }
}

@CacheableTask
abstract class GenerateDesignTokensTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:Input
    abstract val packageId: Property<String>

    @get:OutputFile
    abstract val kotlinOutputFile: RegularFileProperty

    @get:OutputFile
    abstract val swiftOutputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val model = DesignTokensGenerator.parse(tokensFile.get().asFile.readText())
        val ktFile = kotlinOutputFile.get().asFile
        ktFile.parentFile.mkdirs()
        ktFile.writeText(DesignTokensGenerator.renderKotlin(packageId.get(), model))

        val swiftFile = swiftOutputFile.get().asFile
        swiftFile.parentFile.mkdirs()
        swiftFile.writeText(DesignTokensGenerator.renderSwift(model))
    }
}

@CacheableTask
abstract class CheckDesignTokensTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:Input
    abstract val packageId: Property<String>

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinOutputFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftOutputFile: RegularFileProperty

    @get:OutputFile
    abstract val stampFile: RegularFileProperty

    @TaskAction
    fun check() {
        val model = DesignTokensGenerator.parse(tokensFile.get().asFile.readText())
        val expectedKt = DesignTokensGenerator.renderKotlin(packageId.get(), model)
        val expectedSwift = DesignTokensGenerator.renderSwift(model)

        val actualKtFile = kotlinOutputFile.get().asFile
        val actualSwiftFile = swiftOutputFile.get().asFile

        val actualKt = if (actualKtFile.exists()) actualKtFile.readText().replace("\r\n", "\n") else ""
        val actualSwift = if (actualSwiftFile.exists()) actualSwiftFile.readText().replace("\r\n", "\n") else ""

        val mismatches = mutableListOf<String>()
        if (actualKt != expectedKt) {
            mismatches.add(actualKtFile.relativeToOrSelf(actualKtFile.parentFile).path)
        }
        if (actualSwift != expectedSwift) {
            mismatches.add(actualSwiftFile.relativeToOrSelf(actualSwiftFile.parentFile).path)
        }
        if (mismatches.isNotEmpty()) {
            throw GradleException(
                "Generated design token files are out of date with design-tokens/tokens.json: " +
                    "${mismatches.joinToString(", ")}. Run './gradlew generateDesignTokens' to update them.",
            )
        }
        val stamp = stampFile.get().asFile
        stamp.parentFile.mkdirs()
        stamp.writeText("OK")
    }
}

class DesignTokensPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("scaffold.config")
        val cfg = scaffoldConfig
        val pkgPath = cfg.packageId.replace('.', '/')

        val tokensJson = layout.projectDirectory.file("design-tokens/tokens.json")
        val ktOut = layout.projectDirectory.file(
            "androidApp/designsystem/src/main/kotlin/$pkgPath/designsystem/tokens/GeneratedTokens.kt",
        )
        val swiftOut = layout.projectDirectory.file(
            "iosApp/DesignSystem/Generated/GeneratedTokens.swift",
        )

        val generateTask = tasks.register<GenerateDesignTokensTask>("generateDesignTokens") {
            group = "design tokens"
            description = "Generates Compose and SwiftUI design tokens from design-tokens/tokens.json."
            tokensFile.set(tokensJson)
            packageId.set(cfg.packageId)
            kotlinOutputFile.set(ktOut)
            swiftOutputFile.set(swiftOut)
        }

        tasks.register<CheckDesignTokensTask>("checkDesignTokens") {
            group = "verification"
            description = "Verifies generated Compose and SwiftUI design tokens match design-tokens/tokens.json."
            mustRunAfter(generateTask)
            tokensFile.set(tokensJson)
            packageId.set(cfg.packageId)
            kotlinOutputFile.set(ktOut)
            swiftOutputFile.set(swiftOut)
            stampFile.set(layout.buildDirectory.file("reports/design-tokens/check.stamp"))
        }
    }
}
