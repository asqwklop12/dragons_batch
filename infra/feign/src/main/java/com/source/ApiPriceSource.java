package com.source;

import com.client.MarketPriceClient;
import com.dto.MarketPriceDailyResponse;
import com.dto.MarketPriceMonthlyResponse;
import com.properties.MarketPriceApiProperties;
import constant.Constants;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import model.price.MonthlyPriceReader;
import model.price.PriceReadItem;
import model.price.PriceReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiPriceSource implements PriceReader, MonthlyPriceReader {

  private static final String DEFAULT_PERIOD = "3";
  private static final String DEFAULT_ITEM_CODE = "111";
  private static final String DEFAULT_KIND_CODE = "05";
  private static final String DEFAULT_GRADE_RANK = "2";
  private static final String DEFAULT_COUNTY_CODE = "1101";
  private static final DateTimeFormatter KAMIS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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
            Constants.NOT_APPLICABLE,
            Constants.NOT_APPLICABLE,
            parsePrice(item.dpr1()),
            item.unit(),
            regDay
        ))
        .toList();
  }

  @Override
  public List<PriceReadItem> readInMonth(String itemCategoryCode, YearMonth yearMonth) {
    MarketPriceMonthlyResponse response = marketPriceClient.fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        marketPriceApiProperties.getCertKey(),
        marketPriceApiProperties.getCertId(),
        String.valueOf(yearMonth.getYear()),
        "%02d".formatted(yearMonth.getMonthValue()),
        itemCategoryCode
    );

    if (response == null || response.data() == null || response.data().item() == null) {
      return List.of();
    }

    return response.data().item().stream()
        .flatMap(item -> toMonthlyReadItems(item, yearMonth, itemCategoryCode).stream())
        .toList();
  }

  private int parsePrice(String price) {
    if (price == null) {
      return 0;
    }
    String normalized = price.replace(",", "").trim();
    if (normalized.isEmpty() || !normalized.matches("\\d+")) {
      return 0;
    }
    return Integer.parseInt(normalized);
  }

  private List<PriceReadItem> toMonthlyReadItems(
      MarketPriceMonthlyResponse.MarketPriceMonthlyItem item,
      YearMonth targetYearMonth,
      String itemCategoryCode
  ) {
    List<PriceReadItem> items = new ArrayList<>();
    Stream.of(
            monthlyEntry(item, item.day1(), item.dpr1(), targetYearMonth, itemCategoryCode),
            monthlyEntry(item, item.day2(), item.dpr2(), targetYearMonth, itemCategoryCode)
        )
        .flatMap(Optional::stream)
        .forEach(items::add);
    return items;
  }

  private Optional<PriceReadItem> monthlyEntry(
      MarketPriceMonthlyResponse.MarketPriceMonthlyItem item,
      String day,
      String price,
      YearMonth targetYearMonth,
      String itemCategoryCode
  ) {
    if (day == null || day.isBlank() || price == null || price.isBlank()) {
      return Optional.empty();
    }

    final LocalDate regDay;
    try {
      regDay = LocalDate.parse(day, KAMIS_DATE_FORMATTER);
    } catch (DateTimeParseException exception) {
      return Optional.empty();
    }
    if (!YearMonth.from(regDay).equals(targetYearMonth)) {
      return Optional.empty();
    }

    return Optional.of(new PriceReadItem(
        item.itemCode(),
        item.itemName(),
        item.kindCode(),
        item.kindName(),
        itemCategoryCode,
        categoryNameOf(itemCategoryCode),
        item.rankCode(),
        item.rankName(),
        parsePrice(price),
        item.unit(),
        regDay
    ));
  }

  private String categoryNameOf(String itemCategoryCode) {
    return switch (itemCategoryCode) {
      case "100" -> "식량작물";
      case "200" -> "채소류";
      case "300" -> "특용작물";
      case "400" -> "과일류";
      case "500" -> "축산물";
      case "600" -> "수산물";
      default -> Constants.NOT_APPLICABLE;
    };
  }
}
