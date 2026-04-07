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

  private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private final MarketPriceClient marketPriceClient;
  private final MarketPriceApiProperties marketPriceApiProperties;

  @Override
  public List<PriceReadItem> readOn(String itemCategoryCode, LocalDate regDay) {
    MarketPriceDailyResponse response = marketPriceClient.fetchDailyPricesInternal(
        "dailySalesList",
        "json",
        marketPriceApiProperties.getCertKey(),
        marketPriceApiProperties.getCertId(),
        itemCategoryCode,
        regDay.format(API_DATE_FORMAT),
        "N"
    );

    if (response == null || response.data() == null || response.data().item() == null) {
      return List.of();
    }

    return response.data().item().stream()
        .map(item -> new PriceReadItem(
            item.itemCode(),
            item.itemName(),
            item.kindCode(),
            item.kindName(),
            item.marketCode(),
            item.marketName(),
            item.rankCode(),
            item.rankName(),
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
