# 라이브볼트 · Live Vault

앱 표시 이름은 `라이브볼트`, 개인용은 `라이브볼트 개인용`이며 상단 영문명은 `Live Vault`이다. 앱 ID `pro.livemic.studio` / `.personal`과 데이터 경로는 그대로 유지한다.

## 제공 이미지

사용자가 제공한 `22502.jpg`를 변경 없이 `app/src/main/res/drawable-nodpi/live_vault_logo.jpg`에 포함한다. 상단 ImageView는 비율을 유지하고 40dp 정사각형 안에 표시한다. Android 런처에는 같은 이미지를 사용하는 adaptive icon을 적용하며 최종 마스크 모양은 런처가 결정한다. 마이크 서비스 알림의 작은 아이콘은 Android의 단색 규격에 맞춘 기능 아이콘이다.

## 화면 기준

사용자가 지정한 `MainActivity.java`와 `StudioTheme.java`의 기존 카드·탭 구성을 유지한다. 스피커 설명과 출력 게인·보호 해제는 **보컬 입력 카드 → 출력 설정** 팝업으로 옮긴다. 녹음 예약은 하단 **⋯**에서 설정한다.

저장된 Figma 명세의 Noto Sans KR와 Roboto Mono를 폰트 리소스로 포함한다. 폰트 원본은 Google Fonts의 `ofl/notosanskr`·`ofl/robotomono`이며 SIL OFL 라이선스를 `docs/licenses`와 APK의 `assets/licenses`에 포함한다.

Figma 원본 조회는 계정 MCP 도구 한도로 제한되어 이번 변경에서 원본 화면과의 픽셀 단위 일치를 확인하지 못했다. Figma 파일의 디자인 노드는 수정하지 않았다. 앱 실행·APK 검증 결과는 STATUS.md를 확인한다.
