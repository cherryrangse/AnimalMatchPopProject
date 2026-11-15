import math
import struct
import wave
import random

SAMPLE_RATE = 44100

def generate_full_bgm(output_path):
    print("Generating rich pastel game BGM...")
    bpm = 124
    beat_dur = 60.0 / bpm
    total_beats = 64  # 16마디 (약 31초 루프)
    total_samples = int(total_beats * beat_dur * SAMPLE_RATE)
    
    # 2채널 (스테레오) 버퍼
    left_buf = [0.0] * total_samples
    right_buf = [0.0] * total_samples

    # MIDI to freq
    def m2f(m):
        return 440.0 * (2.0 ** ((m - 69) / 12.0))

    # C Major / A Minor 스케일 기반 따뜻하고 사랑스러운 코드 진행
    # 4마디씩: C -> G/B -> Am -> F (4마디) x 2
    # 코러스: F -> G -> Em -> Am -> Dm -> G -> C -> C7 (8마디)
    chord_progression = [
        # 마디 1~4
        (60, [64, 67, 72]),  # C (C4, E4, G4, C5)
        (59, [62, 67, 71]),  # G/B (B3, D4, G4, B4)
        (57, [60, 64, 69]),  # Am (A3, C4, E4, A4)
        (53, [57, 60, 65]),  # F (F3, A3, C4, F4)
        # 마디 5~8
        (60, [64, 67, 72]),  # C
        (59, [62, 67, 71]),  # G/B
        (57, [60, 64, 69]),  # Am
        (53, [57, 60, 65]),  # F
        # 마디 9~12 (후렴 전개)
        (53, [60, 65, 69]),  # F
        (55, [62, 67, 71]),  # G
        (52, [59, 64, 67]),  # Em
        (57, [60, 64, 69]),  # Am
        # 마디 13~16 (마무리 & 루프 턴어라운드)
        (50, [57, 62, 65]),  # Dm
        (55, [59, 62, 67]),  # G
        (48, [60, 64, 67]),  # C
        (48, [58, 64, 67, 70]) # C7
    ]

    # 1. 핑퐁 마림바/뮤직박스 멜로디 (메인 리드 테마)
    melody_notes = [
        # 마디 1 (C)
        (0.0, 72, 0.4), (0.5, 74, 0.4), (1.0, 76, 0.6), (2.0, 79, 0.8), (3.0, 76, 0.8),
        # 마디 2 (G/B)
        (4.0, 74, 0.5), (5.0, 71, 0.5), (6.0, 74, 0.8), (7.0, 76, 0.6),
        # 마디 3 (Am)
        (8.0, 77, 0.5), (9.0, 76, 0.5), (10.0, 74, 0.5), (10.5, 72, 0.5), (11.0, 69, 0.8),
        # 마디 4 (F)
        (12.0, 72, 0.6), (13.0, 74, 0.6), (14.0, 76, 1.2),
        
        # 마디 5 (C)
        (16.0, 79, 0.5), (17.0, 81, 0.5), (18.0, 79, 0.6), (19.0, 76, 0.6),
        # 마디 6 (G/B)
        (20.0, 74, 0.6), (21.0, 76, 0.6), (22.0, 77, 0.8), (23.0, 79, 0.6),
        # 마디 7 (Am)
        (24.0, 81, 0.6), (25.0, 84, 0.6), (26.0, 83, 0.6), (27.0, 81, 0.6),
        # 마디 8 (F)
        (28.0, 79, 0.8), (29.0, 76, 0.8), (30.0, 74, 1.2),

        # 마디 9 (F)
        (32.0, 77, 0.5), (33.0, 79, 0.5), (34.0, 81, 0.8), (35.0, 84, 0.6),
        # 마디 10 (G)
        (36.0, 83, 0.6), (37.0, 81, 0.6), (38.0, 79, 1.0),
        # 마디 11 (Em)
        (40.0, 76, 0.5), (41.0, 79, 0.5), (42.0, 83, 0.8), (43.0, 81, 0.6),
        # 마디 12 (Am)
        (44.0, 79, 0.6), (45.0, 76, 0.6), (46.0, 77, 1.0),
        # 마디 13 (Dm)
        (48.0, 74, 0.5), (49.0, 76, 0.5), (50.0, 77, 0.8), (51.0, 79, 0.6),
        # 마디 14 (G)
        (52.0, 81, 0.6), (53.0, 83, 0.6), (54.0, 84, 1.0), (55.0, 86, 0.5),
        # 마디 15 (C)
        (56.0, 88, 0.8), (57.0, 84, 0.8), (58.0, 79, 0.8), (59.0, 76, 0.8),
        # 마디 16 (C7 & 루프 리턴)
        (60.0, 77, 0.5), (61.0, 76, 0.5), (62.0, 74, 0.5), (63.0, 71, 0.8)
    ]

    # 샘플 합성 함수 (마림바/벨 하모닉스)
    def render_bell(freq, start_sample, dur_samples, gain=0.25, pan=0.0):
        for s in range(dur_samples):
            idx = start_sample + s
            if idx >= total_samples:
                break
            t = s / float(SAMPLE_RATE)
            env = math.exp(-s / (SAMPLE_RATE * 0.45))
            # 기본파 + 2차, 3차, 4차 배음 (맑고 영롱한 마림바/오르골)
            val = (
                math.sin(2.0 * math.pi * freq * t) * 0.6 +
                math.sin(2.0 * math.pi * freq * 2.0 * t) * 0.25 +
                math.sin(2.0 * math.pi * freq * 3.01 * t) * 0.1 +
                math.sin(2.0 * math.pi * freq * 4.0 * t) * 0.05
            ) * env * gain
            left_buf[idx] += val * (1.0 - pan)
            right_buf[idx] += val * (1.0 + pan)

    # 멜로디 렌더링
    for beat_pos, midi_num, dur_beats in melody_notes:
        start_s = int(beat_pos * beat_dur * SAMPLE_RATE)
        dur_s = int(dur_beats * beat_dur * SAMPLE_RATE * 1.5)
        freq = m2f(midi_num)
        pan = 0.15 * math.sin(beat_pos * 0.8)  # 자연스러운 스테레오 패닝
        render_bell(freq, start_s, dur_s, gain=0.28, pan=pan)

    # 2. 통통 튀는 피치카토 아르페지오 (리듬 & 반주)
    for bar_idx, (bass_midi, chord_midis) in enumerate(chord_progression):
        bar_start_beat = bar_idx * 4.0
        # 8분 음표 아르페지오 (한 마디에 8타)
        arp_pattern = [0, 1, 2, 1, 0, 2, 1, 2]
        for sub_idx, chord_note_idx in enumerate(arp_pattern):
            beat = bar_start_beat + sub_idx * 0.5
            start_s = int(beat * beat_dur * SAMPLE_RATE)
            dur_s = int(0.4 * beat_dur * SAMPLE_RATE)
            note = chord_midis[chord_note_idx % len(chord_midis)]
            freq = m2f(note)
            pan = -0.25 if (sub_idx % 2 == 0) else 0.25
            
            # 피치카토 플럭 사운드 (짧고 쫀득한 엔벨로프)
            for s in range(dur_s):
                idx = start_s + s
                if idx >= total_samples: break
                t = s / float(SAMPLE_RATE)
                env = math.exp(-s / (SAMPLE_RATE * 0.14))
                val = (math.sin(2.0 * math.pi * freq * t) + 0.3 * math.sin(2.0 * math.pi * freq * 2 * t)) * env * 0.14
                left_buf[idx] += val * (1.0 - pan)
                right_buf[idx] += val * (1.0 + pan)

    # 3. 따뜻하고 포근한 어쿠스틱 베이스 (풍성한 저음역대 그루브)
    for bar_idx, (bass_midi, _) in enumerate(chord_progression):
        bar_start_beat = bar_idx * 4.0
        # 바운스 베이스 리듬: 1박, 2.5박, 3박, 4박
        bass_hits = [(0.0, 0.9), (1.5, 0.4), (2.0, 0.8), (3.0, 0.7)]
        for b_off, b_dur in bass_hits:
            start_s = int((bar_start_beat + b_off) * beat_dur * SAMPLE_RATE)
            dur_s = int(b_dur * beat_dur * SAMPLE_RATE)
            freq = m2f(bass_midi - 12)  # 1옥타브 아래
            for s in range(dur_s):
                idx = start_s + s
                if idx >= total_samples: break
                t = s / float(SAMPLE_RATE)
                env = math.exp(-s / (SAMPLE_RATE * 0.4))
                # 사인파 + 살짝의 따뜻한 튜브 새추레이션
                raw = math.sin(2.0 * math.pi * freq * t) + 0.25 * math.sin(2.0 * math.pi * freq * 2 * t)
                val = math.tanh(raw * 1.2) * env * 0.24
                left_buf[idx] += val
                right_buf[idx] += val

    # 4. 부드럽고 산뜻한 퍼커션 (어쿠스틱 림샷 & 셰이커 & 킥)
    for b in range(total_beats):
        beat_start_s = int(b * beat_dur * SAMPLE_RATE)
        
        # 킥 (1박, 3박에 부드러운 둥~ 소리)
        if b % 2 == 0:
            dur_s = int(0.18 * SAMPLE_RATE)
            for s in range(dur_s):
                idx = beat_start_s + s
                if idx >= total_samples: break
                t = s / float(SAMPLE_RATE)
                f = 110.0 * math.exp(-s / (SAMPLE_RATE * 0.04))
                env = math.exp(-s / (SAMPLE_RATE * 0.08))
                k = math.sin(2.0 * math.pi * f * t) * env * 0.26
                left_buf[idx] += k
                right_buf[idx] += k

        # 림샷 / 스네어 클랩 (2박, 4박에 탁! 소리)
        if b % 2 == 1:
            dur_s = int(0.12 * SAMPLE_RATE)
            for s in range(dur_s):
                idx = beat_start_s + s
                if idx >= total_samples: break
                env = math.exp(-s / (SAMPLE_RATE * 0.03))
                noise = (random.random() * 2.0 - 1.0) * env * 0.13
                left_buf[idx] += noise * 0.9
                right_buf[idx] += noise * 1.1

        # 셰이커 (8분 음표마다 챠-챠)
        for sub in [0.0, 0.5]:
            sh_start = int((b + sub) * beat_dur * SAMPLE_RATE)
            dur_s = int(0.06 * SAMPLE_RATE)
            for s in range(dur_s):
                idx = sh_start + s
                if idx >= total_samples: break
                env = math.exp(-s / (SAMPLE_RATE * 0.015))
                noise = (random.random() * 2.0 - 1.0) * env * 0.05
                left_buf[idx] += noise
                right_buf[idx] += noise

    # 정규화 및 WAV 파일 작성
    print("Normalizing audio and exporting WAV...")
    max_amp = 0.0
    for i in range(total_samples):
        max_amp = max(max_amp, abs(left_buf[i]), abs(right_buf[i]))
    
    scale_factor = 0.88 / max_amp if max_amp > 0 else 1.0

    wav_file = wave.open(output_path, 'wb')
    wav_file.setnchannels(2)  # Stereo
    wav_file.setsampwidth(2)  # 16-bit
    wav_file.setframerate(SAMPLE_RATE)

    raw_bytes = bytearray()
    for i in range(total_samples):
        # 16비트 클리핑 보호
        l_val = max(-1.0, min(1.0, left_buf[i] * scale_factor))
        r_val = max(-1.0, min(1.0, right_buf[i] * scale_factor))
        l_short = int(l_val * 32767.0)
        r_short = int(r_val * 32767.0)
        raw_bytes.extend(struct.pack('<hh', l_short, r_short))

    wav_file.writeframes(raw_bytes)
    wav_file.close()
    print(f"Successfully generated BGM at: {output_path}")

if __name__ == '__main__':
    out = r"C:\KHT\Developer\Animal-Match-Pop\app\src\main\res\raw\bgm_forest_theme.wav"
    generate_full_bgm(out)
