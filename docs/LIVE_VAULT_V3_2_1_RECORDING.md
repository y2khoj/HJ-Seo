# Live Vault 3.2.1 preview: 녹음 소스 선택

3.2.1-preview / versionCode 12는 녹음 시작 시 불필요하게 Android 화면 공유 창이 열리는 문제를 수정한다. 기본 녹음은 마이크를 담고 앱 내 반주가 재생 중이면 함께 담는다. 이 경우 Android 화면 공유 권한은 요청하지 않는다.

개인용에서 다른 앱 소리를 포함하려면 녹음 버튼 옆 **⋯ → 녹음 설정**에서 이번 녹음에만 선택한다. 설정을 저장하는 시점에는 공유 창이 열리지 않는다. 녹음 시작을 누르면 선택이 소비되고 Android 권한을 요청한다. 권한을 취소하거나 거부하면 녹음을 시작하지 않으며 다음 녹음을 위해 다시 선택해야 한다. 기존에 저장된 다른 앱 캡처 선택은 제거한다. 권한 창이 열려 있는 동안 화면이 재생성되면 진행 중인 요청을 복원한다.

예약 중 안내는 실제 다른 앱 캡처를 선택한 경우에만 다른 앱으로 이동하라고 표시한다. 3.2의 편곡·보컬 루프 변경과 그 검증 이력은 [3.2 명세](LIVE_VAULT_V3_2_SPEC.md)와 [검증 기록](LIVE_VAULT_V3_2_VALIDATION.md)에 보관한다. 3.2.1의 빌드 및 실기기 확인 결과는 별도로 기록한다.

## 검증

서명된 standard·personal debug APK를 각각 arm64-v8a, armeabi-v7a, x86_64로 빌드했고 standard instrumentation APK도 빌드했다. 두 앱 ID는 기존과 같고 개발 서명 인증서 SHA-256 `6a61c39da7f13d3c7622c12068efd1e5bcb43bae2b7748821e2d2a9af169a139`는 기존과 일치한다.

두 flavor의 전체 단위 테스트는 각각 40개다. Standard는 개인용 전용 6개를 건너뛰고 34개 통과, personal은 40개 통과하여 총 74개 실행, 실패와 오류는 0개였다. 새 `RecordingSourceTest` 9개에는 기본 소스와 기존 설정 제거, 한 번만 선택, 권한 허용·거부와 화면 재생성, 요청하지 않은 권한 결과 및 화면 렌더링 검사가 포함된다. Standard에서는 개인용 전용 6개를 건너뛴다. 테스트 대체 클래스는 공개 소스에서 사용하는 shadow이며 개인용 캡처 구현은 별도 비공개 오버레이에 있다.

```sh
./gradlew :app:testStandardDebugUnitTest :app:testPersonalDebugUnitTest
```

서로 다른 GPT-6 Astra 기능·디자인 리뷰어가 각각 검토했다. 기능 리뷰는 소스와 요청 흐름을 승인했고 조건으로 제시한 테스트도 통과했다. 디자인 리뷰는 API 35 Robolectric의 네이티브 Skia 화면을 기본과 130% 글씨로 확인하고 통과했다. 기본 녹음과 앱 내 반주 설명, 이번 녹음에만 적용되는 선택과 권한 안내, 버튼 접근성 이름 및 조작 상태에 대한 지적을 반영했다.

화면 예: [기본 녹음](screenshots/v3.2.1/recording-source-standard-default-mic.png), [130% 일반 설정](screenshots/v3.2.1/recording-source-standard-large-options.png), [130% 개인용 선택](screenshots/v3.2.1/recording-source-personal-large-options-on.png), [130% 개인용 선택 후](screenshots/v3.2.1/recording-source-personal-large-armed.png). 이 이미지는 실제 앱 View의 테스트 렌더링이며 실기기 캡처가 아니다. Android의 실제 공유 대화상자와 마이크·다른 앱 오디오의 실기기 동작은 검증하지 않았다.
