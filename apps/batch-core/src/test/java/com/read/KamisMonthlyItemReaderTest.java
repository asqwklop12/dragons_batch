package com.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.application.MonthlyPriceReadService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import model.price.PriceReadItem;
import org.junit.jupiter.api.Test;

class KamisMonthlyItemReaderTest {

  @Test
  void readReturnsMonthlyItemsInOrderAndThenNull() {
    MonthlyPriceReadService monthlyPriceReadService = org.mockito.Mockito.mock(MonthlyPriceReadService.class);
    YearMonth yearMonth = YearMonth.of(2024, 1);

    given(monthlyPriceReadService.readItems("200", yearMonth))
        .willReturn(
            List.of(
                new PriceReadItem("111", "배추", "01", "일반", "100", "서울", "01", "상품", 12000, "10kg", LocalDate.of(2024, 1, 15)),
                new PriceReadItem("112", "무", "01", "일반", "100", "서울", "01", "상품", 9800, "20kg", LocalDate.of(2024, 1, 16))
            )
        );

    KamisMonthlyItemReader reader = new KamisMonthlyItemReader(monthlyPriceReadService, "200", yearMonth);

    assertThat(reader.read().itemCode()).isEqualTo("111");
    assertThat(reader.read().itemCode()).isEqualTo("112");
    assertThat(reader.read()).isNull();
  }
}
