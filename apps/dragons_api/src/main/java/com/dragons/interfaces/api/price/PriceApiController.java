package com.dragons.interfaces.api.price;

import com.dragons.interfaces.api.price.dto.GetPricesRequest;
import com.dragons.interfaces.api.price.dto.LatestPricesRequest;
import com.dragons.interfaces.api.price.dto.PriceItemResponse;
import com.dragons.interfaces.api.price.dto.PriceListResponse;
import com.dragons.interfaces.api.price.dto.SearchPricesRequest;
import com.dragons.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prices")
@Tag(name = "Prices", description = "가격 데이터 조회 API")
public class PriceApiController {

  private static final List<PriceItemResponse> PRICE_ITEMS = List.of(
      new PriceItemResponse(
          1L,
          "111",
          "배추",
          "01",
          "일반",
          "100",
          "서울",
          "01",
          "상품",
          12000,
          "10kg",
          "2024-01-15",
          "2024-01-15T10:30:00"
      ),
      new PriceItemResponse(
          2L,
          "112",
          "무",
          "01",
          "일반",
          "100",
          "서울",
          "01",
          "상품",
          9800,
          "20kg",
          "2024-01-15",
          "2024-01-15T10:31:00"
      ),
      new PriceItemResponse(
          3L,
          "211",
          "사과",
          "02",
          "부사",
          "200",
          "대구",
          "01",
          "상품",
          28500,
          "10kg",
          "2024-01-16",
          "2024-01-16T09:10:00"
      ),
      new PriceItemResponse(
          4L,
          "311",
          "양파",
          "01",
          "일반",
          "300",
          "부산",
          "02",
          "중품",
          14300,
          "15kg",
          "2024-01-17",
          "2024-01-17T08:45:00"
      )
  );

  @GetMapping
  @Operation(summary = "날짜별 가격 조회")
  public ApiResponse<PriceListResponse> getPricesByDate(@ModelAttribute GetPricesRequest request) {
    List<PriceItemResponse> items = PRICE_ITEMS.stream()
        .filter(priceItem -> priceItem.regDay().equals(request.regDay()))
        .toList();
    return ApiResponse.successResponse(PriceListResponse.of(items));
  }

  @GetMapping("/search")
  @Operation(summary = "품목명 검색")
  public ApiResponse<PriceListResponse> searchPrices(@ModelAttribute SearchPricesRequest request) {
    String keyword = request.itemName().toLowerCase();
    List<PriceItemResponse> items = PRICE_ITEMS.stream()
        .filter(priceItem -> priceItem.itemName().toLowerCase().contains(keyword))
        .toList();
    return ApiResponse.successResponse(PriceListResponse.of(items));
  }

  @GetMapping("/latest")
  @Operation(summary = "최근 저장 데이터 조회")
  public ApiResponse<PriceListResponse> getLatestPrices(@Parameter @ModelAttribute LatestPricesRequest request) {
    int safeLimit = Math.max(resolveLimit(request), 0);
    List<PriceItemResponse> items = PRICE_ITEMS.stream()
        .sorted(Comparator.comparing(PriceItemResponse::createdAt).reversed())
        .limit(safeLimit)
        .toList();
    return ApiResponse.successResponse(PriceListResponse.of(items));
  }

  private int resolveLimit(LatestPricesRequest request) {
    return request.limit() == null ? 50 : request.limit();
  }
}
