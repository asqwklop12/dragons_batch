package com.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import model.price.PriceData;
import model.price.PriceReadItem;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BatchManualRunService {

  private final PriceReadService priceReadService;
  private final ItemProcessor<PriceReadItem, PriceData> priceItemProcessor;
  private final ItemWriter<PriceData> priceItemWriter;

  public int run(String itemCategoryCode, LocalDate regDay) {
    List<PriceData> processedItems = priceReadService.readItems(itemCategoryCode, regDay).stream()
        .map(this::processItem)
        .filter(Objects::nonNull)
        .toList();

    writeItems(processedItems);
    return processedItems.size();
  }

  private PriceData processItem(PriceReadItem item) {
    try {
      return priceItemProcessor.process(item);
    } catch (Exception exception) {
      throw new IllegalStateException("배치 데이터 변환에 실패했습니다.", exception);
    }
  }

  private void writeItems(List<PriceData> processedItems) {
    try {
      priceItemWriter.write(new Chunk<>(processedItems));
    } catch (Exception exception) {
      throw new IllegalStateException("배치 데이터 저장에 실패했습니다.", exception);
    }
  }
}
