package com.read;

import com.application.PriceReadService;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceReadItem;
import org.springframework.batch.infrastructure.item.ItemReader;

@RequiredArgsConstructor
public class PriceItemReader implements ItemReader<PriceReadItem> {

  private final PriceReadService priceReadService;
  private final String itemCategoryCode;
  private final LocalDate regDay;

  private Iterator<PriceReadItem> iterator;

  @Override
  public PriceReadItem read() {
    if (iterator == null) {
      List<PriceReadItem> items = priceReadService.readItems(itemCategoryCode, regDay);
      iterator = items.iterator();
    }

    if (!iterator.hasNext()) {
      return null;
    }

    return iterator.next();
  }
}
