# 백그라운드 예약 녹음 · 두 가지 빌드

## 버전 분리

| 빌드 | 앱 ID | 다른 앱 소리 | 공개 APK |
| --- | --- | --- | --- |
| standard | pro.livemic.studio | 기능/구현/권한 제외 | artifacts/LiveMic-Pro-checkpoint.apk |
| personal | pro.livemic.studio.personal | Android가 허용한 오디오 캡처 옵션 | 공개 저장소에 올리지 않음 |

두 앱은 나란히 설치할 수 있다. 보관함과 권한도 별개이다. Java 공통 UI와 오디오 엔진은 공유하지만, `CaptureSupport`는 flavor별 다른 소스에서 빌드한다. standard에는 MediaProjection 구현과 `FOREGROUND_SERVICE_MEDIA_PROJECTION` 권한이 없다. 서비스도 standard에서 캡처 요청을 거부한다.

## 사용 방법

1. 아래 녹음 바의 **시작 설정**에서 즉시 / 3초 / 5초 / 10초를 선택한다.
2. 개인용에서 반주를 함께 담으려면 **마이크 + 다른 앱 소리**를 켠다.
3. 녹음 버튼을 누르고 마이크 권한을 허용한다. 개인용 내부 오디오 모드는 Android의 공유/캡처 권한도 매번 허용해야 한다.
4. 카운트다운이 표시되면 YouTube, SoundCloud 등으로 이동해 재생한다.
5. 알림 권한을 허용했다면 LiveMic 알림에서 **예약 취소** 또는 **녹음 저장**을 누른다. 알림이 꺼져 있으면 앱으로 돌아와 중지한다. **마이크 끄기**는 녹음을 저장하고 마이크 서비스까지 종료한다.
6. 보관함에서 WAV와 MIDI를 저장하거나 공유한다.

기본 시작 시점은 즉시이고 기본 소스는 마이크다. 직접 선택한 예약 시간과 소스는 다음 실행에도 유지한다. 카메라 영상 촬영은 기존의 화면 내 녹화 흐름을 사용하며, 이번 예약은 오디오 녹음에 적용한다.

## 예약의 실제 의미

카운트다운 동안 오디오 장치와 캡처 권한을 준비하지만 WAV 테이크는 만들지 않는다. 예약은 Activity 대신 foreground service가 관리한다. 화면을 바꾸거나 Activity가 다시 만들어져도 예약이 사라지지 않는다. 화면 갱신은 Activity가 보일 때만 수행하며 마이크 모니터링 상태는 서비스가 유지한다. CPU wake lock은 예약/녹음 동안에만 잡고, 취소·저장·오류·서비스 종료 시 해제한다.

이는 재생 버튼을 해석하는 기능이 아니다. 재생 앱과 무관하게 지정 시간에 시작한다. 타이밍은 단조 증가하는 elapsed realtime 기준이며, 시스템 부하에 따라 실제 시작은 늦어질 수 있다. 외부 앱 UI 자동화, 알림 읽기, 접근성 권한은 사용하지 않는다.

## 개인용 내부 오디오

- Android 10(API 29) 이상에서만 동작한다.
- Android AudioPlaybackCapture / MediaProjection을 사용한다.
- 화면 프레임을 읽거나 저장하지 않고 오디오만 캡처한다.
- 사용자 동의 토큰을 재사용하지 않으며 매번 새 권한 요청으로 시작한다.
- LiveMic 자신의 UID는 제외하여 모니터링 보컬과 내장 반주가 중복 녹음되지 않게 한다.
- 다른 앱 소리는 별도 오디오 입력 스레드 → SPSC 버퍼 → 녹음 믹서로 전달한다.
- 캡처한 소리를 이어폰 출력에 다시 보내지 않는다. 원래 앱의 재생 소리가 그대로 들린다.
- 개인용 설정의 볼륨은 파일에 넣을 캡처 소리의 게인이며, 외부 앱에서 들리는 재생 볼륨은 바꾸지 않는다.
- MIDI는 계속 클린 마이크에서만 추출한다.
- 다른 앱 입력이 감지되지 않으면 앱/알림에 상태를 표시하고, 저장된 테이크에 경고를 남긴다.
- 공유/캡처 권한이 철회되거나 입력 장치가 실패하면 예약을 취소하거나 진행 중 파일을 저장하고 종료한다.

대상 앱이 `ALLOW_CAPTURE_BY_ALL`을 허용해야 한다. 앱이 캡처를 차단하거나 보호된 콘텐츠를 재생하면 오디오가 무음으로 전달될 수 있다. 무음과 캡처 차단을 확실히 구분할 수 없으므로 둘 중 하나로 단정하지 않는다. 이 기능으로 YouTube/SoundCloud의 모든 트랙 녹음을 보장하지 않는다.

마이크와 시스템 오디오의 실제 시간 정렬은 기기별 검증이 필요하다. 강제 종료, 재부팅, OS의 프로세스 종료 뒤에 예약/녹음을 자동 재시작하지 않는다. 30분 한도는 앱 화면과 관계없이 서비스에서 적용한다.

## 빌드

```sh
./gradlew :app:assembleStandardDebug :app:assemblePersonalDebug
```

출력:

- `app/build/outputs/apk/standard/debug/app-standard-debug.apk`
- `app/build/outputs/apk/personal/debug/app-personal-debug.apk`

공개 저장소에는 `app/src/personal/` 구현을 포함하지 않는다. 비공개 오버레이를 별도 전달하며, 그 폴더가 있을 때만 personal flavor가 활성화된다. 공개 checkout은 `./gradlew :app:assembleStandardDebug`로 빌드한다. 위의 두 버전 동시 빌드와 personal instrumentation 명령은 비공개 오버레이가 설치된 개발 환경에서만 사용할 수 있다.

공개 Git 저장소에 APK를 복사할 때는 **standard만** 사용한다. 개인용 APK는 `app/build/` 밖의 비공개 전달 경로에 보관한다.

## 검증

```sh
./scripts/test-native.sh
./scripts/test-schedule.sh
./gradlew :app:assembleStandardDebugAndroidTest :app:assemblePersonalDebugAndroidTest
```

기기 테스트 APK를 설치한 뒤 각 앱 ID에 맞는 instrumentation을 실행한다. 테스트는 마이크 권한을 부여하고 녹음 테이크를 생성하므로 전용 에뮬레이터/테스트 기기에서 실행한다.

```sh
adb shell am instrument -w pro.livemic.studio.test/pro.livemic.studio.BackgroundRecordingInstrumentation
adb shell am instrument -w pro.livemic.studio.personal.test/pro.livemic.studio.BackgroundRecordingInstrumentation
```

일반 기기 테스트는 내부 오디오 동의 창을 자동 허용하지 않는다. 개인용의 실제 시스템 오디오 입력, 지원 앱, 타이밍 정렬은 별도 확인이 필요하다.

공식 참고: [AudioPlaybackCapture](https://developer.android.com/media/platform/av-capture), [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types).
