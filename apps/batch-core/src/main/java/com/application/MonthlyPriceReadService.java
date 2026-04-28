package com.application;

import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.MonthlyPriceReader;
import model.price.PriceReadItem;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlyPriceReadService {

  private final MonthlyPriceReader monthlyPriceReader;

  public List<PriceReadItem> readItems(String itemCategoryCode, YearMonth yearMonth) {
    return monthlyPriceReader.readInMonth(itemCategoryCode, yearMonth);
  }
}
