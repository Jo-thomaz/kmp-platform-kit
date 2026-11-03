package dev.scaffold.buildlogic

import groovy.json.JsonSlurper

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

internal object DesignTokensParser {
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
}
