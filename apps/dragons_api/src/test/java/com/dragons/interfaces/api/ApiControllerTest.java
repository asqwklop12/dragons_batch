package com.dragons.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.application.MonthlyPriceReadService;
import com.application.PriceReadService;
import com.support.MySqlContainerTestSupport;
import com.repository.JpaPriceDataRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.price.PriceData;
import model.price.PriceReadItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiControllerTest extends MySqlContainerTestSupport {

  private static final Pattern JOB_EXECUTION_ID_PATTERN = Pattern.compile("\"jobExecutionId\":(\\d+)");

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @LocalServerPort
  private int port;

  @Autowired
  private JpaPriceDataRepository jpaPriceDataRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @MockitoBean
  private PriceReadService priceReadService;

  @MockitoBean
  private MonthlyPriceReadService monthlyPriceReadService;

  @BeforeEach
  void setUp() {
    clearBatchMetadata();
    jpaPriceDataRepository.deleteAllInBatch();
    jpaPriceDataRepository.saveAllAndFlush(
        List.of(
            priceData("111", "배추", "01", "일반", "100", "서울", "01", "상품", 12000, "10kg", "2024-01-15", "2024-01-15T10:30:00"),
            priceData("112", "무", "01", "일반", "100", "서울", "01", "상품", 9800, "20kg", "2024-01-15", "2024-01-15T10:31:00"),
            priceData("211", "사과", "02", "부사", "200", "대구", "01", "상품", 28500, "10kg", "2024-01-16", "2024-01-16T09:10:00"),
            priceData("311", "양파", "01", "일반", "300", "부산", "02", "중품", 14300, "15kg", "2024-01-17", "2024-01-17T08:45:00")
        )
    );
    given(priceReadService.readItems(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(LocalDate.class)))
        .willReturn(List.of());
    given(monthlyPriceReadService.readItems(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(YearMonth.class)))
        .willReturn(List.of());
  }

  @Test
  void getPricesByDateReturnsExpectedData() throws Exception {
    HttpResponse<String> response = sendGet("/api/prices?regDay=2024-01-15");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"success\":true");
    assertThat(response.body()).contains("\"data\":{\"count\":2");
    assertThat(response.body()).contains("\"itemName\":\"배추\"");
  }

  @Test
  void getLatestPricesReturnsLimitedResults() throws Exception {
    HttpResponse<String> response = sendGet("/api/prices/latest?limit=2");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"data\":{\"count\":2");
  }

  @Test
  void searchPricesReturnsMatchingItem() throws Exception {
    HttpResponse<String> response = sendGet("/api/prices/search?itemName=사과");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"data\":{\"count\":1");
    assertThat(response.body()).contains("\"itemName\":\"사과\"");
  }

  @Test
  void runBatchReturnsBatchExecutionResult() throws Exception {
    LocalDate regDay = LocalDate.of(2024, 1, 15);
    given(priceReadService.readItems("200", regDay))
        .willReturn(
            List.of(
                new PriceReadItem("911", "테스트 배추", "01", "일반", "100", "서울", "01", "상품", 12345, "10kg", regDay),
                new PriceReadItem("912", "테스트 무", "01", "일반", "100", "서울", "01", "상품", 23456, "20kg", regDay)
            )
        );

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + "/api/batch/run?itemCategoryCode=200&regDay=2024-01-15"))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"success\":true");
    assertThat(response.body()).contains("\"message\":null");
    assertThat(response.body()).contains("\"jobExecutionId\":");
    assertThat(response.body()).contains("\"status\":\"COMPLETED\"");
    assertThat(response.body()).contains("\"mockMode\":false");
    assertThat(jdbcTemplate.queryForObject("select count(*) from BATCH_JOB_EXECUTION", Integer.class))
        .isEqualTo(1);
    assertThat(jpaPriceDataRepository.findAllByItemNameContainingIgnoreCaseOrderByCreatedAtDescIdDesc("테스트"))
        .extracting(PriceData::getItemCode)
        .containsExactlyInAnyOrderElementsOf(Set.of("911", "912"));
  }

  @Test
  void runMonthlyBatchReturnsBatchExecutionResult() throws Exception {
    YearMonth yearMonth = YearMonth.of(2024, 1);
    given(monthlyPriceReadService.readItems("200", yearMonth))
        .willReturn(
            List.of(
                new PriceReadItem("911", "테스트 배추", "01", "일반", "100", "서울", "01", "상품", 12345, "10kg", LocalDate.of(2024, 1, 15)),
                new PriceReadItem("912", "테스트 무", "01", "일반", "100", "서울", "01", "상품", 23456, "20kg", LocalDate.of(2024, 1, 16))
            )
        );

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + "/api/batch/run-monthly?itemCategoryCode=200&yyyy=2024&mm=01"))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"success\":true");
    assertThat(response.body()).contains("\"jobExecutionId\":");
    assertThat(response.body()).contains("\"status\":\"COMPLETED\"");
    assertThat(response.body()).contains("\"yyyy\":\"2024\"");
    assertThat(response.body()).contains("\"mm\":\"01\"");
    assertThat(jdbcTemplate.queryForObject("select count(*) from BATCH_JOB_EXECUTION", Integer.class))
        .isEqualTo(1);
    assertThat(jpaPriceDataRepository.findAllByItemNameContainingIgnoreCaseOrderByCreatedAtDescIdDesc("테스트"))
        .extracting(PriceData::getItemCode)
        .containsExactlyInAnyOrderElementsOf(Set.of("911", "912"));
  }

  @Test
  void runBatchPersistsExecutionMetadataInDatabase() throws Exception {
    LocalDate regDay = LocalDate.of(2024, 1, 15);
    given(priceReadService.readItems("200", regDay))
        .willReturn(
            List.of(
                new PriceReadItem("911", "테스트 배추", "01", "일반", "100", "서울", "01", "상품", 12345, "10kg", regDay)
            )
        );

    HttpResponse<String> firstResponse = sendPost("/api/batch/run?itemCategoryCode=200&regDay=2024-01-15");
    HttpResponse<String> secondResponse = sendPost("/api/batch/run?itemCategoryCode=200&regDay=2024-01-15");

    long firstExecutionId = extractJobExecutionId(firstResponse.body());
    long secondExecutionId = extractJobExecutionId(secondResponse.body());

    assertThat(secondExecutionId).isGreaterThan(firstExecutionId);
    assertThat(jdbcTemplate.queryForObject("select count(*) from BATCH_JOB_EXECUTION", Integer.class))
        .isEqualTo(2);
    assertThat(jdbcTemplate.queryForObject("select count(*) from BATCH_STEP_EXECUTION", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void getBatchStatusReturnsEmptyWhenHistoryTrackingIsDisabled() throws Exception {
    HttpResponse<String> response = sendGet("/api/batch/status");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"count\":0");
    assertThat(response.body()).contains("\"mockMode\":false");
    assertThat(response.body()).contains("\"apiConfigured\":true");
    assertThat(response.body()).contains("\"data\":[]");
  }

  @Test
  void getBatchStatusReturnsRecentExecutionHistory() throws Exception {
    LocalDate regDay = LocalDate.of(2024, 1, 15);
    given(priceReadService.readItems("200", regDay))
        .willReturn(
            List.of(
                new PriceReadItem("911", "테스트 배추", "01", "일반", "100", "서울", "01", "상품", 12345, "10kg", regDay)
            )
        );

    sendPost("/api/batch/run?itemCategoryCode=200&regDay=2024-01-15");
    sendPost("/api/batch/run?itemCategoryCode=200&regDay=2024-01-15");

    HttpResponse<String> response = sendGet("/api/batch/status");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"count\":2");
    assertThat(response.body()).contains("\"apiConfigured\":true");
    assertThat(response.body()).contains("\"jobName\":\"kamisPriceJob\"");
    assertThat(response.body()).contains("\"jobExecutionId\":");
    assertThat(response.body()).contains("\"status\":\"COMPLETED\"");
    assertThat(response.body()).contains("\"exitCode\":\"COMPLETED\"");
    assertThat(response.body()).contains("\"itemCategoryCode\":\"200\"");
    assertThat(response.body()).contains("\"regDay\":\"2024-01-15\"");
    assertThat(response.body()).doesNotContain("requestedAt");
  }

  @Test
  void getBatchStatusAppliesRequestedLimitUpToTwenty() throws Exception {
    LocalDate regDay = LocalDate.of(2024, 1, 15);
    given(priceReadService.readItems("200", regDay))
        .willReturn(
            List.of(
                new PriceReadItem("911", "테스트 배추", "01", "일반", "100", "서울", "01", "상품", 12345, "10kg", regDay)
            )
        );

    for (int index = 0; index < 21; index++) {
      sendPost("/api/batch/run?itemCategoryCode=200&regDay=2024-01-15");
    }

    HttpResponse<String> limitedResponse = sendGet("/api/batch/status?limit=2");
    HttpResponse<String> cappedResponse = sendGet("/api/batch/status?limit=999");

    assertThat(limitedResponse.statusCode()).isEqualTo(200);
    assertThat(limitedResponse.body()).contains("\"count\":2");
    assertThat(occurrencesOf(limitedResponse.body(), "\"jobName\":\"kamisPriceJob\"")).isEqualTo(2);

    assertThat(cappedResponse.statusCode()).isEqualTo(200);
    assertThat(cappedResponse.body()).contains("\"count\":20");
    assertThat(occurrencesOf(cappedResponse.body(), "\"jobName\":\"kamisPriceJob\"")).isEqualTo(20);
  }

  @Test
  void openApiDocsEndpointIsExposed() throws Exception {
    HttpResponse<String> response = sendGet("/v3/api-docs");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"title\":\"Dragons Batch API\"");
    assertThat(response.body()).contains("KAMIS 가격 조회 및 배치 실행 API 문서");
    assertThat(response.body()).contains("/api/prices");
    assertThat(response.body()).contains("/api/batch/run");
  }

  @Test
  void swaggerUiEndpointIsExposed() throws Exception {
    HttpResponse<String> response = sendGet("/swagger-ui.html");

    assertThat(response.statusCode()).isIn(200, 302);
  }

  @Test
  void getBatchConfigReturnsActualResolvedConfiguration() throws Exception {
    HttpResponse<String> response = sendGet("/api/batch/config");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"success\":true");
    assertThat(response.body()).contains("\"apiConfigured\":true");
    assertThat(response.body()).contains("\"mockMode\":false");
    assertThat(response.body()).contains("\"baseUrl\":\"https://www.kamis.or.kr\"");
    assertThat(response.body()).contains("\"certKeySet\":true");
    assertThat(response.body()).contains("\"certIdSet\":true");
  }

  private HttpResponse<String> sendGet(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + path))
        .GET()
        .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> sendPost(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + path))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private long extractJobExecutionId(String responseBody) {
    Matcher matcher = JOB_EXECUTION_ID_PATTERN.matcher(responseBody);
    assertThat(matcher.find()).isTrue();
    return Long.parseLong(matcher.group(1));
  }

  private int occurrencesOf(String source, String target) {
    return source.split(Pattern.quote(target), -1).length - 1;
  }

  private void clearBatchMetadata() {
    jdbcTemplate.update("delete from BATCH_STEP_EXECUTION_CONTEXT");
    jdbcTemplate.update("delete from BATCH_JOB_EXECUTION_CONTEXT");
    jdbcTemplate.update("delete from BATCH_STEP_EXECUTION");
    jdbcTemplate.update("delete from BATCH_JOB_EXECUTION_PARAMS");
    jdbcTemplate.update("delete from BATCH_JOB_EXECUTION");
    jdbcTemplate.update("delete from BATCH_JOB_INSTANCE");
    jdbcTemplate.update("update BATCH_STEP_EXECUTION_SEQ set ID = 0 where UNIQUE_KEY = '0'");
    jdbcTemplate.update("update BATCH_JOB_EXECUTION_SEQ set ID = 0 where UNIQUE_KEY = '0'");
    jdbcTemplate.update("update BATCH_JOB_INSTANCE_SEQ set ID = 0 where UNIQUE_KEY = '0'");
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  private PriceData priceData(
      String itemCode,
      String itemName,
      String kindCode,
      String kindName,
      String marketCode,
      String marketName,
      String rankCode,
      String rankName,
      int price,
      String unit,
      String regDay,
      String createdAt
  ) {
    return PriceData.create(
        itemCode,
        itemName,
        kindCode,
        kindName,
        marketCode,
        marketName,
        rankCode,
        rankName,
        price,
        unit,
        LocalDate.parse(regDay),
        LocalDateTime.parse(createdAt)
    );
  }
}
