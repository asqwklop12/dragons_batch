# API 명세서

## 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `http://localhost:8080` |
| Content-Type | `application/json` |
| 인증 | 없음 (로컬 개발용) |

---

## 1. 가격 데이터 API

### 1-1. 날짜별 가격 조회

```
GET /api/prices?regDay={yyyy-MM-dd}
```

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| regDay | Y | 조회 날짜 (예: 2024-01-15) |

**응답 예시**

```json
{
  "success": true,
  "count": 3,
  "message": "조회 성공",
  "data": [
    {
      "id": 1,
      "itemCode": "111",
      "itemName": "배추",
      "kindCode": "01",
      "kindName": "일반",
      "marketCode": "100",
      "marketName": "서울",
      "rankCode": "01",
      "rankName": "상품",
      "price": 12000,
      "unit": "10kg",
      "regDay": "2024-01-15",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

---

### 1-2. 품목명 검색

```
GET /api/prices/search?itemName={검색어}
```

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| itemName | Y | 품목명 (부분 일치, 예: 배추) |

**응답** — 1-1과 동일 구조

---

### 1-3. 최근 저장 데이터 조회

```
GET /api/prices/latest?limit={건수}
```

**Query Parameters**

| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| limit | N | 50 | 최대 조회 건수 |

**응답** — 1-1과 동일 구조

---

## 2. 배치 API

### 2-1. 배치 수동 실행

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

### 2-2. 배치 실행 이력 조회

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

### 2-3. API 설정 확인

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

## 3. KAMIS 외부 API 정보

### Base URL

```
https://www.kamis.or.kr/service/price/xml.do
```

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
    "condition": [...],
    "item": [
      {
        "itemcode": "111",
        "item_name": "배추",
        "kindcode": "01",
        "kind_name": "일반",
        "market_code": "100",
        "marketname": "서울",
        "rank_code": "01",
        "rank": "상품",
        "unit": "10kg",
        "day1": "2024/01/15",
        "dpr1": "12,000",
        "day2": "2024/01/14",
        "dpr2": "11,500",
        "product_cls_code": "01"
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
  └── 날짜 정규화 (/ → -)
       ↓
KamisItemWriter
  └── Upsert 처리 (중복 키 기준: itemCode+kindCode+marketCode+rankCode+regDay)
  └── PriceDataRepository.save()
```
