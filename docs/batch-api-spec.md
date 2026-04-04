# API 명세서 — 배치

## 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `http://localhost:8080` |
| Content-Type | `application/json` |

---

## 1. 배치 수동 실행

```
POST /api/batch/run?itemCategoryCode={코드}&regDay={날짜}
```

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| itemCategoryCode | N | 200 | 품목 카테고리 코드 |
| regDay | N | 오늘 날짜 | 조회 날짜 (yyyy-MM-dd) |

**KAMIS 품목 카테고리 코드**

| 코드 | 분류 |
|------|------|
| 100 | 식량작물 |
| 200 | 채소류 |
| 300 | 특용작물 |
| 400 | 과일류 |
| 500 | 축산물 |
| 600 | 수산물 |

**응답 예시 (성공)**

```json
{
  "success": true,
  "message": "배치 실행 완료",
  "jobId": 1,
  "status": "COMPLETED",
  "startTime": "2024-01-15T10:30:00",
  "endTime": "2024-01-15T10:30:05",
  "itemCategoryCode": "200",
  "regDay": "2024-01-15",
  "mockMode": false
}
```

**응답 예시 (실패)**

```json
{
  "success": false,
  "message": "배치 실행 실패: KAMIS API 호출 실패"
}
```

---

## 2. 배치 실행 이력 조회

```
GET /api/batch/status
```

**응답 예시**

```json
{
  "success": true,
  "count": 2,
  "mockMode": true,
  "apiConfigured": false,
  "data": [
    {
      "jobInstanceId": 2,
      "jobName": "kamisPriceJob",
      "status": "COMPLETED",
      "startTime": "2024-01-15T10:35:00",
      "endTime": "2024-01-15T10:35:03",
      "exitCode": "COMPLETED",
      "params": {
        "itemCategoryCode": "200",
        "regDay": "2024-01-15"
      }
    }
  ]
}
```

---

## 3. API 설정 확인

```
GET /api/batch/config
```

**응답 예시**

```json
{
  "apiConfigured": false,
  "mockMode": true,
  "baseUrl": "https://www.kamis.or.kr/service/price/xml.do",
  "certKeySet": false,
  "certIdSet": false
}
```

---

## 4. KAMIS 외부 API 정보

### dailySalesList (일별 도매가격)

```
GET https://www.kamis.or.kr/service/price/xml.do
  ?action=dailySalesList
  &p_cert_key={인증키}
  &p_cert_id={인증ID}
  &p_returntype=json
  &p_item_category_code={카테고리코드}
  &p_regday={yyyy/MM/dd}
  &p_convert_kg_yn=N
```

### KAMIS API 응답 구조

```json
{
  "data": {
    "item": [
      {
        "itemcode": "111",
        "item_name": "배추",
        "kindcode": "01",
        "kind_name": "일반",
        "rank": "상품",
        "unit": "10kg",
        "day1": "2024/01/15",
        "dpr1": "12,000",
        "day2": "2024/01/14",
        "dpr2": "11,500",
        "day3": "2024/01/08",
        "dpr3": "11,000"
      }
    ]
  }
}
```

### Spring Batch 처리 흐름

```
KamisItemReader
  └── KamisApiService.fetchDailyPrices() 호출
  └── API 응답 → List<PriceItem> 반환
       ↓
KamisItemProcessor
  └── PriceItem → PriceData Entity 변환
  └── 가격 문자열 파싱 (콤마 제거)
  └── 날짜 정규화 (숫자만 추출 → yyyyMMdd)
       ↓
KamisItemWriter
  └── Upsert 처리 (중복 키: itemCode+kindCode+regDay)
  └── PriceDataRepository.save()
```
