package com.dragons.interfaces.api.price;

import com.dragons.application.price.PriceQueryService;
import com.dragons.interfaces.api.price.dto.GetPricesRequest;
import com.dragons.interfaces.api.price.dto.LatestPricesRequest;
import com.dragons.interfaces.api.price.dto.PriceListResponse;
import com.dragons.interfaces.api.price.dto.SearchPricesRequest;
import com.dragons.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prices")
@Tag(name = "Prices", description = "가격 데이터 조회 API")
@RequiredArgsConstructor
public class PriceApiController {

  private final PriceQueryService priceQueryService;

  @GetMapping
  @Operation(summary = "날짜별 가격 조회")
  public ApiResponse<PriceListResponse> getPricesByDate(@ModelAttribute GetPricesRequest request) {
    return ApiResponse.successResponse(priceQueryService.getPricesByDate(request.regDay()));
  }

  @GetMapping("/search")
  @Operation(summary = "품목명 검색")
  public ApiResponse<PriceListResponse> searchPrices(@ModelAttribute SearchPricesRequest request) {
    return ApiResponse.successResponse(priceQueryService.searchPrices(request.itemName()));
  }

  @GetMapping("/latest")
  @Operation(summary = "최근 저장 데이터 조회")
  public ApiResponse<PriceListResponse> getLatestPrices(@Parameter @ModelAttribute LatestPricesRequest request) {
    return ApiResponse.successResponse(priceQueryService.getLatestPrices(request.limit()));
  }
}
