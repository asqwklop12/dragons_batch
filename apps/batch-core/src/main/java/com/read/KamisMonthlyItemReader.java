package com.read;

import com.application.MonthlyPriceReadService;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceReadItem;
import org.springframework.batch.infrastructure.item.ItemReader;

@RequiredArgsConstructor
public class KamisMonthlyItemReader implements ItemReader<PriceReadItem> {

  private final MonthlyPriceReadService monthlyPriceReadService;
  private final String itemCategoryCode;
  private final YearMonth yearMonth;

  private Iterator<PriceReadItem> iterator;

  @Override
  public PriceReadItem read() {
    if (iterator == null) {
      List<PriceReadItem> items = monthlyPriceReadService.readItems(itemCategoryCode, yearMonth);
      if (items == null) {
        items = Collections.emptyList();
      }
      iterator = items.iterator();
    }

    if (!iterator.hasNext()) {
      return null;
    }

    return iterator.next();
  }
}
