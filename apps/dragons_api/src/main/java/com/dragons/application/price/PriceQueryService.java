package com.dragons.application.price;

import com.dragons.interfaces.api.price.dto.PriceItemResponse;
import com.dragons.interfaces.api.price.dto.PriceListResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PriceQueryService {

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

  public PriceListResponse getPricesByDate(String regDay) {
    List<PriceItemResponse> items = PRICE_ITEMS.stream()
        .filter(priceItem -> priceItem.regDay().equals(regDay))
        .toList();
    return PriceListResponse.of(items);
  }

  public PriceListResponse searchPrices(String itemName) {
    String keyword = itemName.toLowerCase();
    List<PriceItemResponse> items = PRICE_ITEMS.stream()
        .filter(priceItem -> priceItem.itemName().toLowerCase().contains(keyword))
        .toList();
    return PriceListResponse.of(items);
  }

  public PriceListResponse getLatestPrices(Integer limit) {
    int safeLimit = Math.max(limit, 0);
    List<PriceItemResponse> items = PRICE_ITEMS.stream()
        .sorted(Comparator.comparing(PriceItemResponse::createdAt).reversed())
        .limit(safeLimit)
        .toList();
    return PriceListResponse.of(items);
  }
}
