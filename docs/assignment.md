# 과제: KAMIS API + Spring Batch 연동

## 과제 목표

1. KAMIS(농산물유통정보) 외부 API를 Spring Boot에서 호출하는 방법을 이해한다
2. `FeignClient`를 사용한 선언형 HTTP 통신 패턴을 익힌다
3. Spring Batch의 Reader → Processor → Writer 구조(Chunk 기반 처리)를 이해하고 직접 구현한다
4. 외부 API 데이터를 DB에 저장하는 파이프라인을 설계하고 구현한다
5. `@ConfigurationProperties`를 활용한 설정 외부화를 실습한다

---

## 사전 준비

### 1. KAMIS API 키 발급

1. [KAMIS 공식 사이트](https://www.kamis.or.kr) 접속
2. 상단 메뉴 → **서비스 안내 → API 신청**
3. 회원가입 후 API 사용 신청
4. 승인 후 **인증키(p_cert_key)** 와 **인증ID(p_cert_id)** 발급
5. `application-local.yml`에 키 입력:

```yaml
kamis:
  api:
    cert-key: 발급받은_인증키
    cert-id: 발급받은_인증ID
    mock-mode: false
```

> 키 발급 전까지는 **Mock 모드**로 동작합니다. Mock 모드에서는 실제 API 대신 샘플 데이터를 사용하므로 배치 구조 학습은 가능합니다.

### 2. 로컬 환경 실행

```bash
# MySQL 실행
docker-compose up -d mysql

# Spring Boot 실행 (local 프로파일)
./gradlew bootRun --args='--spring.profiles.active=local'

# 브라우저에서 확인
open http://localhost:8080
```

---

## 과제 내용

### 단계 1 — 기존 코드 분석 (필수)

다음 파일을 읽고 각 클래스의 역할을 정리하세요.

| 파일 | 확인 포인트 |
|------|------------|
| `KamisFeignClient.java` | `@FeignClient` 선언 방식, `@RequestParam` 매핑 |
| `KamisApiService.java` | FeignClient 호출 방식, Mock 모드 분기 처리 |
| `KamisItemReader.java` | ItemReader 인터페이스 구현, 초기화 패턴 |
| `KamisItemProcessor.java` | 데이터 변환 로직, null 처리 |
| `KamisItemWriter.java` | Chunk 단위 DB 저장, Upsert 로직 |
| `BatchConfig.java` | Job/Step 빈 구성, chunk size 설정 |

**제출물**: 각 클래스에 대한 한국어 주석 추가 (코드 이해 확인)

---

### 단계 2 — 월별 가격 조회 기능 추가 (핵심 과제)

KAMIS API의 `monthlySalesList` 액션을 활용하여 월별 가격 데이터를 조회하는 기능을 추가합니다.

**API 엔드포인트**:
```
https://www.kamis.or.kr/service/price/xml.do
  ?action=monthlySalesList
  &p_cert_key={key}
  &p_cert_id={id}
  &p_returntype=json
  &p_yyyy={연도}
  &p_mm={월}
  &p_item_category_code={코드}
```

**구현 항목**:

1. `KamisFeignClient`에 `fetchMonthlyPrices` 메서드 추가
2. `KamisApiService`에 `fetchMonthlyPrices(String itemCategoryCode, String yyyy, String mm)` 메서드 추가
2. 월별 배치 Job 추가 (`kamisMonthlyPriceJob`)
3. `BatchController`에 `/api/batch/run-monthly` 엔드포인트 추가
4. 프론트엔드 `index.html`에 월별 조회 섹션 추가

---

### 단계 3 — 스케줄링 적용 (심화)

매일 자정에 전날 가격 데이터를 자동으로 수집하도록 스케줄러를 구현합니다.

```java
@Component
@RequiredArgsConstructor
public class KamisBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job kamisPriceJob;

    @Scheduled(cron = "0 0 1 * * *")  // 매일 새벽 1시
    public void scheduleDailyPriceFetch() {
        // 어제 날짜로 배치 실행
        // JobParameters에 날짜 포함
    }
}
```

**구현 항목**:
1. `KamisBatchScheduler` 클래스 작성
2. `@EnableScheduling` 설정 추가
3. 스케줄 주기를 `application.yml`에서 설정 가능하도록 외부화

---

### 단계 4 — 예외 처리 강화 (심화)

현재 배치 처리 중 특정 아이템에서 오류가 발생하면 전체 Step이 실패할 수 있습니다. 아래를 구현하세요.

1. `SkipPolicy` 구현 — 파싱 오류 발생 시 해당 아이템 건너뛰기
2. `ItemWriteListener` 구현 — 저장 성공/실패 로깅
3. Skip 횟수 모니터링 (`SkipCountExceeded` 시 알림)

```java
// 예시
.faultTolerant()
.skip(RuntimeException.class)
.skipLimit(10)
```

---

### 단계 5 — 데이터 시각화 (선택)

`index.html`에 차트를 추가하여 가격 추이를 시각화합니다.

- Chart.js (CDN) 사용
- 특정 품목의 날짜별 가격 변동 라인 차트 구현
- `/api/prices/search?itemName=배추`로 데이터 조회 후 차트 렌더링

---

## 참고 자료

- [KAMIS API 매뉴얼](https://www.kamis.or.kr/customer/board/board.do?boardId=notice)
- [Spring Batch 공식 문서](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [Spring Batch 아키텍처](https://docs.spring.io/spring-batch/docs/current/reference/html/index-single.html#springBatchArchitecture)
- [Spring Cloud OpenFeign 공식 문서](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
