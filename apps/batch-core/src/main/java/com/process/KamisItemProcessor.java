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

    if (item.itemCode() == null || item.itemCode().isBlank()) {
      throw new SkippablePriceDataException("itemCode가 비어 있습니다.");
    }
    if (item.itemName() == null || item.itemName().isBlank()) {
      throw new SkippablePriceDataException("itemName이 비어 있습니다.");
    }
    if (item.unit() == null || item.unit().isBlank()) {
      throw new SkippablePriceDataException("unit이 비어 있습니다.");
    }
    if (item.regDay() == null) {
      throw new SkippablePriceDataException("regDay가 비어 있습니다.");
    }
    if (item.price() <= 0) {
      throw new SkippablePriceDataException("price가 0 이하입니다.");
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
