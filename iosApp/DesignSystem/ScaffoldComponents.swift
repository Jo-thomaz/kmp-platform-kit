import SwiftUI

public struct ScaffoldSurfaceCard<Content: View>: View {
    @Environment(\.scaffoldTokens) private var tokens
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: tokens.spacing.stackGap) {
            content
        }
        .padding(tokens.spacing.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tokens.colors.backgroundSurface)
        .clipShape(RoundedRectangle(cornerRadius: tokens.radius.card))
        .overlay(
            RoundedRectangle(cornerRadius: tokens.radius.card)
                .stroke(tokens.colors.borderSubtle, lineWidth: 1)
        )
    }
}

public struct ScaffoldPrimaryButton: View {
    @Environment(\.scaffoldTokens) private var tokens
    private let title: String
    private let isEnabled: Bool
    private let action: () -> Void

    public init(
        title: String,
        isEnabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.isEnabled = isEnabled
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Text(title)
                .font(tokens.typography.body)
                .foregroundStyle(tokens.colors.actionOnPrimary)
                .padding(.horizontal, tokens.spacing.cardPadding)
                .padding(.vertical, tokens.spacing.stackGap)
                .background(tokens.colors.actionPrimary)
                .clipShape(RoundedRectangle(cornerRadius: tokens.radius.button))
        }
        .disabled(!isEnabled)
    }
}
