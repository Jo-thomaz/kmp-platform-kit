import SwiftUI

private struct ScaffoldTokensKey: EnvironmentKey {
    static let defaultValue = ScaffoldTokens(colors: .light)
}

public extension EnvironmentValues {
    var scaffoldTokens: ScaffoldTokens {
        get { self[ScaffoldTokensKey.self] }
        set { self[ScaffoldTokensKey.self] = newValue }
    }
}

public struct ScaffoldThemeContainer<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    private let overrideDarkMode: Bool?
    private let content: Content

    public init(
        overrideDarkMode: Bool? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.overrideDarkMode = overrideDarkMode
        self.content = content()
    }

    public var body: some View {
        let isDark = overrideDarkMode ?? (colorScheme == .dark)
        let tokens = ScaffoldTokens(colors: isDark ? .dark : .light)
        content.environment(\.scaffoldTokens, tokens)
    }
}
