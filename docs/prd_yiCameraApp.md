# PRD: 샤오미 YI 액션 카메라 컨트롤러 앱 (YI Action Camera Controller)

> **문서 버전**: v1.2.0  
> **최종 수정일**: 2026-10-21  
> **패키지 명 (Application ID)**: `com.github.kimymym.yicamera`  
> **지원 타깃**: Android 8.0+ (API 26+) ~ Android 15 (API 35), 64비트(`arm64-v8a`) 전용 기기 완벽 지원  
> **대상 기기**: 샤오미 YI 액션 카메라 1세대 (Ambarella A7LS 칩셋)

---

## 1. 프로젝트 개요 및 배경

### 1.1 배경
샤오미 YI 1세대 액션 카메라는 Ambarella A7LS 프로세서를 탑재한 고가성비 액션캠으로 현재도 널리 활용되고 있으나, 샤오미 공식 앱(`sportscamera.apk`)은 **32비트(`armeabi-v7a`) 레거시 네이티브 C/C++ 라이브러리에 종속**되어 있습니다.  
최신 안드로이드 스마트폰(Android 14+, Galaxy S24/S25, Pixel 7/8/9 등)은 64비트 전용 시스템으로 32비트 바이너리 실행을 원천 차단하므로 공식 앱의 설치 및 구동이 불가능합니다.

### 1.2 목적
본 프로젝트는 비공개 독점 32비트 라이브러리 없이, **순수 Kotlin 및 Modern Android Development (MAD) 표준 기술 스택**을 바탕으로 카메라의 TCP 7878 JSON 소켓 제어, 저지연 RTSP 뷰파인더 스트리밍, 미디어 갤러리 탐색 및 초고속 다운로드, 시스템 설정을 제공하는 경량 오픈소스 컨트롤러 애플리케이션을 구축하는 것을 목적으로 합니다.

---

## 2. 시스템 통신 아키텍처 (System Architecture)

### 2.1 통신 구조도
카메라와 안드로이드 스마트폰 간의 통신은 Wi-Fi AP(192.168.42.1)를 매개로 3개의 독립적인 채널을 통해 수행됩니다.

```
┌────────────────────────────────────────────────────────┐                ┌───────────────────────────────────┐
│              안드로이드 클라이언트 앱                  │                │      샤오미 YI 액션 카메라        │
│           (com.github.kimymym.yicamera)                │                │          (192.168.42.1)           │
├────────────────────────────────────────────────────────┤                ├───────────────────────────────────┤
│ ① 제어 채널: TCP JSON 소켓 (YiSocketClient)           │ ── TCP 7878 ──►│ Ambarella JSON Command Server     │
│ ② 뷰파인더 채널: Media3 ExoPlayer RTSP                 │ ◄── RTSP 554 ──│ RTSP Live H.264 Stream (/live)    │
│ ③ 미디어 채널: HTTP File Server (OkHttp / Coil)        │ ◄── HTTP 80 ───│ Web Server (/DCIM/100MEDIA/)      │
└────────────────────────────────────────────────────────┘                └───────────────────────────────────┘
```

### 2.2 네트워크 환경 최적화
1. **인터넷 없는 Wi-Fi 소켓 바인딩 (Process Network Binding)**:
   * 안드로이드 OS는 인터넷 연결이 없는 Wi-Fi AP(액션캠)에 연결될 경우 모바일 데이터를 우선하여 통신 오류가 발생합니다.
   * `ConnectivityManager.bindProcessToNetwork()` API를 적용하여 앱의 모든 소켓 통신을 카메라 Wi-Fi 네트워크로 강제 바인딩합니다.
2. **Cleartext 트래픽 허용 (Network Security Config)**:
   * Android 9(Pie)부터 강화된 평문 HTTP 차단 정책을 대응하기 위해 사설 IP `192.168.42.1`에 대해 Cleartext 허용 정책(`network_security_config.xml`)을 수립합니다.
3. **Media3 ExoPlayer 저지연(Low-Latency) 튜닝**:
   * RTSP 엔드포인트: `rtsp://192.168.42.1/live` (H.264 Baseline, AAC)
   * `DefaultLoadControl`의 버퍼링 재생 임계값을 100~300ms로 최소화.
   * UDP 패킷 드롭 아티팩트 방지를 위해 RTP-over-TCP(`RtspMediaSource.Factory().setForceUseRtpTcp(true)`) 강제 적용.
4. **동영상 전용 내장 썸네일(`.THM`) 연동**:
   * Ambarella 하드웨어는 동영상 녹화 시 동일 디렉토리에 초소형 JPEG 썸네일(`.THM`)을 자동 생성합니다.
   * 고용량 원본 파일 파싱 없이 HTTP 엔드포인트(`http://192.168.42.1/DCIM/100MEDIA/{filename}.THM`)를 우선 요청하여 썸네일 로딩 지연을 해결합니다.

---

## 3. 화면 구성 및 사용자 인터랙션 플로우 (UI/UX)

```
┌─────────────────────────────────────────────────────────┐
│             화면 1: 카메라 연결 (ConnectScreen)         │
│  - 카메라 Wi-Fi(YD...) 감지, 안내 문구, 설정 바로가기   │
└────────────────────────────┬────────────────────────────┘
                             │ (Wi-Fi 감지 및 세션 토큰 획득 성공)
                             ▼
┌─────────────────────────────────────────────────────────┐
│               화면 2: 메인 제어 화면 (MainScreen)       │
│  ┌───────────────────────────────────────────────────┐  │
│  │ 공통 헤더: 배터리(%), SD카드 잔여량, 연결 상태     │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌─────────────────┬─────────────────┬───────────────┐  │
│  │ 탭 1: 프리뷰    │ 탭 2: 갤러리    │ 탭 3: 설정    │  │
│  │ (LivePreview)   │ (MediaGallery)  │ (Settings)    │  │
│  └─────────────────┴─────────────────┴───────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 세부 기능 명세 및 요구사항 (Functional Requirements)

### 4.1 카메라 연결 화면 (ConnectScreen)
1. **Wi-Fi AP 안내 및 상태 감지**:
   * 샤오미 YI 액션캠 1세대의 기본 Wi-Fi SSID 접두어는 **`YD...`**(또는 `YDXJ_...`)로 안내 문구를 명시합니다.
   * 사용자가 카메라 측면의 Wi-Fi 버튼을 눌러 파란 LED가 점멸/점등되면 스마트폰을 해당 AP에 연결하도록 가이드합니다.
   * 연결 가이드 2번 항목: `"스마트폰 Wi-Fi 설정에서 'YD...' 로 시작하는 카메라 Wi-Fi에 연결합니다."`
2. **시스템 Wi-Fi 설정 원터치 이동**:
   * [Wi-Fi 설정 열기] 버튼 클릭 시 시스템 Wi-Fi 설정(`Settings.ACTION_WIFI_SETTINGS`) 화면으로 인텐트 전환.
3. **자동 세션 연결 및 토큰 발급**:
   * 스마트폰이 카메라 AP에 연결됨이 감지되면 자동으로 TCP 7878 소켓을 생성하고 `{"msg_id": 257, "token": 0}` 명령을 전송하여 세션 토큰을 발급받습니다.
   * 토큰 발급 완료 즉시 메인 제어 화면으로 자동 진입합니다.

---

### 4.2 메인 제어 화면: 공통 상단 헤더 (Header Info Bar)
1. **배터리 잔여량 인디케이터**:
   * 주기적 상태 폴링(`msg_id: 13`)을 통해 배터리 퍼센트(%) 및 충전 여부 아이콘을 실시간 반영.
2. **SD 카드 저장 용량 표시**:
   * 잔여 용량 및 총 용량(`msg_id: 5`)을 계산하여 `XX.X GB / YY.Y GB` 형태로 텍스트 표시.
3. **연결 상태 표시 및 연결 해제**:
   * 녹색 활성 상태 점 및 연결 해제(Disconnect) 아이콘 제공.
   * 터치 시 연결 해제 확인 다이얼로그 표시 후 세션 종료(`msg_id: 258`) 및 초기 연결 화면으로 복귀.

---

### 4.3 탭 1: 실시간 라이브 프리뷰 (LivePreviewTab)
1. **초저지연 RTSP 실시간 뷰파인더**:
   * `rtsp://192.168.42.1/live` 스트림을 Media3 ExoPlayer Surface에 렌더링.
   * `vf_start` (`msg_id: 259, param: "none_force"`)를 전송하여 카메라 비디오 스트리밍 개시.
2. **3x4 구도 가이드 그리드**:
   * 우측 상단 그리드 아이콘 터치로 3x4 안내선 오버레이 표시/숨김 토글.
3. **영상 상하 반전 (Flip 180°)**:
   * **프리뷰 우측 상단 전용 토글 버튼 (`ScreenRotation`)** 제공.
   * 활성화 시 시안 컬러(`0xFF00E5FF`)로 인디케이터 강조 표시.
   * 토글 시 카메라 TCP 소켓으로 `msg_id: 2, type: "video_rotate", param: "on"` (또는 `"off"`)를 즉시 전송하여 카메라 하드웨어 레벨에서 영상 반전 반영.
4. **사진 / 동영상 모드 전환**:
   * 대형 세그먼트 스위치를 통해 사진(Photo, `capture`) 모드와 동영상(Video, `record`) 모드 간 전환.
   * 모드 전환 시 `msg_id: 2, type: "system_mode"` 전송.
5. **촬영 및 녹화 제어**:
   * **사진 모드**: 대형 원형 셔터 버튼. 셔터 터치 시 촬영 명령(`msg_id: 769`) 전송 및 촬영 햅틱/사운드 피드백.
   * **동영상 모드**: 녹화 시작(`msg_id: 513`) 시 붉은색 사각 정지 버튼으로 UI 전환 및 실시간 녹화 타이머(`REC 00:23`) 노출. 녹화 정지 터치 시 중지 명령(`msg_id: 514`) 전송.
6. **셔터 연타 및 중복 요청 방지 (Debounce / Cooldown)**:
   * 촬영/녹화 시작/중지 버튼 클릭 시 `isShutterProcessing` 플래그를 즉시 활성화하여 카메라로부터 소켓 응답(`rval`)이 수신될 때까지 추가 터치 차단.
   * 응답 수신 후 최소 1.5초간의 쿨다운 딜레이를 두어 하드웨어 상태 동기화 보장.
7. **녹화 중 화면 켜짐 유지 (`FLAG_KEEP_SCREEN_ON`)**:
   * 동영상 녹화가 진행되는 동안 스마트폰 화면이 절전 모드로 꺼지지 않도록 `FLAG_KEEP_SCREEN_ON` 플래그를 활성화하고, 녹화 중지 시 자동 해제.
8. **백그라운드 / 포그라운드 생명주기 복원**:
   * 앱이 백그라운드로 전환될 때 비디오 리소스를 일시 중지하고, 포그라운드로 복귀 시 소켓 세션 연결 유지 상태를 점검하여 RTSP 스트림을 자동으로 재연결.
   * 연결이 영구 단절된 경우 재연결 또는 대기화면 복귀 다이얼로그 표시.
9. **안전한 시스템 Back 키 처리**:
   * **녹화 중 Back 키 입력 시**: 즉시 종료되지 않고 "녹화가 진행 중입니다. 녹화를 중지하시겠습니까?" 확인 다이얼로그 표시.
   * **대기 중 Back 키 입력 시**: "카메라 연결을 해제하고 메인 화면으로 돌아가시겠습니까?" 확인 다이얼로그 표시 후 세션 정리 및 복귀.

---

### 4.4 SD 카드 오류(-28 / -14) 처리 및 포맷 가이드 정책
1. **오류 감지 메커니즘**:
   * 사진 촬영(`msg_id: 769`) 또는 동영상 녹화(`msg_id: 513`) 시 카메라가 비프음 3회를 울리며 응답 코드로 `rval: -28` (SD Card write error / File system mismatch) 또는 `rval: -14` (SD Card format required)를 반환하는 경우를 포착.
2. **사용자 가이드 다이얼로그 노출**:
   * 오류 감지 시 사용자가 스마트폰이나 PC에서 카드를 빼서 직접 포맷하지 않도록 안내하는 전용 다이얼로그 표시:
     * **제목**: `"SD 카드 포맷 안내"`
     * **내용**: `"카메라 파일 시스템 오류(-28)가 발생했습니다. 카메라의 안정적인 동작을 위해 [설정 > SD 카드 포맷]을 진행해 주세요."`
3. **설정 탭 즉시 이동 버튼 제공**:
   * 다이얼로그 내 **[설정으로 이동]** 버튼 클릭 시, 하단 네비게이션의 [설정] 탭(`selectedTab = 2`)으로 즉각 전환하여 사용자가 [메모리 관리] 섹션에서 카메라 자체 포맷을 즉시 실행할 수 있도록 유도.

---

### 4.5 탭 2: 미디어 갤러리 및 다운로드 (MediaGalleryTab)
1. **하이브리드 미디어 탐색 엔진**:
   * 1차: TCP 소켓 파일 목록 조회 명령(`msg_id: 1282, param: "/tmp/fuse_d/DCIM/100MEDIA -D -S"`)을 통해 전체 파일명, 크기, 촬영 일시를 JSON으로 일괄 수신.
   * 2차 (Fallback): 소켓 조회가 비어있거나 실패할 경우, 카메라 내장 HTTP 웹서버(`http://192.168.42.1/DCIM/100MEDIA/`)의 HTML 디렉토리 인덱스를 정규식으로 파싱하여 목록 자동 복원.
2. **동영상 전용 내장 썸네일(`.THM`) 초고속 로딩**:
   * Ambarella 카메라가 동영상 저장 시 자동 생성하는 `.THM` (초소형 고압축 JPEG 썸네일) 엔드포인트를 우선 요청:
     * `http://192.168.42.1/DCIM/100MEDIA/{filenameWithoutExtension}.THM`
   * 실패 시 `.thm` 소문자 요청 $\rightarrow$ 원본 URL Fallback으로 안전한 썸네일 스트리밍 파이프라인 유지.
3. **3열 상하 스크롤 그리드 및 UI 간소화**:
   * 3열 그리드 레이아웃.
   * **동영상 아이템**: 우측 하단에 `[▶ 01:23]` 형태의 단일 오버레이(재생 아이콘 + 동영상 재생 길이)만 표시하여 가독성 극대화.
   * **사진 아이템**: 불필요한 용량 텍스트 표시를 제거하고 깔끔한 썸네일 이미지 제공.
4. **선택 모드 전환 및 일괄 다운로드**:
   * 상단 툴바의 **새로고침(Refresh) 버튼 옆에 [선택 모드 전환] 버튼** 추가 (또는 아이템 롱프레스로 선택 모드 진입).
   * 선택 모드 진입 시 전체 선택/해제 체크박스 및 선택된 항목 개수 표시.
   * 1개 이상 선택 시 화면 하단 중앙에 플로팅 **[다운로드 (N개)]** 액션 버튼 노출.
5. **다운로드 진행률 표시 및 Scoped Storage 저장**:
   * 다운로드 실행 시 `DownloadProgressDialog`를 통해 현재 다운로드 중인 파일명, 개별 파일 진행률(%), 전체 진행률(N/Total)을 시각적으로 표시.
   * 최신 안드로이드 권장 규격인 **MediaStore API (Scoped Storage)**를 준수하여 공용 저장소에 저장:
     * 사진: `Environment.DIRECTORY_PICTURES/YiCamera` (`MediaStore.Images`)
     * 동영상: `Environment.DIRECTORY_MOVIES/YiCamera` (`MediaStore.Video`)
   * 저장 즉시 안드로이드 기본 갤러리 앱에 노출되도록 미디어 스캐닝 연동.
6. **전체화면 미디어 뷰어 (`FullscreenMediaViewer`)**:
   * 사진: 고해상도 원본 뷰어 및 핀치 투 줌/팬 제스처 지원.
   * 동영상: Media3 ExoPlayer를 활용한 실시간 스트리밍 재생. 하단 재생바(Seekbar)와 컨트롤러가 기기 하단 시스템 네비게이션 바를 침범하지 않도록 Window Insets(`navigationBarsPadding`) 완벽 적용.

---

### 4.6 탭 3: 카메라 및 Wi-Fi 설정 (SettingsTab)
1. **카메라 지원 목록 동적 쿼리 (`msg_id: 3`)**:
   * `get_all_settings` 명령을 통해 카메라 펌웨어가 현재 지원하는 실제 파라미터 및 선택 가능한 옵션 배열(`options`)을 실시간 조회하여 드롭다운에 매핑.
2. **촬영 설정 (Capture Settings)**:
   * 동영상 해상도 및 프레임레이트 (`video_resolution`: 예: `1920x1080 60P 16:9`, `1920x1080 30P 16:9`, `1280x720 120P 16:9` 등).
   * 사진 해상도 (`photo_size`: 예: `16M (4608x3456 4:3)`, `13M`, `8M` 등).
   * 렌즈 왜곡 보정 (Lens Distortion Correction, `warp_enable`: `on` / `off`).
   * **영상 상하 반전 (Flip)**: `video_rotate` 드롭다운 ("꺼짐 (기본)", "켜짐 (180°)") 제공.
3. **카메라 기기 설정 (System Settings)**:
   * 비프음 볼륨 (`buzzer_volume`: `high`, `low`, `mute`).
   * 비프음 멜로디 (`buzzer_ring`: `ring`, `mute`).
   * LED 인디케이터 모드 (`led_mode`: `all`, `front`, `off`).
   * 자동 절전 종료 (`auto_power_off`: `off`, `3m`, `5m`, `10m`).
4. **카메라 Wi-Fi 및 네트워크 설정 (`WifiConfigDialog`)**:
   * 카메라 현재 Wi-Fi 이름(SSID) 및 마스킹된 비밀번호 카드 표시.
   * [Wi-Fi 설정 변경] 버튼 터치 시 변경 다이얼로그 노출:
     * **Wi-Fi 이름 (SSID)**: 1~32자 텍스트 입력 (`type: "wifi_ssid"`).
     * **Wi-Fi 비밀번호**: 8자 이상 WPA2 규격 입력 (`type: "wifi_password"`), 비밀번호 보이기/숨기기(eye) 아이콘 지원.
     * **경고 안내**: "Wi-Fi 설정을 변경하면 카메라가 재시작되며 기존 연결이 끊어집니다." 문구 명시.
5. **메모리 및 저장공간 관리 (Storage Management)**:
   * SD 카드 전체 용량 및 사용량/잔여량 계산.
   * 시각적 용량 게이지 바(`LinearProgressIndicator`) 제공.
   * 상태 이상 알림 배너 (SD 카드 미삽입, 포맷 필요, 잔여 용량 부족 등).
   * **SD 카드 포맷 기능**:
     * 위험 경고 팝업 확인 후 카메라 포맷 명령(`msg_id: 4, param: "D:"`) 전송.
     * 포맷 완료 후 용량 갱신 및 완료 토스트 노출.
6. **시스템 정보 및 연결 해제**:
   * 펌웨어 버전, 하드웨어 모델(`Ambarella A7LS`), 시리얼 번호 확인.
   * 안전한 세션 연결 해제 버튼.

---

## 5. TCP 7878 JSON 통신 프로토콜 명세 (Protocol Specification)

### 5.1 패킷 구조
* **인코딩**: UTF-8 JSON 문자열
* **기본 요청 형식**:
  ```json
  {"msg_id": 257, "token": 0}
  {"msg_id": 2, "token": 1, "type": "video_resolution", "param": "1920x1080 60P 16:9"}
  ```
* **기본 응답 형식**:
  ```json
  {"rval": 0, "msg_id": 257, "param": 1}
  ```

### 5.2 주요 명령어(Message ID) 일람표

| 기능 | msg_id | type | param | 성공 응답 예시 | 비고 |
| :--- | :---: | :---: | :---: | :--- | :--- |
| **세션 시작 (토큰 발급)** | `257` | - | `null` | `{"rval":0, "param":1}` | 응답 `param`이 세션 토큰 |
| **세션 종료** | `258` | - | `null` | `{"rval":0}` | 세션 반납 및 해제 |
| **뷰파인더 스트림 시작** | `259` | - | `"none_force"` | `{"rval":0}` | RTSP 포트 554 개방 |
| **뷰파인더 스트림 중지** | `260` | - | `null` | `{"rval":0}` | RTSP 스트리밍 중지 |
| **사진 촬영** | `769` | - | `null` | `{"rval":0}` | 실패 시 `rval: -28` 등 에러 반환 |
| **동영상 녹화 시작** | `513` | - | `null` | `{"rval":0}` | 동영상 촬영 시작 |
| **동영상 녹화 중지** | `514` | - | `null` | `{"rval":0}` | 촬영 완료 및 파일 저장 |
| **전체 설정 조회** | `3` | - | `null` | `{"rval":0, "param":[...]}` | 지원 옵션 목록 및 현재 값 일괄 반환 |
| **개별 설정 조회** | `1` | `설정키` | `null` | `{"rval":0, "param":"value"}` | `wifi_ssid`, `wifi_password` 등 개별 조회 |
| **설정 값 변경** | `2` | `설정키` | `설정값` | `{"rval":0}` | `video_rotate`, `wifi_ssid`, `system_mode` 등 |
| **SD 카드 포맷** | `4` | - | `"D:"` | `{"rval":0}` | 카메라 자체 FAT32 포맷 수행 |
| **저장 용량 질의** | `5` | `"free"` / `"total"` | `null` | `{"rval":0, "param": 15200000}` | KB 단위 반환 |
| **배터리 상태 질의** | `13` | - | `null` | `{"rval":0, "param": 85}` | 배터리 잔여 퍼센트(%) |
| **미디어 파일 목록 조회** | `1282` | - | `"/tmp/fuse_d/DCIM/100MEDIA -D -S"` | `{"rval":0, "listing":[...]}` | 파일명, 크기, 시간 수신 |

### 5.3 프로토콜 에러 코드 (Error Codes)

| 에러 코드 (`rval`) | 명칭 | 원인 및 권장 처리 |
| :---: | :--- | :--- |
| `0` | `SUCCESS` | 정상 완료 |
| `-28` | `CARD_WRITE_ERROR` | SD 카드 쓰기 오류 / 파일시스템 불일치 $\rightarrow$ **[설정 > SD 카드 포맷 안내 다이얼로그]** 노출 |
| `-14` | `CARD_NEED_FORMAT` | 카드 포맷 필요 $\rightarrow$ **[설정 > SD 카드 포맷 안내 다이얼로그]** 노출 |
| `-4` | `INVALID_TOKEN` | 세션 토큰 만료 또는 유효하지 않음 $\rightarrow$ `msg_id: 257` 재연결 수행 |
| `-2` | `CARD_FULL` | SD 카드 저장 공간 부족 $\rightarrow$ 미디어 삭제 또는 백업 안내 |
| `-1` | `OPERATION_FAILED` | 명령 실행 거부 (녹화 중 설정 변경 등) $\rightarrow$ 상태 확인 후 재시도 |

---

## 6. 개발 및 기술 스택 명세 (Technical Stack)

* **플랫폼 및 타깃 SDK**:
  * Min SDK: `26` (Android 8.0 Oreo)
  * Target / Compile SDK: `35` (Android 15 Vanilla Ice Cream)
  * ABI 아키텍처: `arm64-v8a`, `armeabi-v7a`, `x86_64` (100% Kotlin 기반으로 64비트 전용 기기 완벽 지원)
* **언어 및 컴파일러**: Kotlin `2.0.21` (Kotlin DSL `.kts`)
* **UI 프레임워크**: Jetpack Compose (`androidx.compose.bom:2024.10.01`), Material 3 (`androidx.compose.material3:1.3.1`)
* **아키텍처 패턴**: Google Modern Android Development (MAD) Clean Architecture + MVVM + UDF (단방향 데이터 플로우)
* **비동기 및 동시성**: Kotlin Coroutines `1.9.0` & Flow (`StateFlow`, `SharedFlow`)
* **의존성 주입**: Dagger Hilt `2.51.1` (`hilt-android`, `hilt-navigation-compose`)
* **미디어 스트리밍**: AndroidX Media3 ExoPlayer RTSP `1.5.0` (`media3-exoplayer-rtsp`, `media3-ui`)
* **이미지 로딩 및 썸네일 캐싱**: Coil Compose `3.0.4` (`coil3:coil-compose`, `coil-network-okhttp`)
* **데이터 직렬화**: Kotlinx Serialization JSON `1.7.3`
* **네트워크**: Non-blocking TCP Socket (`java.net.Socket`), HTTP Client (`java.net.HttpURLConnection` / OkHttp)
* **저장소 및 파일**: Android Scoped Storage (`MediaStore` API)

---

## 7. 빌드, 배포 및 테스트 가이드

### 7.1 원터치 빌드 & 디바이스 설치 스크립트
프로젝트 루트 디렉토리의 PowerShell 스크립트를 통해 원터치로 클린 빌드, 설치, 실행이 가능합니다:

```powershell
powershell -ExecutionPolicy Bypass -File C:\devWorks\yicamera\build_and_install.ps1
```

* **수행 절차**:
  1. Gradle 8.11.1 Wrapper 환경 검증.
  2. `yiCameraApp:assembleDebug` 빌드 실행.
  3. ADB 연결 기기(예: Samsung Galaxy `SM-S942N`)로 `app-debug.apk` 설치 (`adb install -r`).
  4. 메인 액티비티 즉시 실행 (`com.github.kimymym.yicamera/.MainActivity`).

### 7.2 수동 Gradle 빌드
```powershell
cd C:\devWorks\yicamera\yiCameraApp
./gradlew assembleDebug
```
* **빌드 산출물 위치**:
  * `C:\devWorks\yicamera\yiCameraApp\app\build\outputs\apk\debug\app-debug.apk`
