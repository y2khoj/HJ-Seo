# LiveMic Pro · 디자인 시안 v2

[Figma 디자인 열기](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=3-50)

사용자가 검토한 Android UI 시안입니다. 편집 가능한 화면·컴포넌트와 화면 이동 프로토타입은 위 Figma 파일에 있습니다. 이 문서는 최신 디자인과 네이티브 구현 범위를 구분합니다.

## 화면 바로가기

| 화면 | Figma |
| --- | --- |
| 스튜디오 | [412×915](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=3-50) · [360×800](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=4-26) |
| 반주 | [내장 반주](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=3-150) |
| 내 파일 | [최근 목록](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=4-496) · [작은 화면](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=9-543) |
| 실시간 MIDI | [음정·피아노 롤](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=3-213) |
| 카메라 | [촬영 준비](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=3-282) · [촬영 중](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=4-579) |
| 촬영 상태 | [일시정지](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=10-42) · [후면](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=10-80) · [저장 중](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=4-622) |
| 보관함 | [테이크 목록](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=3-319) |
| 상세 효과 | [효과·입력 장치](https://www.figma.com/design/GdF4HcoWrlJyqyHvp1m26R?node-id=4-438) |

## 최신 디자인 결정

- 전역에는 오디오 녹음·타이머 바만 고정합니다. 마이크와 모니터링 조작은 스튜디오의 스크롤 본문에 둡니다.
- 내장 반주는 팝·재즈·락·Jpop·Kpop 장르와 곡 목록으로 구성합니다.
- 내 파일에는 제목 옆 작은 지원 형식, 파일 가져오기, 최근 불러온 반주 목록을 표시합니다. 최근 목록은 시안용 예시 데이터입니다.
- 영상 촬영을 누르면 카메라 화면에서 영상과 효과 오디오를 함께 녹음하는 흐름입니다. 실제 시작은 권한과 장치 준비 성공 후 진행해야 합니다.
- 촬영 중 상단에 `●REC 00:25`, 코너에 전·후면 전환, 하단에 일시정지·재개와 중단·저장 버튼을 표시합니다.
- 영상 일시정지 시 오디오도 함께 일시정지하고 타이머를 유지합니다. 중단하면 영상·오디오를 함께 저장합니다. 촬영 중 탭 이탈 프로토타입은 저장 화면으로 연결합니다.

## 반주 수록 후보

| 장르 | 시안에 표시한 후보 | 제작 방향 |
| --- | --- | --- |
| 팝 | Auld Lang Syne, Amazing Grace | 전통곡의 신규 팝 편곡 |
| 재즈 | When the Saints Go Marching In, Careless Love | 전통곡의 신규 재즈 편곡 |
| 락 | Stadium Rock 01, Classic Rock 02 | 제작 예정 오리지널 반주 제목 |
| Jpop | 사쿠라 사쿠라, 토랸세 | 일본 전통곡의 신규 팝 편곡 |
| Kpop | 아리랑, 도라지타령 | 한국 전통곡의 신규 팝 편곡 |

현대 히트곡의 사용권을 확보하거나 반주 음원을 추가한 결과는 아닙니다. 전통곡도 배포 지역과 사용할 편곡·녹음의 권리를 확인한 뒤 새로운 반주를 제작해야 합니다. 현대 팝·Jpop·Kpop 히트곡은 라이선스 확보 후 추가할 수 있습니다.

## 시각 시스템과 검토

차콜·라임 색상 토큰 8개, 텍스트 스타일 3개, 기존 상태 컴포넌트 세트 10개를 사용합니다. 한국어는 Noto Sans KR, 타이머와 음정은 Roboto Mono입니다. 주요 화면은 412×915와 360×800으로 설계하고 작은 화면의 본문은 세로 스크롤합니다.

수정한 스튜디오·반주·파일·카메라와 일시정지 상태를 Figma 렌더링으로 확인했습니다. 작은 파일 화면에서 발견한 카드·타이머 너비 문제를 수정하고 텍스트가 부모 너비를 넘지 않는지 재확인했습니다. 실제 Android 기기에서의 사용성 검증은 아직 진행하지 않았습니다.

## 네이티브 구현 범위

Figma 파일은 화면 이동 프로토타입이며 실영상 대신 미리보기 영역을 표시합니다. 이제 별도로 Android Java Views와 개발 APK에 디자인을 적용했습니다. 실제 앱은 Camera2 미리보기와 녹음 엔진을 사용합니다.

구현 진입점은 `app/src/main/java/pro/livemic/studio/MainActivity.java`이며 공통 시각 요소는 `StudioTheme.java`, 실시간 센트 미터는 `PitchMeter.java`, 최근 반주 기록은 `RecentBackings.java`에 있습니다. 개발용 APK는 `artifacts/LiveMic-Pro-design-v2.apk`입니다.

최근 파일 URI 저장·재선택과 오류 안내, 영상·오디오 동시 시작/일시정지/재개, 전·후면 전환 경로를 구현했습니다. WAV의 정지 구간은 제외하고 MIDI도 같은 시간축으로 압축합니다. 실제 반주 음원 제작은 남아 있으며, 프런트·리어 전환 중 녹화 연속성과 방향·오디오 동기화는 장치별 검증이 필요합니다. 새 APK의 화면·실제 촬영 런타임 테스트는 아직 수행하지 못했습니다. 기존 기능 검증 현황은 [STATUS.md](STATUS.md)를 참고하세요.

## 예약 녹음·스피커 통합본

최신 통합 APK는 `artifacts/LiveMic-Pro-checkpoint.apk`입니다. 기존 디자인 v2와 최근 파일, 촬영 전환·일시정지 기능을 유지하고 시작 설정(즉시/3/5/10초), 출력 모드(이어폰/휴대폰/블루투스), 스피커 보호 상태와 낮은 출력 재시작을 추가했습니다. 스피커 기능의 긴급 대응을 위해 헤더에 출력 음소거를 추가했습니다. 이 추가 UI는 위 Figma 파일에 아직 반영하지 않았습니다.

기존 `LiveMic-Pro-design-v2.apk`와 통합본은 개발 서명이 다릅니다. 기존 v2 테이크를 보존하려면 파일을 내보내기 한 뒤 배포용 앱을 교체하세요. 개인용은 별도 앱 ID로 기존 v2와 함께 설치할 수 있습니다. 검증 범위는 최신 STATUS.md를 확인하세요.
