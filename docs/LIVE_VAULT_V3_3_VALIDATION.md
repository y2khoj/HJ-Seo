# Live Vault 3.3 preview 검증

3.3.0-preview / versionCode 13은 개인용 APK만 전달한다. 공개 standard APK는 3.2.1을 유지하며 SHA-256은 `f84486dc562454b88f533ddd14fc6538fee8651974aff2737d0cb817991ff948`이다. 개인용 3 ABI APK 빌드가 통과했으며 앱 ID `pro.livemic.studio.personal`, 버전 3.3.0-preview / versionCode 13과 기존 개발 서명 인증서 SHA-256 `6a61c39da7f13d3c7622c12068efd1e5bcb43bae2b7748821e2d2a9af169a139`를 확인했다. APK SHA-256은 `372da91e71e5d191e6fa319c22a6eb8296fef2831bd1b74326bebe5b503264bf`다.

개인용 전체 단위 검사 50개가 통과했다: Arrangement 10, Export 2, Import 6, Project 7, Redesign 1, Harmony 6, Language 1, Recording 9, 기존 Screens 8. 스키마 정렬 후 파일 교환 검사 8개도 다시 통과했다. 별도의 GPT-6 Astra 기능 리뷰는 조건부 승인했고 제시된 테스트 조건은 통과했다. 별도의 GPT-6 Astra 디자인 리뷰도 최종 렌더링을 확인해 통과했다. 360 너비·130% 글씨에서 세 소스 버튼, 복잡한 분할 코드 이름과 내부 ID가 없는 미리보기를 확인했다. 마지막 UI 검사 1개와 개인용 APK 재빌드도 통과했다.

한국어·영어 작곡 화면, 재생 중 코드, 트랙·곡 순서, 내보내기·가져오기 미리보기, 라이브·화음, 언어 설정과 선택을 기본 및 130% 글씨로 실제 Android View에서 렌더링했다. 360 너비의 영어 확대 화면도 포함한다. 총 42개 PNG를 `docs/screenshots/v3.3/`에 보관한다. 예: [한국어 작곡기](screenshots/v3.3/v33-ko-default-top.png), [영어 360 너비·130%](screenshots/v3.3/v33-en-small-large-top.png), [영어 확대 내보내기](screenshots/v3.3/v33-en-large-export.png), [한국어 확대 미리보기](screenshots/v3.3/v33-ko-large-import-preview.png), [언어 선택](screenshots/v3.3/v33-en-large-language-picker.png). 이는 테스트 환경 렌더링이며 실기기 캡처가 아니다.

파일 선택 중 화면이 재생성되어도 내보내기 요청은 캐시 식별자로 복원한다. 가져오기 파싱 도중 화면이 재생성되면 미리보기를 안전하게 취소하므로 파일을 다시 선택해야 한다. 가져오기는 최대 8 MiB의 UTF-8 일반 JSON을 검증하고, 녹음 중에는 적용을 막는다. 기존 프로젝트의 백업과 가져온 복사본은 각각 새 ID를 받으며 기존 프로젝트와 사용자 설정, 파트 길이 밖에 보관된 유효 음표도 보존한다. 언어 전환은 사용자 이름을 바꾸지 않고 실행 중 오디오를 중단하지 않는다. 영상 녹화 화면은 종료 후 갱신한다.

실제 기기의 오디오·마이크·Bluetooth·지연, Android 파일 선택기와 외부 AI의 응답 품질은 별도 확인이 필요하다.
