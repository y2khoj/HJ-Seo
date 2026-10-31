# Live Vault 3.2 preview 검증 기록

3.2.0-preview / versionCode 11의 서명된 standard·personal debug APK를 각각 3개 ABI로 빌드했고 standard instrumentation APK도 빌드했다. 두 앱 ID는 기존과 같고 두 APK의 개발 서명 인증서 SHA-256은 `6a61c39da7f13d3c7622c12068efd1e5bcb43bae2b7748821e2d2a9af169a139`로 기존 인증서와 일치한다. SF2 체크섬도 기존과 같다. Standard APK SHA-256은 `321281dea622c92c6d6d6ef0a9abd670c4ddb88290af20ddbc221050e49d8795`이다.

전체 Gradle 빌드의 Java/Robolectric 검사는 31개 통과했다: Arrangement 10, Composer 7, Harmony 6, Screens 8. 이름의 유니코드 저장·복원, 스크롤 유지, 녹음 대상 선택 잠금 및 카운트인 표시를 포함한 마지막 테스트 재실행도 31개 전부 오류 없이 통과했다. 호스트 harmony, composer, native, schedule, v2와 기타 보이싱 parity 검사도 통과했다. 편곡 검사는 3,168개 코드·박자·템포 조합과 96개 기타 패턴·리듬·템포 조합을 확인하고, 기타 parity는 12개 근음과 11개 코드 종류의 132개 Java/네이티브 조합을 비교한다.

재실행에 사용하는 주요 명령:

```sh
./gradlew :app:testStandardDebugUnitTest
./scripts/test-harmony.sh
./scripts/test-composer.sh
./scripts/test-native.sh
./scripts/test-schedule.sh
./scripts/test-v2.sh
./scripts/test-guitar-parity.sh
./scripts/test-arrangement.sh
```

화면 PNG는 실제 Android View를 Robolectric API 35에서 412 × 893, 기본 및 130% 글씨 크기로 렌더링한 결과다. 오디오 장치는 테스트 대체를 사용하며 실기기 캡처가 아니다. 예: [기본 작곡기](screenshots/v3.2/review-default-composer-top.png), [130% 연주 설정](screenshots/v3.2/review-large-performance-sheet.png), [130% 카운트인](screenshots/v3.2/review-large-harmony-countin.png), [130% 녹음](screenshots/v3.2/review-large-harmony-recording.png). 나머지 화면도 같은 디렉터리에 있다. 두 개의 독립적인 GPT-6 Astra 디자인·기능 리뷰에서 확인한 사항을 수정했다. [독립 리뷰](LIVE_VAULT_V3_2_REVIEW.md).

[전후 비교 샘플 듣기](audio/v3.2/README.md). `scripts/test-arrangement.sh`는 실제 Java 스케줄러가 내보낸 이벤트를 C++ 오프라인 렌더러와 앱의 GeneralUser GS 1.44 SF2로 렌더링한다. 비교 전 기준은 `ad22bd5`의 스케줄러·리듬·음악 이론이며, 동일한 현재 렌더 엔진과 나머지 모델을 쓴다. 기본 프로젝트의 기타·베이스·드럼·뮤트된 건반(각 트랙 볼륨 70%), 90 BPM과 스타일 0/5/6을 사용한다. 44.1kHz 스테레오 PCM16 WAV로 출력한다. 정규화 전 피크는 순서대로 전 0.6602/0.6369/0.5318, 후 0.6832/0.6832/0.6822이며 클리핑은 없었다. FFmpeg 1회 패스 `loudnorm=I=-20:TP=-1.5:LRA=11`과 `-ar 44100 -c:a pcm_s16le`를 적용한 뒤 MP3로 변환했고 측정 통합 음량은 −20.2~−20.0 LUFS였다.

호스트의 기본 4트랙 계획 생성 벤치마크는 3회 준비 실행 후 5회 측정에서 8마디 11.8~49.5ms, 256마디 201.6~408.1ms였다. 이는 호스트 계획 생성 시간이며 Android 오디오 CPU 부하를 뜻하지 않는다. 최대 8트랙 프로젝트의 기기 성능은 측정하지 않았다. TSF 렌더는 여전히 샘플마다 호출된다. 샘플은 재현 가능한 비교 자료이며 자연스러움의 실청 평가가 아니다.

실제 목소리 품질, 마이크·유선·Bluetooth 라우팅과 지연, 피드백, 카메라 동기화 및 장시간 동작은 실기기에서 확인해야 한다. Android 오디오 CPU 벤치마크는 수행하지 않았다.
