# Live Vault 개발용 APK

`Live-Vault-standard.apk`: 라이브볼트(Live Vault), standard 마이크 전용, 2.1.0-preview, Android 9 이상.

승인된 이중 원호 마이크 벡터로 런처와 상단 로고를 변경했습니다. Noto Sans KR·Roboto Mono를 포함하고 하단을 3탭으로 합치고 레이턴시·녹음 싱크·스케일 연습을 추가했습니다. 즉시·3·5·10초 백그라운드 예약 녹음, 스피커 출력·하울링 보호를 유지합니다. 다른 앱 오디오 캡처 구현과 권한은 포함하지 않습니다.

2.1에서는 상단의 승인 로고, 눈금형 슬라이더, 금속 다이얼, 아이콘 3탭, 반주 미니 플레이어, 파형·MIDI 보관함 카드와 4개 항목의 전체 메뉴를 적용했습니다. 실제 Android 뷰 렌더링은 [UI_REFRESH.md](../docs/UI_REFRESH.md)에서 확인하세요.

SHA-256은 `Live-Vault-standard.apk.sha256`에 기록합니다. 개발용 debug 서명이며 1.2 체크포인트와 앱 ID·서명이 같아 해당 버전을 업데이트할 수 있습니다. 개인용은 별도 앱 ID이며 공개 저장소에 올리지 않습니다.

`LiveMic-Pro-checkpoint.apk`와 `LiveMic-Pro-design-v2.apk`는 이전 버전 기록입니다. 이전 design-v2 APK는 다른 개발 키를 사용하므로 교체 전에 테이크를 내보내세요.

검증 범위는 `docs/STATUS.md`, 브랜드 적용은 `docs/BRANDING.md`를 확인하세요.
