Live Vault 리디자인 · 선택 화면 확장판

총 28개 화면: 핵심 화면 6개 + 선택·설정·확인 화면 22개.
현재 Figma 연동은 Starter 호출 한도, 브라우저는 Site Unavailable로 기존 Figma 파일에 직접 반영하지 못했습니다.
이 파일은 편집 요소로 구성한 SVG 디자인 컨셉이며 실제 Android 실행 화면이나 Figma 원본 파일이 아닙니다.
같은 변경 브랜치에 앱 UI 구현이 포함되어 있으나 이 시안과 모든 화면이 동일한 것은 아닙니다.

가져오기
Figma Design에서 숫자로 시작하는 SVG를 캔버스로 끌어놓으세요. 각 화면은 390×844입니다.
전체 비교용 파일은 LiveVault_redesign_board.svg 및 LiveVault_options_board_1~4.svg입니다.
Noto Sans KR / Roboto Mono를 권장하며, 가져오기 환경에 따라 텍스트가 대체되거나 벡터로 바뀔 수 있습니다.

공통 규칙
- 기본 Android AlertDialog 대신 같은 차콜색 커스텀 시트·확인창을 사용하도록 제안합니다.
- 일반 선택: 라임 라디오와 경계 / 선택 완료: 라임 단색 버튼.
- 삭제와 지우기: 붉은색 버튼, 대상과 영향 범위를 명시.
- 녹음·예약 중 변경 불가: 같은 자리에 비활성 상태로 표시하며 이유를 안내.
- 단위와 최대값을 함께 표시하고 소리 입력·실시간 출력·녹음 상태를 구분.
- 최소 터치 영역 48dp, 한국어 본문 14sp 권장. 실제 작은 화면·확대 글꼴 검증 필요.
- Android 자체의 권한·파일 선택·공유 창은 OS 테마를 따릅니다. 앱 내부 UI와 구분해야 합니다.

화면 목록
01_live_ready — 라이브 · 녹음 준비
02_live_recording — 라이브 · 녹음 중
03_backing — 반주 선택과 재생
04_library — 보관함 · 재생 중
05_practice — 보컬 연습
06_camera — 영상 녹화 준비
07_output_route — 출력 장치 선택
08_vocal_presets — 보컬 프리셋
09_record_options — 녹음 예약과 저장
10_audio_settings — 오디오 설정
11_effects — 상세 보컬 효과
12_reverb_space — 리버브 공간 선택
13_input_device — 마이크 입력 선택
14_bluetooth_device — 블루투스 기기 선택
15_speaker_protection — 스피커 출력과 보호
16_practice_mode — 보컬 연습 방식
17_practice_pattern — 음계와 속도
18_start_note — 시작음 전체 선택
19_backing_source — 반주 소스 선택
20_backing_style — 내장 반주 스타일
21_take_actions — 저장 테이크 메뉴
22_rename_take — 테이크 이름 변경
23_delete_take — 테이크 삭제 확인
24_file_error — 파일 불러오기 오류
25_video_exit — 영상 녹화 중 이동 확인
26_practice_results — 보컬 연습 결과
27_recent_backing_actions — 최근 반주 메뉴
28_clear_midi — 채보 지우기 확인

예시 파일·기기·시간·정확도·파형은 시각 설계용 데이터입니다.
출력 버퍼 표시는 실제 왕복 지연이나 Bluetooth 지연 제거를 의미하지 않습니다.
외부 앱 오디오는 배포용에서 직접 포함되지 않는다는 안내를 유지했습니다.
상호작용, 상태 전이, 스크롤과 보정 기능은 별도 구현·검증이 필요합니다.
