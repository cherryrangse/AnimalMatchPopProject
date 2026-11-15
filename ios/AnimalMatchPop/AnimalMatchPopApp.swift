import SwiftUI

@main
struct AnimalMatchPopApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { newPhase in
            switch newPhase {
            case .active:
                let soundEnabled = UserDefaults.standard.object(forKey: "sound_enabled") as? Bool ?? true
                SoundManager.shared.startBGM(enabled: soundEnabled)
            case .background, .inactive:
                SoundManager.shared.stopBGM()
            @unknown default:
                break
            }
        }
    }
}
