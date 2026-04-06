package com.dragons.application.price;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragons.interfaces.api.price.dto.PriceListResponse;
import org.junit.jupiter.api.Test;

class PriceQueryServiceTest {

  private final PriceQueryService priceQueryService = new PriceQueryService();

  @Test
  void getPricesByDateFiltersByRegDay() {
    PriceListResponse response = priceQueryService.getPricesByDate("2024-01-15");

    assertThat(response.count()).isEqualTo(2);
    assertThat(response.data())
        .extracting(item -> item.itemName())
        .containsExactly("배추", "무");
  }

  @Test
  void searchPricesFindsMatchingItemName() {
    PriceListResponse response = priceQueryService.searchPrices("사과");

    assertThat(response.count()).isEqualTo(1);
    assertThat(response.data().getFirst().itemCode()).isEqualTo("211");
  }

  @Test
  void getLatestPricesAppliesRequestedLimitInCreatedAtOrder() {
    PriceListResponse response = priceQueryService.getLatestPrices(2);

    assertThat(response.count()).isEqualTo(2);
    assertThat(response.data())
        .extracting(item -> item.itemName())
        .containsExactly("양파", "사과");
  }
}
