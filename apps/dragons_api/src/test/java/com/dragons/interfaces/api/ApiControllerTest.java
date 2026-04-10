package com.dragons.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.application.PriceReadService;
import com.support.MySqlContainerTestSupport;
import com.repository.JpaPriceDataRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import model.price.PriceData;
import model.price.PriceReadItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiControllerTest extends MySqlContainerTestSupport {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @LocalServerPort
  private int port;

  @Autowired
  private JpaPriceDataRepository jpaPriceDataRepository;

  @MockitoBean
  private PriceReadService priceReadService;

  @BeforeEach
  void setUp() {
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
    assertThat(jpaPriceDataRepository.findAllByItemNameContainingIgnoreCaseOrderByCreatedAtDescIdDesc("테스트"))
        .extracting(PriceData::getItemCode)
        .containsExactlyInAnyOrderElementsOf(Set.of("911", "912"));
  }

  @Test
  void getBatchStatusReturnsEmptyWhenHistoryTrackingIsDisabled() throws Exception {
    HttpResponse<String> response = sendGet("/api/batch/status");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"count\":0");
    assertThat(response.body()).contains("\"mockMode\":false");
    assertThat(response.body()).contains("\"data\":[]");
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

  private HttpResponse<String> sendGet(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + path))
        .GET()
        .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
