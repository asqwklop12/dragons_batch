package com.write;

import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceData;
import model.price.PriceDataRepository;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

@RequiredArgsConstructor
public class KamisItemWriter implements ItemWriter<PriceData> {

  private final PriceDataRepository priceDataRepository;

  @Override
  public void write(Chunk<? extends PriceData> chunk) {
    List<? extends PriceData> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }
    priceDataRepository.saveAll(items);
  }
}
