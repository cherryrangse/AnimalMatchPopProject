import SwiftUI

private struct ParticleSpec {
    let xRatio: CGFloat
    let baseYRatio: CGFloat
    let radius: CGFloat
    let speed: CGFloat
    let phase: CGFloat
    let color: Color
}

struct FeverParticleOverlayView: View {
    let isActive: Bool

    private let particles: [ParticleSpec] = {
        let palette: [Color] = [
            Color(hex: 0xFF85A2),
            Color(hex: 0xFFD54F),
            Color(hex: 0x42A5F5),
            Color(hex: 0x81C784),
            Color(hex: 0xFF4081)
        ]
        return (0..<24).map { i in
            let seed = Double(i * 37 + 13)
            let x = CGFloat((sin(seed * 1.3) + 1.0) * 0.5)
            let y = CGFloat((cos(seed * 2.1) + 1.0) * 0.5)
            let r = CGFloat(4.0 + (Double(i % 5) * 1.5))
            let spd = CGFloat(0.5 + Double(i % 4) * 0.2)
            let ph = CGFloat(seed.truncatingRemainder(dividingBy: 6.28))
            return ParticleSpec(xRatio: x, baseYRatio: y, radius: r, speed: spd, phase: ph, color: palette[i % palette.count])
        }
    }()

    var body: some View {
        if isActive {
            TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
                let progress = (timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1.8)) / 1.8
                Canvas { context, size in
                    for p in particles {
                        let rawY = p.baseYRatio - CGFloat(progress) * p.speed
                        let currentYRatio = rawY - floor(rawY)
                        let waveX = sin(CGFloat(progress) * 6.28 + p.phase) * 12.0
                        let x = min(max(p.xRatio * size.width + waveX, 0), size.width)
                        let y = currentYRatio * size.height
                        let alpha = min(max(0.35 + 0.45 * cos(CGFloat(progress) * 6.28 + p.phase), 0.15), 0.85)

                        let rect = CGRect(x: x - p.radius, y: y - p.radius, width: p.radius * 2, height: p.radius * 2)
                        context.fill(Path(ellipseIn: rect), with: .color(p.color.opacity(Double(alpha))))
                    }
                }
            }
            .allowsHitTesting(false)
        }
    }
}
