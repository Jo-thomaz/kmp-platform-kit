import SwiftUI

@main
struct ScaffoldIOSApp: App {
    var body: some Scene {
        WindowGroup {
            ScaffoldThemeContainer {
                ReferenceNavigationContainer()
            }
        }
    }
}
