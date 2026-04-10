package com.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketPriceDailyResponse(
    @JsonProperty("error_code") String errorCode,
    List<List<String>> condition,
    List<MarketPriceDailyItem> price
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonDeserialize(using = MarketPriceDailyItemDeserializer.class)
  public record MarketPriceDailyItem(
      @JsonProperty("productno") String productNo,
      @JsonProperty("item_name") String itemName,
      @JsonProperty("product_cls_code") String productClsCode,
      @JsonProperty("product_cls_name") String productClsName,
      @JsonProperty("category_code") String categoryCode,
      @JsonProperty("category_name") String categoryName,
      String productName,
      String unit,
      String day1,
      String dpr1,
      String day2,
      String dpr2,
      String day3,
      String dpr3,
      String day4,
      String dpr4,
      String direction,
      String value
  ) {
  }
}
