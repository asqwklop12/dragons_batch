package com.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.dto.MarketPriceDailyResponse;
import com.dto.MarketPriceMonthlyResponse;
import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.Test;

class MarketPriceClientTest {

  @Test
  void dailyPriceResponseUsesTypedRecordsInsteadOfMap() {
    RecordComponent[] components = MarketPriceDailyResponse.MarketPriceDailyItem.class.getRecordComponents();

    assertThat(components).extracting(RecordComponent::getName)
        .contains("productNo", "itemName", "productClsCode", "categoryCode", "dpr1", "direction");
  }

  @Test
  void monthlyPriceResponseUsesTypedRecordsInsteadOfMap() {
    RecordComponent[] components = MarketPriceMonthlyResponse.MarketPriceMonthlyItem.class.getRecordComponents();

    assertThat(components).extracting(RecordComponent::getName)
        .contains("itemCode", "itemName", "kindCode", "marketCode", "dpr1", "productClsCode");
  }
}
