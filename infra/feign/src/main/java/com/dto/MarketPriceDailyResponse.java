package com.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketPriceDailyResponse(
    MarketPriceDailyData data
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MarketPriceDailyData(
      List<MarketPriceDailyItem> item
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MarketPriceDailyItem(
      @JsonProperty("itemcode") String itemCode,
      @JsonProperty("item_name") String itemName,
      @JsonProperty("kindcode") String kindCode,
      @JsonProperty("kind_name") String kindName,
      @JsonProperty("market_code") String marketCode,
      @JsonProperty("marketname") String marketName,
      @JsonProperty("rank_code") String rankCode,
      @JsonProperty("rank") String rankName,
      String unit,
      String day1,
      String dpr1
  ) {
  }
}
