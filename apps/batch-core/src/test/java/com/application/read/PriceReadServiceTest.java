package com.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import model.price.PriceReadItem;
import model.price.PriceReader;
import org.junit.jupiter.api.Test;

class PriceReadServiceTest {

  @Test
  void readItemsDelegatesToPort() {
    PriceReader priceReader = mock(PriceReader.class);
    PriceReadService priceReadService = new PriceReadService(priceReader);
    LocalDate regDay = LocalDate.of(2024, 1, 15);
    List<PriceReadItem> expected = List.of(
        new PriceReadItem("111", "배추", "01", "일반", "100", "서울", "01", "상품", 12000, "10kg", regDay)
    );

    given(priceReader.readOn("200", regDay)).willReturn(expected);

    assertThat(priceReadService.readItems("200", regDay)).isEqualTo(expected);
  }
}
