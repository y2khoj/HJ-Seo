# Live Vault · Figma UI handoff

최신 결과: [디자인 시안 v2와 화면 링크](DESIGN_RESULT.md). 아래 명세는 다음 디자인 결정과 함께 적용합니다.

- 마이크·모니터링 조작은 스튜디오 본문에만 배치하고 전역 하단에는 오디오 녹음 바를 둡니다.
- 내장 반주는 팝·재즈·락·Jpop·Kpop 곡 목록으로 구성합니다. 실제 음원과 권리 확보는 별도입니다.
- 내 파일 제목 옆에 작은 지원 형식을 표시하고 최근 불러온 파일 목록을 제공합니다.
- 영상 촬영 시작 시 오디오도 시작하며, REC 타이머·코너 전후면 전환·일시정지/재개·중단/저장을 제공합니다. 영상과 오디오는 함께 일시정지하고 함께 저장합니다.

## 작업의 성격

Android 네이티브 앱의 최종 UI를 설계한다. UI 기준은 사용자가 지정한 `MainActivity.java`와 `StudioTheme.java`이다. 기존 디자인 v2 화면 순서를 유지한다. 앱 전체를 WebView로 다시 만들 필요는 없다. 현재 SDK는 Java Views를 사용하지만 추후 Compose/Kotlin으로 화면을 교체할 수 있다. 오디오 C++ 엔진은 그대로 유지할 수 있다.

## 우선 설계할 프레임

기본 Android 휴대폰 412×915 및 작은 화면 360×800. 시스템 상태바/하단 제스처 영역을 고려한다. 터치 영역 최소 48dp, 본문 최소 14sp, 주요 버튼 16sp. 가로 스크롤로 중요한 녹음 버튼을 숨기지 않는다.

### 01. 스튜디오

- 상단: Live Vault
- 스튜디오 스크롤 본문: 마이크 ON/OFF, 모니터링 긴급 음소거
- 장치 상태: 이어폰 연결, 마이크 권한, 샘플레이트, 출력 버퍼 값
- 입력 레벨, 피크 경고, 보컬 상태 표시
- 마이크 게인 / 반주 볼륨을 구분한 믹서
- 프리셋: 노래방 / 발라드 / 콘서트 홀 / 팝 보컬 / 대성당 / 생목소리
- 고급 효과는 접을 수 있는 패널 또는 바텀시트
- 리버브 공간·양·잔향, 딜레이 양·간격·반복, Low/Mid/High, 컴프레서
- 입력 장치 선택

### 02. 반주

- 모드: 내장 밴드 / 내 파일 / YouTube
- 내장 반주: 팝·재즈·락·Jpop·Kpop 장르와 곡 목록, 재생/정지, BPM, ±6 키, 메트로놈
- 파일: 제목 옆 작은 지원 형식, 가져오기, 최근 불러온 목록, 선택한 파일명, 재생/일시정지
- YouTube: URL 또는 검색, 외부 열기, 녹음에 포함되지 않는다는 설명
- 후속 임베드 설계 시 로딩 / 차단 / 재시도 / 외부 열기 상태를 준비

### 03. 실시간 MIDI

- 현재 노트와 옥타브를 가장 크게 표시
- 센트 미터 -50 → 0 → +50, 주파수 보조 표시
- 최근 12초 피아노 롤, 음정 감지 대기 상태
- 신스 더블링 토글, MIDI 저장, 채보 지우기
- 녹음 중 채보 지우기 금지

### 04. 카메라

- 권한 미허용 상태와 허용 버튼
- 전면/후면 카메라 미리보기; 전면 미리보기는 거울 방향
- 전면/후면 전환, 이어폰/마이크 상태, 소형 레벨 표시
- 영상 촬영 시작 시 오디오 녹음도 자동 시작
- 상단 REC 타이머, 코너 전면/후면 전환, 영상·오디오 동시 일시정지/재개와 중단/저장
- 촬영 중 화면 이탈 시 저장 확인 또는 저장 안내
- 촬영 종료 뒤 MP4 결합 진행 상태(다른 녹음 시작 방지)
- 카메라 실패 시 오디오 스튜디오로 돌아가는 경로
- MP4 결합 실패 시 WAV/MIDI는 보존됐다는 안내

### 05. 보관함

- 빈 상태: 첫 녹음으로 연결
- 테이크 이름, 날짜, 길이, 오디오/영상 구분
- 재생/정지, WAV/MID/MP4 파일 저장, 공유, 이름 변경, 삭제
- 삭제 확인과 저장 오류 상태
- 영상 인코딩 진행 중 상태

## 전역 녹음 바

모든 화면에서 오디오 녹음/정지와 타이머를 볼 수 있게 한다. 카메라 영상 녹화와 오디오만 녹음을 구분한다. 녹음 중 버튼과 마이크 끄기를 혼동하지 않게 한다. 마이크 OFF 상태에서 녹음을 누르면 권한/마이크 켜기 경로를 안내한다.

## 필요한 컴포넌트와 variants

- MicPower: off / requesting / live / denied / disconnected
- Monitor: off / on / unavailable(no wired output)
- RecordButton: idle / recording / preparing / finalizing / error
- LevelMeter: silence / normal / peak
- PresetCard: default / selected
- EffectControl: default / disabled / dragging
- SourceTab: selected / default
- PitchReadout: waiting / tracking / weak signal
- CameraStage: permission / loading / preview / recording / failed
- TakeCard: audio / video / processing / recovery warning
- Toast / confirm dialog / bottom sheet / empty state

## 초기 시각 토큰 제안

최종 디자인에서 변경해도 된다. 토큰 이름은 유지하면 구현에 편리하다.

| 토큰 | 값 | 용도 |
| --- | --- | --- |
| background | #101411 | 앱 바탕 |
| surface | #1C231E | 카드·시트 |
| surfaceRaised | #27312A | 눌림·선택 보조 |
| accent | #D0F56B | 주요 액션·활성 |
| textPrimary | #F0F3ED | 기본 텍스트 |
| textSecondary | #9BA99C | 보조 정보 |
| danger | #FF7D7D | 녹음·에러·클리핑 |
| border | #334036 | 구분선 |

간격: 4, 8, 12, 16, 24, 32dp. 모서리: 8/14/24dp. 숫자와 타이머는 고정폭 글꼴을 권장한다. 배경 장식보다 레벨·음정·녹음 상태의 가독성을 우선한다.

## 코드 연결점

| 설계 영역 | 현재 파일 |
| --- | --- |
| 전체 화면/탭/권한/컨트롤 | `MainActivity.java` |
| 레벨 표시/피아노 롤 | `StudioVisual.java` |
| 카메라 미리보기·녹화 | `CameraController.java` |
| 녹음 시작/종료 | `RecordingManager.java` |
| 테이크·파일 목록 | `TakeStore.java` |
| 백그라운드 마이크 알림 | `AudioService.java` |
| 오디오 파라미터/채보 인터페이스 | `NativeAudio.java` |

경로의 공통 prefix: `app/src/main/java/pro/livemic/studio/`.

## 설계에서 피할 표현

- 실측하지 않은 `0ms`, `무지연`, `하드웨어 가속` 보장
- `오류 0%`, YouTube 차단 우회 보장
- 아직 없는 APK 빌드/설치 센터; 이 앱 자체가 APK다
- 영상을 저장하기 전에 성공 완료 상태 표시
- 카메라 촬영 중이라는 사실을 숨기는 UI

## 추가: 백그라운드 예약 녹음

- 녹음 바의 `⋯` 버튼에 시작 설정: 즉시 / 3초 / 5초 / 10초.
- 상태: idle → permission → preparing → countdown → recording → saving → saved/error.
- countdown: 남은 초, 다른 앱으로 이동 안내, 예약 취소.
- 알림: 카운트다운 / 녹음 타이머 / 취소 또는 저장 / 마이크 끄기.
- 배포용: 마이크 소스만 표시.
- 개인용: `마이크 + 다른 앱 소리` 토글, 파일에 넣을 외부 소리 게인, Android 권한 설명.
- 개인용 녹음 상태: 외부 소리 입력 중 / 외부 소리 신호 없음. 무음만으로 차단 여부를 확정하지 말 것.
- 개인용은 공식 Android 캡처 허용 범위만 지원하며 화면 녹화는 수행하지 않음.
- 카메라 영상 녹화는 화면을 유지하는 기존 흐름이며 예약 녹음 바는 오디오 전용.

## 추가: 스피커 출력

- 출력 모드 시트: 이어폰 / 휴대폰 스피커 / 연결된 블루투스 스피커 선택.
- 스피커 입력은 휴대폰 마이크. 블루투스 전송 지연 안내.
- 낮은 시작 게인(18%), 별도 모니터링 게인(최대 65%)과 시스템 볼륨 구분.
- 보호 상태: normal / notch suppression / protection muted / device disconnected.
- 자동 음소거 후 사용자 액션으로만 낮은 출력으로 재시작. 녹음은 계속된다는 설명.
- 지원 장치 AEC 활성 상태 표시. 하울링 완전 제거/무지연 표현 금지.
- 녹음·예약 중 출력 경로 변경 비활성.
