import SwiftUI

struct AnimalVectorTileView: View {
    let animal: AnimalType
    let special: SpecialBlockType
    let isSelected: Bool
    let isMatched: Bool
    var tileId: Int64 = 0

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 60.0)) { timeline in
            let elapsed = timeline.date.timeIntervalSinceReferenceDate
            let waveAngle = (elapsed.truncatingRemainder(dividingBy: 2.1) / 2.1) * (.pi * 2.0)
            let phase = waveAngle + Double(tileId % 17) * 0.55
            let sinVal = sin(phase)
            let cosVal = cos(phase)

            let baseScale: CGFloat = isMatched ? 1.25 : (isSelected ? 1.14 : 1.0)
            let pulseCycle = abs(sin((elapsed / 0.52) * .pi))
            let specialPulse: CGFloat = special == .normal ? 1.0 : (0.93 + CGFloat(pulseCycle) * 0.16)

            let squashIntensity: CGFloat = isSelected ? 0.065 : 0.032
            let scaleX = baseScale * (1.0 + CGFloat(sinVal) * squashIntensity) * specialPulse
            let scaleY = baseScale * (1.0 - CGFloat(sinVal) * squashIntensity) * specialPulse

            let tiltDeg: Double = {
                if isMatched { return sin(phase * 3.0) * 10.0 }
                if isSelected { return sin(phase * 2.5) * 7.5 }
                return cosVal * 2.2
            }()

            let isHappyOrBlink = isMatched || isSelected || (sin(phase + Double(tileId % 5) * 1.1) > 0.92)
            let earTwitch = CGFloat(sin(phase * 2.0) * 0.06)

            Canvas { context, size in
                let bobY = CGFloat(sinVal) * size.height * 0.024
                let cx = size.width / 2.0
                let cy = size.height / 2.0

                context.translateBy(x: cx, y: cy + bobY)
                context.rotate(by: .degrees(tiltDeg))
                context.scaleBy(x: scaleX, y: scaleY)
                context.translateBy(x: -cx, y: -cy)

                if special == .rainbowMirror {
                    drawRainbowMirrorBall(context: &context, size: size, waveAngle: waveAngle)
                } else {
                    drawAnimalFace(
                        context: &context,
                        size: size,
                        animal: animal,
                        isHappyOrBlink: isHappyOrBlink,
                        earTwitch: earTwitch
                    )
                    drawSpecialOverlay(context: &context, size: size, special: special)
                }

                if isMatched {
                    drawBurstSparkles(context: &context, size: size)
                }
            }
        }
    }

    private func drawAnimalFace(
        context: inout GraphicsContext,
        size: CGSize,
        animal: AnimalType,
        isHappyOrBlink: Bool,
        earTwitch: CGFloat
    ) {
        let w = size.width
        let h = size.height
        let cx = w / 2.0
        let cy = h / 2.0
        let r = min(w, h) * 0.42
        let baseColor = animal.color
        let darkEye = Color(hex: 0x2D1E18)
        let blush = animal == .bunny ? Color(hex: 0xFF80AB, opacity: 0.45) : Color(hex: 0xFF5252, opacity: 0.38)
        let earOffsetY = r * earTwitch

        // 1. 귀(Ears)
        switch animal {
        case .bunny:
            let earW = r * 0.42
            let earH = r * 0.98
            let leftRect = CGRect(x: cx - r * 0.65, y: cy - r * 1.35 + earOffsetY, width: earW, height: earH)
            let rightRect = CGRect(x: cx + r * 0.23, y: cy - r * 1.35 - earOffsetY, width: earW, height: earH)
            let earBorder = Color(hex: 0xE0E0E0)
            let innerPink = Color(hex: 0xFFB2C9)

            context.fill(Path(roundedRect: leftRect, cornerRadius: earW / 2.0), with: .color(.white))
            context.stroke(Path(roundedRect: leftRect, cornerRadius: earW / 2.0), with: .color(earBorder), lineWidth: 1.8)
            let leftInner = CGRect(x: cx - r * 0.55, y: cy - r * 1.20 + earOffsetY, width: earW * 0.5, height: earH * 0.65)
            context.fill(Path(roundedRect: leftInner, cornerRadius: earW / 3.0), with: .color(innerPink))

            context.fill(Path(roundedRect: rightRect, cornerRadius: earW / 2.0), with: .color(.white))
            context.stroke(Path(roundedRect: rightRect, cornerRadius: earW / 2.0), with: .color(earBorder), lineWidth: 1.8)
            let rightInner = CGRect(x: cx + r * 0.33, y: cy - r * 1.20 - earOffsetY, width: earW * 0.5, height: earH * 0.65)
            context.fill(Path(roundedRect: rightInner, cornerRadius: earW / 3.0), with: .color(innerPink))

        case .bear:
            fillCircle(&context, center: CGPoint(x: cx - r * 0.68, y: cy - r * 0.68 + earOffsetY), radius: r * 0.38, color: baseColor)
            fillCircle(&context, center: CGPoint(x: cx - r * 0.68, y: cy - r * 0.68 + earOffsetY), radius: r * 0.20, color: Color(hex: 0xD7CCC8))
            fillCircle(&context, center: CGPoint(x: cx + r * 0.68, y: cy - r * 0.68 - earOffsetY), radius: r * 0.38, color: baseColor)
            fillCircle(&context, center: CGPoint(x: cx + r * 0.68, y: cy - r * 0.68 - earOffsetY), radius: r * 0.20, color: Color(hex: 0xD7CCC8))

        case .cat:
            var leftEar = Path()
            leftEar.move(to: CGPoint(x: cx - r * 0.85, y: cy - r * 0.25))
            leftEar.addLine(to: CGPoint(x: cx - r * 0.75, y: cy - r * 1.05 + earOffsetY))
            leftEar.addLine(to: CGPoint(x: cx - r * 0.15, y: cy - r * 0.70))
            leftEar.closeSubpath()
            context.fill(leftEar, with: .color(baseColor))

            var rightEar = Path()
            rightEar.move(to: CGPoint(x: cx + r * 0.85, y: cy - r * 0.25))
            rightEar.addLine(to: CGPoint(x: cx + r * 0.75, y: cy - r * 1.05 - earOffsetY))
            rightEar.addLine(to: CGPoint(x: cx + r * 0.15, y: cy - r * 0.70))
            rightEar.closeSubpath()
            context.fill(rightEar, with: .color(baseColor))

        case .pig:
            fillCircle(&context, center: CGPoint(x: cx - r * 0.72, y: cy - r * 0.62 + earOffsetY), radius: r * 0.32, color: Color(hex: 0xEC407A))
            fillCircle(&context, center: CGPoint(x: cx + r * 0.72, y: cy - r * 0.62 - earOffsetY), radius: r * 0.32, color: Color(hex: 0xEC407A))

        case .penguin:
            fillCircle(&context, center: CGPoint(x: cx, y: cy - r * 0.95 + earOffsetY), radius: r * 0.22, color: baseColor)

        case .frog:
            fillCircle(&context, center: CGPoint(x: cx - r * 0.52, y: cy - r * 0.72), radius: r * 0.38, color: baseColor)
            fillCircle(&context, center: CGPoint(x: cx + r * 0.52, y: cy - r * 0.72), radius: r * 0.38, color: baseColor)
        }

        // 2. 얼굴 본체 + 그림자 + 외곽선 + 젤리 광택
        fillCircle(&context, center: CGPoint(x: cx, y: cy + r * 0.13), radius: r * 1.04, color: Color.black.opacity(0.20))
        fillCircle(&context, center: CGPoint(x: cx, y: cy), radius: r, color: baseColor)
        let rimColor = animal == .bunny ? Color(hex: 0xE0E0E0) : Color.white.opacity(0.88)
        strokeCircle(&context, center: CGPoint(x: cx, y: cy), radius: r, color: rimColor, lineWidth: 2.2)
        fillCircle(&context, center: CGPoint(x: cx - r * 0.08, y: cy - r * 0.12), radius: r * 0.80, color: Color.white.opacity(0.30))
        fillCircle(&context, center: CGPoint(x: cx - r * 0.45, y: cy - r * 0.48), radius: r * 0.16, color: Color.white.opacity(0.55))

        // 3. 동물별 얼굴 디테일
        switch animal {
        case .penguin:
            fillCircle(&context, center: CGPoint(x: cx, y: cy + r * 0.12), radius: r * 0.72, color: .white)
            var beak = Path()
            beak.move(to: CGPoint(x: cx - r * 0.25, y: cy + r * 0.05))
            beak.addLine(to: CGPoint(x: cx + r * 0.25, y: cy + r * 0.05))
            beak.addLine(to: CGPoint(x: cx, y: cy + r * 0.35))
            beak.closeSubpath()
            context.fill(beak, with: .color(Color(hex: 0xFFB300)))

        case .pig:
            let snoutRect = CGRect(x: cx - r * 0.36, y: cy - r * 0.02, width: r * 0.72, height: r * 0.48)
            context.fill(Path(roundedRect: snoutRect, cornerRadius: r * 0.24), with: .color(Color(hex: 0xEC407A)))
            fillCircle(&context, center: CGPoint(x: cx - r * 0.14, y: cy + r * 0.22), radius: r * 0.08, color: darkEye)
            fillCircle(&context, center: CGPoint(x: cx + r * 0.14, y: cy + r * 0.22), radius: r * 0.08, color: darkEye)

        case .bear:
            fillCircle(&context, center: CGPoint(x: cx, y: cy + r * 0.20), radius: r * 0.36, color: Color(hex: 0xEFEBE9))
            fillCircle(&context, center: CGPoint(x: cx, y: cy + r * 0.12), radius: r * 0.13, color: darkEye)

        case .cat:
            fillCircle(&context, center: CGPoint(x: cx, y: cy + r * 0.12), radius: r * 0.12, color: Color(hex: 0xFF80AB))
            strokeLine(&context, from: CGPoint(x: cx - r * 0.85, y: cy + r * 0.05), to: CGPoint(x: cx - r * 0.45, y: cy + r * 0.12), color: darkEye.opacity(0.6), width: 2.3)
            strokeLine(&context, from: CGPoint(x: cx + r * 0.85, y: cy + r * 0.05), to: CGPoint(x: cx + r * 0.45, y: cy + r * 0.12), color: darkEye.opacity(0.6), width: 2.3)

        case .bunny:
            let nosePink = Color(hex: 0xFF4081)
            fillCircle(&context, center: CGPoint(x: cx, y: cy + r * 0.12), radius: r * 0.10, color: nosePink)
            strokeLine(&context, from: CGPoint(x: cx, y: cy + r * 0.14), to: CGPoint(x: cx, y: cy + r * 0.22), color: nosePink, width: 2.2)
            strokeLine(&context, from: CGPoint(x: cx, y: cy + r * 0.22), to: CGPoint(x: cx - r * 0.12, y: cy + r * 0.26), color: nosePink, width: 2.2)
            strokeLine(&context, from: CGPoint(x: cx, y: cy + r * 0.22), to: CGPoint(x: cx + r * 0.12, y: cy + r * 0.26), color: nosePink, width: 2.2)

        case .frog:
            fillCircle(&context, center: CGPoint(x: cx - r * 0.52, y: cy - r * 0.72), radius: r * 0.22, color: .white)
            fillCircle(&context, center: CGPoint(x: cx + r * 0.52, y: cy - r * 0.72), radius: r * 0.22, color: .white)
            if isHappyOrBlink {
                strokeLine(&context, from: CGPoint(x: cx - r * 0.62, y: cy - r * 0.72), to: CGPoint(x: cx - r * 0.42, y: cy - r * 0.72), color: darkEye, width: 2.8)
                strokeLine(&context, from: CGPoint(x: cx + r * 0.42, y: cy - r * 0.72), to: CGPoint(x: cx + r * 0.62, y: cy - r * 0.72), color: darkEye, width: 2.8)
            } else {
                fillCircle(&context, center: CGPoint(x: cx - r * 0.52, y: cy - r * 0.72), radius: r * 0.12, color: darkEye)
                fillCircle(&context, center: CGPoint(x: cx + r * 0.52, y: cy - r * 0.72), radius: r * 0.12, color: darkEye)
            }
            strokeLine(&context, from: CGPoint(x: cx - r * 0.35, y: cy + r * 0.18), to: CGPoint(x: cx + r * 0.35, y: cy + r * 0.18), color: darkEye, width: 2.8)
        }

        // 4. 공통 눈 & 볼터치
        if animal != .frog {
            if isHappyOrBlink {
                var leftSmile = Path()
                leftSmile.move(to: CGPoint(x: cx - r * 0.48, y: cy - r * 0.08))
                leftSmile.addQuadCurve(to: CGPoint(x: cx - r * 0.24, y: cy - r * 0.08), control: CGPoint(x: cx - r * 0.36, y: cy - r * 0.24))
                context.stroke(leftSmile, with: .color(darkEye), style: StrokeStyle(lineWidth: 2.8, lineCap: .round))

                var rightSmile = Path()
                rightSmile.move(to: CGPoint(x: cx + r * 0.24, y: cy - r * 0.08))
                rightSmile.addQuadCurve(to: CGPoint(x: cx + r * 0.48, y: cy - r * 0.08), control: CGPoint(x: cx + r * 0.36, y: cy - r * 0.24))
                context.stroke(rightSmile, with: .color(darkEye), style: StrokeStyle(lineWidth: 2.8, lineCap: .round))
            } else {
                fillCircle(&context, center: CGPoint(x: cx - r * 0.36, y: cy - r * 0.12), radius: r * 0.135, color: darkEye)
                fillCircle(&context, center: CGPoint(x: cx + r * 0.36, y: cy - r * 0.12), radius: r * 0.135, color: darkEye)
                fillCircle(&context, center: CGPoint(x: cx - r * 0.40, y: cy - r * 0.16), radius: r * 0.055, color: .white)
                fillCircle(&context, center: CGPoint(x: cx + r * 0.32, y: cy - r * 0.16), radius: r * 0.055, color: .white)
            }
        }

        fillCircle(&context, center: CGPoint(x: cx - r * 0.52, y: cy + r * 0.10), radius: r * 0.17, color: blush)
        fillCircle(&context, center: CGPoint(x: cx + r * 0.52, y: cy + r * 0.10), radius: r * 0.17, color: blush)
    }

    private func drawSpecialOverlay(context: inout GraphicsContext, size: CGSize, special: SpecialBlockType) {
        let w = size.width
        let h = size.height
        let cx = w / 2.0
        let cy = h / 2.0
        let r = min(w, h) * 0.44

        switch special {
        case .lineBombH:
            strokeLine(&context, from: CGPoint(x: w * 0.08, y: cy), to: CGPoint(x: w * 0.92, y: cy), color: .white.opacity(0.9), width: 4.5)
            strokeCircle(&context, center: CGPoint(x: cx, y: cy), radius: r, color: Color(hex: 0xFFEA00), lineWidth: 3.0)
        case .lineBombV:
            strokeLine(&context, from: CGPoint(x: cx, y: h * 0.08), to: CGPoint(x: cx, y: h * 0.92), color: .white.opacity(0.9), width: 4.5)
            strokeCircle(&context, center: CGPoint(x: cx, y: cy), radius: r, color: Color(hex: 0x00E5FF), lineWidth: 3.0)
        case .areaBomb:
            strokeCircle(&context, center: CGPoint(x: cx, y: cy), radius: min(w, h) * 0.45, color: Color(hex: 0xFF1744), lineWidth: 3.5)
            fillCircle(&context, center: CGPoint(x: cx, y: h * 0.12), radius: min(w, h) * 0.12, color: Color(hex: 0xFFEA00))
        case .normal, .rainbowMirror:
            break
        }
    }

    private func drawRainbowMirrorBall(context: inout GraphicsContext, size: CGSize, waveAngle: Double) {
        let w = size.width
        let h = size.height
        let cx = w / 2.0
        let cy = h / 2.0
        let r = min(w, h) * 0.42

        let colors = [
            Color(hex: 0xFF5252), Color(hex: 0xFFB300), Color(hex: 0xFFEB3B),
            Color(hex: 0x69F0AE), Color(hex: 0x40C4FF), Color(hex: 0xE040FB),
            Color(hex: 0xFF5252)
        ]

        let circlePath = Path(ellipseIn: CGRect(x: cx - r, y: cy - r, width: r * 2, height: r * 2))
        context.fill(
            circlePath,
            with: .conicGradient(Gradient(colors: colors), center: CGPoint(x: cx, y: cy), angle: .radians(waveAngle))
        )
        context.stroke(circlePath, with: .color(.white), lineWidth: 3.0)

        var star = Path()
        star.move(to: CGPoint(x: cx, y: cy - r * 0.65))
        star.addLine(to: CGPoint(x: cx + r * 0.20, y: cy - r * 0.20))
        star.addLine(to: CGPoint(x: cx + r * 0.65, y: cy))
        star.addLine(to: CGPoint(x: cx + r * 0.20, y: cy + r * 0.20))
        star.addLine(to: CGPoint(x: cx, y: cy + r * 0.65))
        star.addLine(to: CGPoint(x: cx - r * 0.20, y: cy + r * 0.20))
        star.addLine(to: CGPoint(x: cx - r * 0.65, y: cy))
        star.addLine(to: CGPoint(x: cx - r * 0.20, y: cy - r * 0.20))
        star.closeSubpath()
        context.fill(star, with: .color(.white))
    }

    private func drawBurstSparkles(context: inout GraphicsContext, size: CGSize) {
        let cx = size.width / 2.0
        let cy = size.height / 2.0
        let r = min(size.width, size.height) * 0.47
        strokeCircle(&context, center: CGPoint(x: cx, y: cy), radius: r, color: Color(hex: 0xFFF59D, opacity: 0.85), lineWidth: 4.5)
        fillCircle(&context, center: CGPoint(x: cx - r * 0.85, y: cy - r * 0.85), radius: r * 0.14, color: .white)
        fillCircle(&context, center: CGPoint(x: cx + r * 0.85, y: cy - r * 0.85), radius: r * 0.14, color: .white)
        fillCircle(&context, center: CGPoint(x: cx - r * 0.85, y: cy + r * 0.85), radius: r * 0.14, color: .white)
        fillCircle(&context, center: CGPoint(x: cx + r * 0.85, y: cy + r * 0.85), radius: r * 0.14, color: .white)
    }

    // MARK: - Helpers

    private func fillCircle(_ context: inout GraphicsContext, center: CGPoint, radius: CGFloat, color: Color) {
        let rect = CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)
        context.fill(Path(ellipseIn: rect), with: .color(color))
    }

    private func strokeCircle(_ context: inout GraphicsContext, center: CGPoint, radius: CGFloat, color: Color, lineWidth: CGFloat) {
        let rect = CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)
        context.stroke(Path(ellipseIn: rect), with: .color(color), lineWidth: lineWidth)
    }

    private func strokeLine(_ context: inout GraphicsContext, from: CGPoint, to: CGPoint, color: Color, width: CGFloat) {
        var path = Path()
        path.move(to: from)
        path.addLine(to: to)
        context.stroke(path, with: .color(color), style: StrokeStyle(lineWidth: width, lineCap: .round))
    }
}
