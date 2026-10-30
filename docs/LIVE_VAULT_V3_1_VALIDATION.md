# Live Vault 3.1 검증 기록

대상은 `3.1.0-preview / versionCode 10`이다. [기능 명세](LIVE_VAULT_V3_1_SPEC.md) · [3.0 검증 이력](LIVE_VAULT_V3_VALIDATION.md).

- JDK 17, SDK 35, NDK 27.2 환경에서 standard/personal debug 및 standard instrumentation APK 빌드 통과. instrumentation은 빌드까지만 확인했다.
- Java/Robolectric 20개 통과: ComposerProjectTest 7개, HarmonySessionTest 6개, StudioScreenshotTest 7개. 화면 검사는 오디오 네이티브를 대체한 View 렌더링이다.
- 호스트 화음 검사: 44.1/48 kHz 입력 피치 변환의 3도·5도 주파수 성분, ±60센트 비브라토 기준음, 12키×5스케일, 4/8마디×3박자 카운트인·루프·취소·WAV·팬·뮤트와 실시간 할당 0 확인. 6/8의 90 BPM, 4마디 길이는 16/3초다.
- 기존 composer, native, schedule, v2 호스트 검사 통과 및 NDK arm64 엔진 구문 검사 통과.
- 두 APK의 앱 ID (`pro.livemic.studio`, `pro.livemic.studio.personal`), 버전 3.1.0-preview / code 10, 3개 ABI와 기존 인증서 SHA-256 `6a61c39da7f13d3c7622c12068efd1e5bcb43bae2b7748821e2d2a9af169a139` 일치를 확인했다. 음원 원본 해시도 일치하며 MediaProjection 권한은 personal에만 있다. APK에 서명키는 포함되지 않았다. standard 산출물을 갱신했고 personal은 비공개로 전달한다. arm64 JNI 함수 11개와 standard의 개인 캡처 클래스 제외 및 personal의 포함을 확인했다.
- 메타데이터 AtomicFile 백업 복구, 저장 중 설정 경합, 재생 중 undo, 저장 실패 복구, 오류 중복 제거 및 재시작 시 라이브 OFF를 검사했다.
- 차콜/네온 테마와 고정 조작부의 기본/130% 글씨 화면을 검토했다. 아래 이미지는 실제 Android View의 테스트 렌더링이며 실기기 캡처가 아니다.

| 화면 | 기본 글씨 | 130% 글씨 |
|---|---|---|
| 화음 | [기본](screenshots/v3.1/v3.1-default-harmony.png) | [확대](screenshots/v3.1/v3.1-large-harmony.png) |
| 레이어 | [기본](screenshots/v3.1/v3.1-default-harmony-layers.png) | [확대](screenshots/v3.1/v3.1-large-harmony-layers.png) |
| 녹음 취소 | [기본](screenshots/v3.1/v3.1-default-harmony-cancel.png) | [확대](screenshots/v3.1/v3.1-large-harmony-cancel.png) |
| 템포 | [기본](screenshots/v3.1/v3.1-default-tempo.png) | [확대](screenshots/v3.1/v3.1-large-tempo.png) |
| 악기 검색 | [기본](screenshots/v3.1/v3.1-default-search.png) | [확대](screenshots/v3.1/v3.1-large-search.png) |
| 작곡기 | [기본](screenshots/v3.1/v3.1-default-composer.png) | [확대](screenshots/v3.1/v3.1-large-composer.png) |
| 목소리 악기 | [기본](screenshots/v3.1/v3.1-default-voice.png) | [확대](screenshots/v3.1/v3.1-large-voice.png) |
| 트랙 | [기본](screenshots/v3.1/v3.1-default-tracks.png) | [확대](screenshots/v3.1/v3.1-large-tracks.png) |

2-head granular 피치 변환에는 포먼트 교정이 없다. 실제 마이크 음색과 화음 품질·지연, AAudio 마이크 합류·종료와 서비스 소유권 및 재시작, Bluetooth·영상·개인 캡처, 장시간 동작은 Android 실기기에서 아직 검증하지 않았다.
