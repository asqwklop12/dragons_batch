package com.application.monthly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.application.MonthlyPriceReadService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import model.price.MonthlyPriceReader;
import model.price.PriceReadItem;
import org.junit.jupiter.api.Test;

class MonthlyPriceReadServiceTest {

  @Test
  void readItemsDelegatesToMonthlyReader() {
    MonthlyPriceReader monthlyPriceReader = mock(MonthlyPriceReader.class);
    MonthlyPriceReadService monthlyPriceReadService = new MonthlyPriceReadService(monthlyPriceReader);
    YearMonth yearMonth = YearMonth.of(2024, 1);
    List<PriceReadItem> expected = List.of(
        new PriceReadItem("111", "배추", "01", "일반", "100", "서울", "01", "상품", 12000, "10kg", LocalDate.of(2024, 1, 15))
    );

    given(monthlyPriceReader.readInMonth("200", yearMonth)).willReturn(expected);

    assertThat(monthlyPriceReadService.readItems("200", yearMonth)).isEqualTo(expected);
  }
}
