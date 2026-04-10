package com.write;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import model.price.PriceData;
import model.price.PriceDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

class KamisItemWriterTest {

  @Test
  void writeDelegatesChunkItemsToRepository() throws Exception {
    PriceDataRepository repository = mock(PriceDataRepository.class);
    KamisItemWriter writer = new KamisItemWriter(repository);
    PriceData cabbage = priceData("111", "배추", 12000);
    PriceData radish = priceData("112", "무", 9800);

    writer.write(Chunk.of(cabbage, radish));

    then(repository).should().saveAll(List.of(cabbage, radish));
  }

  @Test
  void writeSkipsRepositoryCallWhenChunkIsEmpty() throws Exception {
    PriceDataRepository repository = mock(PriceDataRepository.class);
    KamisItemWriter writer = new KamisItemWriter(repository);

    writer.write(new Chunk<>());

    then(repository).should(never()).saveAll(org.mockito.ArgumentMatchers.anyList());
  }

  private PriceData priceData(String itemCode, String itemName, int price) {
    return PriceData.create(
        itemCode,
        itemName,
        "01",
        "일반",
        "100",
        "서울",
        "01",
        "상품",
        price,
        "10kg",
        LocalDate.of(2024, 1, 15),
        LocalDateTime.of(2024, 1, 15, 10, 30)
    );
  }
}
