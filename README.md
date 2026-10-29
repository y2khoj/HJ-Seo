# LiveMic Pro — Android native checkpoint

실시간 마이크 모니터링, 보컬 효과, 단선율 MIDI 채보, 반주, 카메라 녹화를 위한 Android 프로젝트입니다.

> **현재 상태: 개발 중인 체크포인트.** 원본 HTML을 포장한 앱이 아니라 네이티브로 다시 구현 중입니다. 검토한 Figma 시안 v2를 네이티브 화면에 적용했습니다. 실기기에서 지연·오디오 라우팅·카메라 동기화 검증이 필요합니다. 자세한 검증 결과와 남은 작업은 [STATUS.md](docs/STATUS.md)를 보세요.

## 디자인 v2 APK

[디자인 v2 개발용 APK 다운로드](https://github.com/y2khoj/HJ-Seo/raw/refs/heads/main/artifacts/LiveMic-Pro-design-v2.apk) · 버전 `1.1.0-design-v2` / Android 9 이상.

빌드·Lint·네이티브 테스트·서명 검사를 통과했습니다. 실기기 화면과 카메라 동기화는 아직 검증 전입니다. 이전 체크포인트 APK와 개발 서명이 달라 덮어설치할 수 없습니다. 기존 앱을 삭제해야 하는 경우 **먼저 보관함의 WAV/MID/MP4를 파일로 저장하세요. 앱 삭제 시 앱 내부 녹음도 삭제됩니다.**

## 이전 체크포인트 APK

[개발용 APK 릴리스](https://github.com/y2khoj/HJ-Seo/blob/main/artifacts/LiveMic-Pro-checkpoint.apk). 빌드·서명·에뮬레이터 설치를 확인했으며, 실기기에서 기능 검증 전인 개발 버전입니다.

## Figma 디자인

[최신 디자인 시안과 화면별 링크](docs/DESIGN_RESULT.md) · [Figma에서 열기](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=3-50)

마이크·모니터링을 스튜디오로 이동하고, 장르별 반주·최근 파일 목록·카메라 조작을 네이티브 코드와 개발 APK에 반영했습니다.

- [화면 코드](app/src/main/java/pro/livemic/studio/MainActivity.java) · [디자인 토큰과 공통 뷰](app/src/main/java/pro/livemic/studio/StudioTheme.java)
- [최근 파일 저장](app/src/main/java/pro/livemic/studio/RecentBackings.java) · [카메라 제어](app/src/main/java/pro/livemic/studio/CameraController.java)
- 영상 촬영 시 오디오도 시작하며 일시정지·재개·중단 조작을 연결했습니다. 촬영 중 전·후면 전환은 지원 해상도가 맞는 기기에서 동작하며 실기기 검증이 필요합니다.
- 유명곡 목록은 음원이 없는 수록 후보입니다. 현재 재생 가능한 내장 음악은 연습용 합성 반주입니다.

- [화면·상태·컴포넌트 명세](docs/FIGMA_HANDOFF.md)
- [Figma에 붙여넣을 디자인 브리프](docs/FIGMA_PROMPT.md)
- [설계 토큰 JSON](docs/design-tokens.json)
- [제품 범위와 기술 제약](docs/PRODUCT.md)

Figma에서는 화면을 자유롭게 재설계해도 됩니다. 핵심 오디오 엔진과 UI를 분리했으므로 화면 구성을 변경할 수 있습니다. 현재 UI 진입점은 `app/src/main/java/pro/livemic/studio/MainActivity.java`입니다.

## 기술 구성

- Java 17 / Android SDK 35, Android 9(API 28) 이상
- C++17 / NDK 27.2 / AAudio full-duplex 저지연 스트림
- 네이티브 3밴드 EQ, 컴프레서, 알고리즘 리버브, 피드백 딜레이
- YIN 기반 보컬 음정 분석 / Standard MIDI File Format 0, 480 PPQ
- WAV PCM16 스테레오 녹음 / Camera2 + MediaRecorder(H.264) / AAC 변환 + MP4 결합
- 앱 전용 보관함 / Android 문서 선택기를 통한 파일 저장 / 콘텐츠 URI 공유
- 프런트엔드 라이브러리, 서버, 로그인, 클라우드 업로드 없음

## 빌드

Android Studio에서 폴더를 열고 SDK/NDK 설치 안내를 따르세요. 명령행 빌드는 JDK 17 또는 21, Android SDK가 필요합니다.

```sh
# local.properties는 각 기기에서 생성하며 Git에 포함하지 않습니다.
printf 'sdk.dir=/absolute/path/to/Android/Sdk\n' > local.properties
./gradlew :app:assembleDebug
```

필요 패키지: `platforms;android-35`, `build-tools;35.0.0`, `ndk;27.2.12479018`, `cmake;3.22.1`.

APK 출력: `app/build/outputs/apk/debug/app-debug.apk`.
ABI: `arm64-v8a`, `armeabi-v7a`, `x86_64`.

현재 release 설정도 개발용 debug 키를 사용합니다. **스토어 배포용 서명키는 포함하지 않습니다.** 실제 배포 전 소유자의 서명키로 release 설정을 변경하세요.

## 테스트

```sh
./scripts/test-native.sh
```

호스트 C++ 테스트는 음정 탐지, 무음 처리, MIDI 파일 구조, 링 버퍼, DSP의 유한 출력과 리버브 테일, 일시정지 구간을 제외한 MIDI 타이밍을 확인합니다. 장치 I/O와 카메라 테스트를 대체하지 않습니다.

## 사용 흐름

1. 유선 / USB 이어폰을 연결합니다.
2. 마이크를 켜고 Android 녹음 권한을 허용합니다.
3. 보컬 프리셋과 반주를 선택합니다.
4. 오디오 녹음 또는 카메라 화면의 영상 녹화를 시작합니다.
5. 보관함에서 WAV·MID·MP4를 재생, 저장, 공유합니다.

## 반드시 구분할 사항

- 무지연을 보장하지 않습니다. 표시 값은 AAudio 출력 버퍼 길이이며 실제 왕복 지연 측정치가 아닙니다.
- 유선/USB 출력이 없으면 마이크 모니터링을 막습니다. 녹음 자체는 사용할 수 있습니다.
- 블루투스는 저지연 모니터링 대상으로 지원하지 않습니다.
- YouTube는 현재 외부 앱/브라우저 링크만 제공합니다. 그 오디오는 녹음에 포함되지 않습니다.
- 카메라는 앱이 화면에 보일 때 사용하며, 화면을 벗어나면 영상을 저장하고 카메라를 해제합니다.
- 영상 저장 시 WAV를 AAC로 인코딩해 MP4에 결합합니다. 이 단계는 촬영 후에 진행되므로 저장을 기다려야 합니다.
