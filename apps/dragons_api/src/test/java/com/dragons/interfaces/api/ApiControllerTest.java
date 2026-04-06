package com.dragons.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Sql(
    statements = {
        "DELETE FROM price_data",
        "INSERT INTO price_data (id, item_code, item_name, kind_code, kind_name, market_code, market_name, rank_code, rank_name, price, unit, reg_day, created_at) VALUES (1, '111', '배추', '01', '일반', '100', '서울', '01', '상품', 12000, '10kg', '2024-01-15', '2024-01-15 10:30:00')",
        "INSERT INTO price_data (id, item_code, item_name, kind_code, kind_name, market_code, market_name, rank_code, rank_name, price, unit, reg_day, created_at) VALUES (2, '112', '무', '01', '일반', '100', '서울', '01', '상품', 9800, '20kg', '2024-01-15', '2024-01-15 10:31:00')",
        "INSERT INTO price_data (id, item_code, item_name, kind_code, kind_name, market_code, market_name, rank_code, rank_name, price, unit, reg_day, created_at) VALUES (3, '211', '사과', '02', '부사', '200', '대구', '01', '상품', 28500, '10kg', '2024-01-16', '2024-01-16 09:10:00')",
        "INSERT INTO price_data (id, item_code, item_name, kind_code, kind_name, market_code, market_name, rank_code, rank_name, price, unit, reg_day, created_at) VALUES (4, '311', '양파', '01', '일반', '300', '부산', '02', '중품', 14300, '15kg', '2024-01-17', '2024-01-17 08:45:00')"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ApiControllerTest {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @LocalServerPort
  private int port;

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
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + "/api/batch/run?itemCategoryCode=200&regDay=2024-01-15"))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"success\":true");
    assertThat(response.body()).contains("\"message\":\"배치 실행 완료\"");
    assertThat(response.body()).contains("\"status\":\"COMPLETED\"");
    assertThat(response.body()).contains("\"mockMode\":true");
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
}
