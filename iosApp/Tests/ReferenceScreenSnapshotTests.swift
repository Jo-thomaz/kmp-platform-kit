import XCTest
import SwiftUI
import UIKit
import SnapshotTesting
import SharedUmbrella
@testable import iosApp

@MainActor
final class ReferenceScreenSnapshotTests: XCTestCase {
    private static let defaultPinnedDeviceTag = "iPhone17Pro_iOS26_4"

    private var sampleItems: [ReferenceItem] {
        [
            ReferenceIosInterop.shared.createItem(
                id: "019a4448-dc01-7000-8000-000000000001",
                title: "Offline-first architecture",
                summary: "Shared presenter state rendered natively in Compose and SwiftUI",
                isCompleted: false
            ),
            ReferenceIosInterop.shared.createItem(
                id: "019a4448-dc02-7000-8000-000000000002",
                title: "Deterministic design tokens",
                summary: "Single W3C DTCG source generating Compose and SwiftUI themes",
                isCompleted: false
            )
        ]
    }

    private var isRecordMode: Bool {
        guard let raw = ProcessInfo.processInfo.environment["SNAPSHOT_RECORD_MODE"]?.lowercased() else {
            return false
        }
        return raw == "true" || raw == "1" || raw == "yes"
    }

    private var deviceTag: String {
        if let envTag = ProcessInfo.processInfo.environment["SNAPSHOT_DEVICE_TAG"], !envTag.isEmpty {
            return envTag
        }
        return Self.defaultPinnedDeviceTag
    }

    private var referenceDirectory: String {
        if let envDir = ProcessInfo.processInfo.environment["SNAPSHOT_REFERENCE_DIR"], !envDir.isEmpty {
            return envDir
        }
        return URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("docs/parity/reference/ios", isDirectory: true)
            .path
    }

    private func assertReferenceSnapshot(
        name: String,
        darkMode: Bool,
        state: ReferenceUiState,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let modeSuffix = darkMode ? "dark" : "light"
        let snapshotTestName = "\(name)_\(deviceTag)"
        let rootView = ScaffoldThemeContainer(overrideDarkMode: darkMode) {
            ReferenceScreen(state: state, onAction: { _ in })
        }
        .frame(width: 400, height: 800)

        let hostingController = UIHostingController(rootView: rootView)
        hostingController.overrideUserInterfaceStyle = darkMode ? .dark : .light
        hostingController.view.frame = CGRect(x: 0, y: 0, width: 400, height: 800)

        let recordFlag = isRecordMode
        let failure = verifySnapshot(
            of: hostingController,
            as: .image(precision: 0.99, perceptualPrecision: 0.98, size: CGSize(width: 400, height: 800)),
            named: modeSuffix,
            record: recordFlag,
            snapshotDirectory: referenceDirectory,
            file: file,
            testName: snapshotTestName,
            line: line
        )

        if !recordFlag, let failureMessage = failure {
            XCTFail(failureMessage, file: file, line: line)
        }
    }

    func testLoadingLight() {
        assertReferenceSnapshot(name: "Loading", darkMode: false, state: ReferenceUiStateLoading.shared)
    }

    func testLoadingDark() {
        assertReferenceSnapshot(name: "Loading", darkMode: true, state: ReferenceUiStateLoading.shared)
    }

    func testContentLight() {
        assertReferenceSnapshot(
            name: "Content",
            darkMode: false,
            state: ReferenceIosInterop.shared.contentState(items: sampleItems)
        )
    }

    func testContentDark() {
        assertReferenceSnapshot(
            name: "Content",
            darkMode: true,
            state: ReferenceIosInterop.shared.contentState(items: sampleItems)
        )
    }

    func testEmptyLight() {
        assertReferenceSnapshot(name: "Empty", darkMode: false, state: ReferenceUiStateEmpty.shared)
    }

    func testEmptyDark() {
        assertReferenceSnapshot(name: "Empty", darkMode: true, state: ReferenceUiStateEmpty.shared)
    }

    func testErrorLight() {
        let error = ReferenceIosInterop.shared.networkError(
            message: "Network connection unavailable",
            statusCode: 503
        )
        assertReferenceSnapshot(
            name: "Error",
            darkMode: false,
            state: ReferenceUiStateError(error: error, retryable: true)
        )
    }

    func testErrorDark() {
        let error = ReferenceIosInterop.shared.networkError(
            message: "Network connection unavailable",
            statusCode: 503
        )
        assertReferenceSnapshot(
            name: "Error",
            darkMode: true,
            state: ReferenceUiStateError(error: error, retryable: true)
        )
    }
}
