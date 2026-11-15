# MASTER_SPEC_V2.md - Animal Match Pop (정통 3매치 애니멀 퍼즐)

## 1. Project Overview
- **Brand/Studio:** Pixel Vector
- **App Title:** Animal Match Pop (애니멀 매치 팝)
- **Package Name:** `com.pixelvector.animalmatch`
- **Genre:** Classic Match-3 Casual Puzzle
- **Target / Min / Compile SDK:** 35 (Android 15) / 26 / 35
- **Tech Stack:** Kotlin, Jetpack Compose (BOM), Material3, ViewModel, Coroutines, DataStore Preferences
- **Core Principle:** 중력 스와이프 배제. 정통 인접 타일 스왑(Swap), 3매치 판정, 중력 낙하 및 연쇄 콤보(Cascade) 중심.

---

## 2. Core Game Rules & Mechanics

### 2.1 Board Specification
- Grid Size: 8x8 고정 그리드
- Total Cells: 64개

### 2.2 Animal Characters (6 Types)
1. `BUNNY` (Pink, `#FF85A2`)
2. `BEAR` (Brown/Beige, `#8D6E63`)
3. `CAT` (Yellow, `#FFD54F`)
4. `PIG` (Soft Pink, `#F48FB1`)
5. `PENGUIN` (Blue, `#42A5F5`)
6. `FROG` (Lime Green, `#81C784`)

### 2.3 Tile State & Special Blocks
- `NORMAL`: 기본 3매치 대상
- `LINE_BOMB_H`: 4개 가로 매치 시 생성. 매치 시 해당 가로 라인 전체 소거
- `LINE_BOMB_V`: 4개 세로 매치 시 생성. 매치 시 해당 세로 라인 전체 소거
- `AREA_BOMB`: T자 또는 L자 5개 매치 시 생성. 주변 3x3 범위 폭발
- `RAINBOW_MIRROR`: 일렬 5개 매치 시 생성. 인접 동물과 스왑 시 해당 동물 화면 전체 소거

### 2.4 Interaction Rules
- **Swap & Rollback:** 상/하/좌/우 인접한 두 타일만 드래그 또는 2회 터치로 교환 가능. 매치가 성립되지 않을 경우 원위치 복귀(Rollback).
- **Cascade & Refill:** 매치된 타일 소거 -> 위의 타일들이 아래로 자연 낙하 -> 상단 빈자리에 새 타일 무작위 생성 -> 추가 매치 발생 시 콤보 누적.
- **Fever Time:** 5연속 콤보 달성 시 5초간 Fever 모드 (점수 2배 획득 + 파티클 연출).

---

## 3. UI/UX Architecture (Jetpack Compose)

```text
+-------------------------------------------------------------+
|          [ Google AdMob Adaptive Banner (최상단 고정) ]      |
+-------------------------------------------------------------+
|          [ 25dp Safety Divider ]                            |
+-------------------------------------------------------------+
|  [ Score: 12,450 ]        [ Time: 42s ]       [ Combo: x3 ] | <- Top HUD
+-------------------------------------------------------------+
|  [ Fever Bar: ■■■■■■□□□□ ]                                  |
+-------------------------------------------------------------+
|                                                             |
|                   8x8 Match-3 Board                         |
|           - Canvas/Vector 기반 고품질 렌더링                  |
|           - 드래그 제스처 및 인접 셀 스왑 애니메이션           |
|                                                             |
+-------------------------------------------------------------+
|  [ Sound Toggle ]      [ Pause ]      [ HighScore: 48,200 ] | <- Bottom Bar
+-------------------------------------------------------------+
```

---

## 4. Clean Architecture & Security Modules
- **Package Structure (`com.pixelvector.animalmatch`):**
  - `domain/` (`model`, `engine`, `security`): 순수 Kotlin 도메인 모델, 8x8 Match-3 코어 엔진, 보안 모듈
  - `data/` (`local`): DataStore Preferences 기반 암호화/체크섬 상태 저장소 (최고 점수, 코인, 사운드, 광고 제거 여부)
  - `ui/` (`theme`, `components`, `screen`, `viewmodel`): Jetpack Compose UI 및 MVVM ViewModel
- **Security & Protection:**
  - `SecureInt`: 런타임 XOR 난독화로 점수 및 코인 메모리 스캔/변조 방어
  - `AntiMacroLimiter`: 비정상 초고속 터치 및 고정 주기 매크로 입력 차단
  - `DataStore Preferences`: 기기 핑거프린트 + SHA-256 해시 체크섬 및 XOR/암호화 기반 최고 점수·코인 무결성 검증
- **Localization:** 5대 언어 지원 (`values`, `values-ko`, `values-ja`, `values-es`, `values-de`)

---

## 5. Development Phases
- **Phase 1:** 프로젝트 스캐폴딩, Gradle 설정 (SDK 35, Compose BOM), Clean Architecture + MVVM 기본 패키지 구조, 보안 모듈(`SecureInt`, `AntiMacroLimiter`), DataStore 암호화 저장소, 5대 언어 `strings.xml`, 도메인 기초 모델(`AnimalType`, `SpecialBlockType`, `TileCell`) 및 초기 빌드/테스트 검증.
- **Phase 2:** 8x8 Match-3 코어 엔진 (초기 무결성 스폰, 인접 스왑 & 롤백, 3/4/5/T/L 매치 감지, 특수 블록 생성 및 연쇄 폭발, 중력 낙하 & 리필 Cascade 루프, Fever 게이지 로직) 및 JUnit 단위 테스트.
- **Phase 3:** Jetpack Compose Canvas/Vector 기반 6종 동물 캐릭터 및 특수 블록 고품질 렌더링, 드래그/터치 스왑 애니메이션, HUD/Fever Bar/파티클 연출 구현.
- **Phase 4:** AdMob Adaptive Banner (25dp Safety Margin), Google UMP 동의, Play Billing (`remove_ads`), 사운드/효과음 및 출시 패키징(R8).
