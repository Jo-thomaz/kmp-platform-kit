// AUTO-GENERATED FROM design-tokens/tokens.json — DO NOT EDIT BY HAND.
import SwiftUI

enum PrimitiveTokens {
    enum Colors {
        static let brand500 = Color(.sRGB, red: 0.1451, green: 0.3882, blue: 0.9216, opacity: 1.0)
        static let brand400 = Color(.sRGB, red: 0.3765, green: 0.6471, blue: 0.9804, opacity: 1.0)
        static let neutral50 = Color(.sRGB, red: 0.9725, green: 0.9804, blue: 0.9882, opacity: 1.0)
        static let neutral100 = Color(.sRGB, red: 0.9451, green: 0.9608, blue: 0.9765, opacity: 1.0)
        static let neutral200 = Color(.sRGB, red: 0.8863, green: 0.9098, blue: 0.9412, opacity: 1.0)
        static let neutral400 = Color(.sRGB, red: 0.5804, green: 0.6392, blue: 0.7216, opacity: 1.0)
        static let neutral600 = Color(.sRGB, red: 0.2784, green: 0.3333, blue: 0.4118, opacity: 1.0)
        static let neutral800 = Color(.sRGB, red: 0.1176, green: 0.1608, blue: 0.2314, opacity: 1.0)
        static let neutral900 = Color(.sRGB, red: 0.0588, green: 0.0902, blue: 0.1647, opacity: 1.0)
        static let neutral950 = Color(.sRGB, red: 0.0078, green: 0.0235, blue: 0.0902, opacity: 1.0)
        static let red500 = Color(.sRGB, red: 0.8627, green: 0.1490, blue: 0.1490, opacity: 1.0)
        static let red400 = Color(.sRGB, red: 0.9725, green: 0.4431, blue: 0.4431, opacity: 1.0)
        static let green500 = Color(.sRGB, red: 0.0863, green: 0.6392, blue: 0.2902, opacity: 1.0)
        static let green400 = Color(.sRGB, red: 0.2902, green: 0.8706, blue: 0.5020, opacity: 1.0)
        static let white = Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0)
        static let black = Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 1.0)
    }
    enum Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
    }
    enum Radius {
        static let sm: CGFloat = 6
        static let md: CGFloat = 12
        static let lg: CGFloat = 20
        static let full: CGFloat = 999
    }
    enum Elevation {
        static let none: CGFloat = 0
        static let low: CGFloat = 2
        static let medium: CGFloat = 6
        static let high: CGFloat = 12
    }
    enum Typography {
        static let headingLarge = Font.system(size: 28, weight: .bold)
        static let titleMedium = Font.system(size: 18, weight: .semibold)
        static let bodyMedium = Font.system(size: 16, weight: .regular)
        static let labelSmall = Font.system(size: 12, weight: .medium)
    }
    enum Motion {
        static let fast: Double = 0.150
        static let normal: Double = 0.250
        static let slow: Double = 0.400
    }
}

public struct SemanticColorTokens: Sendable {
    public let backgroundCanvas: Color
    public let backgroundSurface: Color
    public let backgroundElevated: Color
    public let textPrimary: Color
    public let textSecondary: Color
    public let actionPrimary: Color
    public let actionOnPrimary: Color
    public let statusError: Color
    public let statusSuccess: Color
    public let borderSubtle: Color

    public static let light = SemanticColorTokens(
        backgroundCanvas: PrimitiveTokens.Colors.neutral50,
        backgroundSurface: PrimitiveTokens.Colors.white,
        backgroundElevated: PrimitiveTokens.Colors.neutral100,
        textPrimary: PrimitiveTokens.Colors.neutral900,
        textSecondary: PrimitiveTokens.Colors.neutral600,
        actionPrimary: PrimitiveTokens.Colors.brand500,
        actionOnPrimary: PrimitiveTokens.Colors.white,
        statusError: PrimitiveTokens.Colors.red500,
        statusSuccess: PrimitiveTokens.Colors.green500,
        borderSubtle: PrimitiveTokens.Colors.neutral200
    )

    public static let dark = SemanticColorTokens(
        backgroundCanvas: PrimitiveTokens.Colors.neutral950,
        backgroundSurface: PrimitiveTokens.Colors.neutral900,
        backgroundElevated: PrimitiveTokens.Colors.neutral800,
        textPrimary: PrimitiveTokens.Colors.neutral50,
        textSecondary: PrimitiveTokens.Colors.neutral400,
        actionPrimary: PrimitiveTokens.Colors.brand400,
        actionOnPrimary: PrimitiveTokens.Colors.neutral950,
        statusError: PrimitiveTokens.Colors.red400,
        statusSuccess: PrimitiveTokens.Colors.green400,
        borderSubtle: PrimitiveTokens.Colors.neutral800
    )
}

public struct SemanticSpacingTokens: Sendable {
    public let screenPadding: CGFloat = PrimitiveTokens.Spacing.md
    public let cardPadding: CGFloat = PrimitiveTokens.Spacing.md
    public let stackGap: CGFloat = PrimitiveTokens.Spacing.sm
    public let sectionGap: CGFloat = PrimitiveTokens.Spacing.lg
    public init() {}
}

public struct SemanticRadiusTokens: Sendable {
    public let card: CGFloat = PrimitiveTokens.Radius.md
    public let button: CGFloat = PrimitiveTokens.Radius.sm
    public let pill: CGFloat = PrimitiveTokens.Radius.full
    public init() {}
}

public struct SemanticElevationTokens: Sendable {
    public let card: CGFloat = PrimitiveTokens.Elevation.low
    public let modal: CGFloat = PrimitiveTokens.Elevation.high
    public init() {}
}

public struct SemanticTypographyTokens: Sendable {
    public let screenTitle: Font = PrimitiveTokens.Typography.headingLarge
    public let cardTitle: Font = PrimitiveTokens.Typography.titleMedium
    public let body: Font = PrimitiveTokens.Typography.bodyMedium
    public let caption: Font = PrimitiveTokens.Typography.labelSmall
    public init() {}
}

public struct SemanticMotionTokens: Sendable {
    public let quick: Double = PrimitiveTokens.Motion.fast
    public let standard: Double = PrimitiveTokens.Motion.normal
    public init() {}
}

public struct ScaffoldTokens: Sendable {
    public let colors: SemanticColorTokens
    public let spacing = SemanticSpacingTokens()
    public let radius = SemanticRadiusTokens()
    public let elevation = SemanticElevationTokens()
    public let typography = SemanticTypographyTokens()
    public let motion = SemanticMotionTokens()

    public init(colors: SemanticColorTokens = .light) {
        self.colors = colors
    }
}
