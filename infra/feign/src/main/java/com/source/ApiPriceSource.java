package com.source;

import com.client.MarketPriceClient;
import com.dto.MarketPriceDailyResponse;
import com.properties.MarketPriceApiProperties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceReadItem;
import model.price.PriceReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiPriceSource implements PriceReader {

  private static final String DEFAULT_PERIOD = "3";
  private static final String DEFAULT_ITEM_CODE = "111";
  private static final String DEFAULT_KIND_CODE = "05";
  private static final String DEFAULT_GRADE_RANK = "2";
  private static final String DEFAULT_COUNTY_CODE = "1101";

  private final MarketPriceClient marketPriceClient;
  private final MarketPriceApiProperties marketPriceApiProperties;

  @Override
  public List<PriceReadItem> readOn(String itemCategoryCode, LocalDate regDay) {
    MarketPriceDailyResponse response = marketPriceClient.fetchDailyPricesInternal(
        "dailySalesList",
        marketPriceApiProperties.getCertKey(),
        marketPriceApiProperties.getCertId(),
        "json",
        itemCategoryCode,
        DEFAULT_PERIOD,
        "N",
        String.valueOf(regDay.getYear()),
        DEFAULT_ITEM_CODE,
        DEFAULT_KIND_CODE,
        DEFAULT_GRADE_RANK,
        DEFAULT_COUNTY_CODE
    );

    if (response == null || response.price() == null) {
      return List.of();
    }

    return response.price().stream()
        .map(item -> new PriceReadItem(
            item.productNo(),
            item.itemName(),
            item.productClsCode(),
            item.productClsName(),
            item.categoryCode(),
            item.categoryName(),
            "",
            "",
            parsePrice(item.dpr1()),
            item.unit(),
            regDay
        ))
        .toList();
  }

  private int parsePrice(String price) {
    if (price == null || price.isBlank()) {
      return 0;
    }
    return Integer.parseInt(price.replace(",", "").trim());
  }
}
