# batch.html 사용 가이드

`batch.html`은 Spring Boot 서버가 실행 중일 때 `http://localhost:8080/batch.html`로 접근합니다.

---

## 화면 구성

### 1. 상단 상태 표시줄

페이지 로드 시 `GET /api/batch/config`를 호출해 서버 연결 상태를 표시합니다.

| 표시 | 의미 |
|------|------|
| 노란 점 (깜빡임) | API 키 미설정 |
| 초록 점 | KAMIS API 키 설정됨 |
| 빨간 점 | 서버 연결 실패 |

---

### 2. API 설정 상태 카드

`GET /api/batch/config` 응답을 기반으로 아래 3가지 항목을 표시합니다.

- API 설정 여부
- cert-key 입력 여부
- cert-id 입력 여부

구현해야 할 응답 형식은 `batch-api-spec.md` 참고.

---

### 3. 배치 수동 실행 카드

카테고리와 날짜를 선택한 뒤 실행 버튼을 누르면 `POST /api/batch/run`을 호출합니다.

| 파라미터 | 설명 |
|----------|------|
| itemCategoryCode | 품목 카테고리 코드 (100~600) |
| regDay | 수집 기준일 |

실행 결과(성공/실패, jobId, status 등)를 화면에 표시합니다.

---

### 4. 실행 이력 카드

`GET /api/batch/status`를 호출해 최근 배치 실행 내역을 표시합니다.

각 항목에는 Job 이름, 실행 상태(COMPLETED/FAILED), 시작/종료 시각, 파라미터가 포함됩니다.

---

## 전체 호출 흐름

`batch.html`을 브라우저에서 열면 별도 조작 없이 아래 두 API가 **자동으로 호출**됩니다.

```
batch.html 로드
  ├─ GET  /api/batch/config   → 상태 표시줄 + 설정 카드 렌더링 (자동)
  └─ GET  /api/batch/status   → 실행 이력 카드 렌더링 (자동)

[▶ 실행] 클릭
  └─ POST /api/batch/run?itemCategoryCode=200&regDay=2024-01-15
       └─ 응답 결과 화면 출력 후 이력 자동 갱신
```

---

## index.html과의 연계

`batch.html`에서 배치를 실행해 DB에 데이터를 적재하면,
`index.html`의 **API 연동** 모드에서 날짜별로 조회할 수 있습니다.

```
batch.html → POST /api/batch/run → DB 적재
index.html → GET /api/prices?regDay=yyyyMMdd → 조회
```
