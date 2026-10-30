# 3.0 검증 기록 — 2026-10-30

기준 커밋 `e1dd49c`에서 `codex/live-vault-3-composer`로 구현했다. 목표 버전은 `3.0.0-preview / versionCode 9`이다. 아래 검사는 호스트/Android 뷰 렌더링/빌드 검사이며 실제 S26 Ultra에서 실행한 결과가 아니다.

## 완료한 검사

| 영역 | 확인한 내용 | 결과 |
|---|---|---|
| Android 빌드 | standard/personal debug, arm64-v8a/armeabi-v7a/x86_64, standard instrumentation APK | 통과 |
| 음악/저장 | 12키의 자동·수동·분수 베이스·프레이즈 전조, 5스케일 7도 화성, 수동/멜로디 보존, 3박자 분할/6/8 BPM 환산 | 통과 |
| 음역/편성 | 12근음×11종 기타 음역/코드톤/최대 6현, 베이스 28–55, 8트랙 계획, 곡 길이/버전 거부 | 통과 |
| 재현/프레이즈 | 동일 시드 이벤트 일치, 사용자 프레이즈 시작 beat 무작위화 없음, 양자화/스트럼 시차/표현 저장 | 통과 |
| 저장 트랜잭션 | AtomicFile 초안 복구, 20단계 이력, 완료 전 기존 트랙 보존, 취소, 완료 교체 후 undo/redo, 전조 | 통과 |
| 네이티브 발음 | 44.1/48kHz 합성 196Hz 입력의 최초 발음 100ms 이내, 동일음 강약 재발음, ±55센트 비브라토 유지, 신뢰도/무음 게이트 | 통과 |
| 네이티브 합성 | 44.1/48kHz 8트랙+목소리, 베이스 최초 옥타브 고정, 카운트인/프레이즈 종료, 정지 후 무음 | 통과 |
| 콜백 할당 | TSF malloc/realloc 계측으로 재생 콜백 중 추가 할당 0 | 통과 |
| UI | API 35, 412×893, 위 36/아래 24px inset, 기본/130% 글씨, 8트랙, 스크롤 전후 재생·프레이즈 버튼 고정, 악기 검색, 기존 3탭 | 통과 |
| 기존 기능 | API 28/35 라이브/연습/카메라 고정 조작부, 선택 시트/슬라이더/seek, 기존 DSP/피드백 보호/캡처 버스/녹음 싱크/보컬 채점/예약 검사 | 통과 |
| APK 내용 | 두 앱 ID/versionCode 9, SF2 원본 SHA-256, 음원/TSF 라이선스, 3 ABI, standard에 MediaProjection 권한/캡처 구현 제외 | 통과 |

Java/Robolectric 13개(프로젝트 7개·화면/세션 6개)가 통과했다. native compiler는 NDK 27.2, SDK 35, build-tools 35.0.0, CMake 3.22.1, JDK 17을 사용했다. TinySoundFont 원본의 null-pointer offset 매크로에 대한 NDK 경고는 남아 있다.

## 실제 View 렌더링

아래는 실제 Java 화면을 Robolectric/API 35·네이티브 Skia로 렌더링했다. 오디오 JNI만 테스트 대체를 사용하며 테스트 데이터/대체 클래스는 설치 APK에 들어가지 않는다. 실기기 스크린샷이나 발성 측정 그래프가 아니다.

| 기본 글씨 | 130% 글씨 |
|---|---|
| [작곡기](screenshots/v3.0/v3-default-composer.png) | [작곡기](screenshots/v3.0/v3-large-composer.png) |
| [8트랙 본문 하단](screenshots/v3.0/v3-default-tracks.png) | [8트랙 본문 하단](screenshots/v3.0/v3-large-tracks.png) |
| [목소리 연주](screenshots/v3.0/v3-default-voice.png) | [목소리 연주](screenshots/v3.0/v3-large-voice.png) |
| [악기 검색](screenshots/v3.0/v3-default-search.png) | [악기 검색](screenshots/v3.0/v3-large-search.png) |

## 아직 실기기에서 확인하지 않은 항목

- AAudio 출력만 시작 → 입력 합류 → 마이크 종료 중 반주 유지, Android 14/15 foreground service와 장치 선점/복구.
- 실제 지속음/따따따/글리산도/숨소리/옥타브 오류/코드 전환. 합성 입력 결과를 실제 발성 지연이나 모든 주파수의 성능으로 일반화하지 않는다.
- 유선/USB/Bluetooth의 왕복·전송 지연, 실제 음향 하울링·AEC 효과와 스피커 음색.
- 기기에서 들은 연주와 저장 프레이즈의 전체 타이밍 대조, 8트랙·16마디·장시간 음성 연주의 CPU/xrun/발열/배터리.
- 마이크/목소리/반주 선택 믹스의 실제 WAV/영상, 가이드음의 음향 재유입, Camera2 촬영과 싱크.
- 개인용 MediaProjection 승인·철회, 대상 앱 캡처 허용 및 실제 클린 보컬/내부 오디오 시간 정렬. 개인용은 새 로컬 오버레이로 빌드했고 이전 비공개 구현과의 런타임 대조는 하지 않았다.
- 실제 S26 Ultra의 One UI 화면 확대·키보드·제스처/3버튼 내비게이션과 TalkBack 연속 탐색.

현재 환경에는 `/dev/kvm`과 실제 Android 장치가 없어 장치 instrumentation APK는 빌드까지만 확인했다.

## 서명과 전달 상태

기존 `artifacts/Live-Vault-standard.apk`는 그대로 보존한다. 기존 서명 SHA-256은 `6a61c39da7f13d3c7622c12068efd1e5bcb43bae2b7748821e2d2a9af169a139`이고 이번 검증 키는 `bc13d391e30da9ae1463df3a5af5fb5f60dc589e1ea1e8748877a3b73c45f2c2`이다. 현재 APK는 **새 키의 검증용이며 기존 설치를 그대로 업데이트할 수 없다**. 기존 테이크가 있는 앱을 제거해 설치하는 방법을 업데이트 경로로 안내하지 않는다.

기존 서명키가 도착하면 `LIVE_VAULT_KEYSTORE`를 지정해 두 APK를 재빌드하고 원본 APK와 `apksigner verify --print-certs` 지문을 대조한 다음 업데이트용으로 전달한다. 이는 아직 완료하지 못한 전달 요구사항이다. 개인용 소스와 APK는 Git ignore 경로와 비공개 전달 경로에만 둔다.

## 재현 명령

```sh
./scripts/test-native.sh
./scripts/test-schedule.sh
./scripts/test-v2.sh
./scripts/test-composer.sh
./gradlew :app:testStandardDebugUnitTest :app:assembleStandardDebug
# 비공개 오버레이를 별도 설치한 환경에서만
./gradlew :app:assemblePersonalDebug
./gradlew :app:assembleStandardDebugAndroidTest
```

Android 테스트 보고서는 `app/build/reports/tests/testStandardDebugUnitTest/`, 화면은 `app/build/studio-screenshots/`에 생성한다. 빌드 로그/서명키/개인용 오버레이는 공개 소스에 포함하지 않는다.
