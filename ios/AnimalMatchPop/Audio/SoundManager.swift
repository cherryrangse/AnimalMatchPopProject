import AVFoundation
import Foundation

final class SoundManager {
    static let shared = SoundManager()

    private let sampleRate = 44100
    private var bgmPlayer: AVAudioPlayer?
    private var popPlayers: [AVAudioPlayer] = []
    private var explosionPlayer: AVAudioPlayer?
    private var laserPlayer: AVAudioPlayer?
    private var magicChimePlayer: AVAudioPlayer?
    private var feverFanfarePlayer: AVAudioPlayer?
    private var rollbackPlayer: AVAudioPlayer?
    private var isInitialized = false

    private init() {}

    func initializeIfNeeded() {
        guard !isInitialized else { return }
        isInitialized = true

        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            // AudioSession setup failure is non-fatal
        }

        if let url = Bundle.main.url(forResource: "bgm_forest_theme", withExtension: "wav") {
            bgmPlayer = try? AVAudioPlayer(contentsOf: url)
            bgmPlayer?.numberOfLoops = -1
            bgmPlayer?.volume = 0.45
            bgmPlayer?.prepareToPlay()
        }

        let baseFreqs: [Float] = [523.25, 587.33, 659.25, 698.46, 783.99, 880.00, 987.77, 1046.50]
        popPlayers = baseFreqs.compactMap { freq in
            let data = pcmToWav(generatePopTone(freq: freq))
            let p = try? AVAudioPlayer(data: data)
            p?.volume = 0.85
            p?.prepareToPlay()
            return p
        }

        explosionPlayer = try? AVAudioPlayer(data: pcmToWav(generateExplosionTone()))
        explosionPlayer?.prepareToPlay()

        laserPlayer = try? AVAudioPlayer(data: pcmToWav(generateLaserTone()))
        laserPlayer?.prepareToPlay()

        magicChimePlayer = try? AVAudioPlayer(data: pcmToWav(generateMagicChimeTone()))
        magicChimePlayer?.prepareToPlay()

        feverFanfarePlayer = try? AVAudioPlayer(data: pcmToWav(generateFanfareTone()))
        feverFanfarePlayer?.prepareToPlay()

        rollbackPlayer = try? AVAudioPlayer(data: pcmToWav(generatePopTone(freq: 330.0)))
        rollbackPlayer?.volume = 0.55
        rollbackPlayer?.prepareToPlay()
    }

    func startBGM(enabled: Bool) {
        initializeIfNeeded()
        guard enabled else {
            stopBGM()
            return
        }
        if bgmPlayer?.isPlaying == false {
            bgmPlayer?.play()
        }
    }

    func stopBGM() {
        if bgmPlayer?.isPlaying == true {
            bgmPlayer?.pause()
        }
    }

    func playMatchPop(enabled: Bool, combo: Int = 1) {
        guard enabled, !popPlayers.isEmpty else { return }
        let idx = min(max(combo - 1, 0), popPlayers.count - 1)
        let p = popPlayers[idx]
        p.currentTime = 0
        p.play()
    }

    func playExplosion(enabled: Bool) {
        guard enabled else { return }
        explosionPlayer?.currentTime = 0
        explosionPlayer?.play()
    }

    func playLaser(enabled: Bool) {
        guard enabled else { return }
        laserPlayer?.currentTime = 0
        laserPlayer?.play()
    }

    func playMagicChime(enabled: Bool) {
        guard enabled else { return }
        magicChimePlayer?.currentTime = 0
        magicChimePlayer?.play()
    }

    func playInvalidRollback(enabled: Bool) {
        guard enabled else { return }
        rollbackPlayer?.currentTime = 0
        rollbackPlayer?.play()
    }

    func playFeverStart(enabled: Bool) {
        guard enabled else { return }
        feverFanfarePlayer?.currentTime = 0
        feverFanfarePlayer?.play()
    }

    // MARK: - PCM Synthesis

    private func generatePopTone(freq: Float) -> [Int16] {
        let count = (sampleRate * 140) / 1000
        var buffer = [Int16](repeating: 0, count: count)
        for i in 0..<count {
            let t = Float(i) / Float(sampleRate)
            let progress = Float(i) / Float(count)
            let curFreq = freq * (1.0 + 0.6 * (1.0 - progress))
            let wave = sin(2.0 * Float.pi * curFreq * t)
            let env = (1.0 - progress) * (1.0 - progress)
            buffer[i] = Int16(max(-32767, min(32767, wave * env * 32767.0 * 0.8)))
        }
        return buffer
    }

    private func generateExplosionTone() -> [Int16] {
        let count = (sampleRate * 420) / 1000
        var buffer = [Int16](repeating: 0, count: count)
        for i in 0..<count {
            let t = Float(i) / Float(sampleRate)
            let progress = Float(i) / Float(count)
            let lowFreq = 140.0 * (1.0 - progress * 0.75)
            let boom = sin(2.0 * Float.pi * lowFreq * t)
            let noise = Float.random(in: -1.0...1.0) * (1.0 - progress * 0.7)
            let mixed = (boom * 0.8 + noise * 0.2) * (1.0 - progress)
            buffer[i] = Int16(max(-32767, min(32767, mixed * 32767.0 * 0.9)))
        }
        return buffer
    }

    private func generateLaserTone() -> [Int16] {
        let count = (sampleRate * 280) / 1000
        var buffer = [Int16](repeating: 0, count: count)
        for i in 0..<count {
            let t = Float(i) / Float(sampleRate)
            let progress = Float(i) / Float(count)
            let curFreq = 1600.0 * (1.0 - progress * 0.8)
            let wave = sin(2.0 * Float.pi * curFreq * t)
            let env = 1.0 - progress
            buffer[i] = Int16(max(-32767, min(32767, wave * env * 32767.0 * 0.75)))
        }
        return buffer
    }

    private func generateMagicChimeTone() -> [Int16] {
        let count = (sampleRate * 550) / 1000
        var buffer = [Int16](repeating: 0, count: count)
        let freqs: [Float] = [880, 1108, 1318, 1760]
        for i in 0..<count {
            let t = Float(i) / Float(sampleRate)
            let progress = Float(i) / Float(count)
            var wave: Float = 0
            for (idx, f) in freqs.enumerated() {
                let offset = Float(idx) * 0.08
                if t >= offset {
                    wave += sin(2.0 * Float.pi * f * (t - offset)) * 0.25
                }
            }
            let env = 1.0 - progress
            buffer[i] = Int16(max(-32767, min(32767, wave * env * 32767.0 * 0.8)))
        }
        return buffer
    }

    private func generateFanfareTone() -> [Int16] {
        let count = (sampleRate * 600) / 1000
        var buffer = [Int16](repeating: 0, count: count)
        let chord: [Float] = [523.25, 659.25, 783.99, 1046.50]
        for i in 0..<count {
            let t = Float(i) / Float(sampleRate)
            let progress = Float(i) / Float(count)
            var sum: Float = 0
            for f in chord {
                sum += sin(2.0 * Float.pi * f * t)
            }
            let env = 1.0 - progress
            buffer[i] = Int16(max(-32767, min(32767, (sum / Float(chord.count)) * env * 32767.0 * 0.85)))
        }
        return buffer
    }

    private func pcmToWav(_ pcm: [Int16]) -> Data {
        let byteLen = pcm.count * 2
        let totalLen = byteLen + 36
        var data = Data(capacity: 44 + byteLen)

        func appendUInt32(_ val: UInt32) {
            var le = val.littleEndian
            withUnsafeBytes(of: &le) { data.append(contentsOf: $0) }
        }
        func appendUInt16(_ val: UInt16) {
            var le = val.littleEndian
            withUnsafeBytes(of: &le) { data.append(contentsOf: $0) }
        }

        data.append(contentsOf: "RIFF".utf8)
        appendUInt32(UInt32(totalLen))
        data.append(contentsOf: "WAVE".utf8)
        data.append(contentsOf: "fmt ".utf8)
        appendUInt32(16)
        appendUInt16(1)
        appendUInt16(1)
        appendUInt32(UInt32(sampleRate))
        appendUInt32(UInt32(sampleRate * 2))
        appendUInt16(2)
        appendUInt16(16)
        data.append(contentsOf: "data".utf8)
        appendUInt32(UInt32(byteLen))

        for sample in pcm {
            var le = sample.littleEndian
            withUnsafeBytes(of: &le) { data.append(contentsOf: $0) }
        }
        return data
    }
}
