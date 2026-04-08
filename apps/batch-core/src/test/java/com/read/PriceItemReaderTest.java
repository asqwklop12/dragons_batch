package com.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.application.PriceReadService;
import java.time.LocalDate;
import java.util.List;
import model.price.PriceReadItem;
import org.junit.jupiter.api.Test;

class PriceItemReaderTest {

  @Test
  void readReturnsItemsInOrderAndThenNull() throws Exception {
    PriceReadService priceReadService = mock(PriceReadService.class);
    LocalDate regDay = LocalDate.of(2024, 1, 15);

    given(priceReadService.readItems("200", regDay))
        .willReturn(
            List.of(
                new PriceReadItem("111", "배추", "01", "일반", "100", "서울", "01", "상품", 12000, "10kg", regDay),
                new PriceReadItem("112", "무", "01", "일반", "100", "서울", "01", "상품", 9800, "20kg", regDay)
            )
        );

    PriceItemReader reader = new PriceItemReader(priceReadService, "200", regDay);

    assertThat(reader.read().itemName()).isEqualTo("배추");
    assertThat(reader.read().itemName()).isEqualTo("무");
    assertThat(reader.read()).isNull();
  }
}
