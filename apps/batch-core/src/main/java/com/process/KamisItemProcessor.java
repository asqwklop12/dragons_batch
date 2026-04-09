package com.process;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import model.price.PriceData;
import model.price.PriceReadItem;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@RequiredArgsConstructor
public class KamisItemProcessor implements ItemProcessor<PriceReadItem, PriceData> {

  private final Clock clock;

  @Override
  public PriceData process(PriceReadItem item) {
    if (item == null) {
      return null;
    }

    return PriceData.create(
        item.itemCode(),
        item.itemName(),
        item.kindCode(),
        item.kindName(),
        item.marketCode(),
        item.marketName(),
        item.rankCode(),
        item.rankName(),
        item.price(),
        item.unit(),
        item.regDay(),
        LocalDateTime.now(clock)
    );
  }
}
