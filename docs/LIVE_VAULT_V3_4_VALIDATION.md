# Live Vault 3.4 preview 검증

3.4.0-preview / versionCode 14 개인용 APK를 세 ABI로 빌드했다. 앱 ID `pro.livemic.studio.personal`과 기존 개발 서명 인증서 SHA-256 `6a61c39da7f13d3c7622c12068efd1e5bcb43bae2b7748821e2d2a9af169a139`를 확인했다. 개인용 APK SHA-256은 `795215bd3d5ffe8c7291cd186b509e9f26db3cdd82c22343683c58501bfaac63`다. 공개 standard APK는 3.2.1과 SHA-256 `f84486dc562454b88f533ddd14fc6538fee8651974aff2737d0cb817991ff948`을 유지한다.

개인용 전체 단위 검사 58개가 통과했다. 최종 문구 변경 후 라이브 화면 7개와 기존 화면 1개도 재실행해 통과했다. 노브는 기존 효과 범위에 맞춰 잔향 길이 0.3~6초, 에코 간격 70~700ms를 사용한다. 드래그 중 값 동기화, 스크롤과의 터치 처리, 키보드 및 TalkBack 범위 조작과 마이크·모니터 안전 경로를 검사했다.

독립적인 GPT-6 Astra 기능 리뷰는 제스처와 범위 수정 후 승인했고 디자인 리뷰도 최종 화면을 확인해 통과했다. 한국어·영어, 412dp 기본·130% 및 360dp 130%의 대기·노브·녹음·안전 상태를 실제 Android View로 렌더링한 PNG 24개가 있다. 예: [한국어 기본](screenshots/v3.4/v34-ko-412-default-idle.png), [한국어 확대 노브](screenshots/v3.4/v34-ko-412-large-knobs.png), [영어 360dp 확대](screenshots/v3.4/v34-en-360-large-idle.png), [영어 녹음](screenshots/v3.4/v34-en-412-large-recording.png), [안전 음소거](screenshots/v3.4/v34-en-360-large-safety.png).

이 이미지는 테스트 환경의 렌더링이며 실제 기기 캡처가 아니다. 마이크·Bluetooth·지연, 실시간 오디오 품질과 실제 기기의 터치 및 접근성 사용성은 별도 확인이 필요하다.
