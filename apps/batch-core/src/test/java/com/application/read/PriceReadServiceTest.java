package com.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.exception.TransientPriceReadException;
import feign.FeignException;
import feign.RetryableException;
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

  @Test
  void readItemsConvertsRequestTimeoutFeignExceptionToTransientException() {
    PriceReadService priceReadService = new PriceReadService(failingReader(new FeignException(408)));

    assertThatThrownBy(() -> priceReadService.readItems("100", LocalDate.of(2024, 1, 15)))
        .isInstanceOf(TransientPriceReadException.class)
        .hasMessage("KAMIS 일별 가격 API 호출 중 일시 오류가 발생했습니다.")
        .hasCauseInstanceOf(FeignException.class);
  }

  @Test
  void readItemsConvertsRetryableFeignExceptionToTransientException() {
    PriceReadService priceReadService = new PriceReadService(failingReader(new RetryableException()));

    assertThatThrownBy(() -> priceReadService.readItems("100", LocalDate.of(2024, 1, 15)))
        .isInstanceOf(TransientPriceReadException.class)
        .hasMessage("KAMIS 일별 가격 API 호출 중 일시 오류가 발생했습니다.")
        .hasCauseInstanceOf(RetryableException.class);
  }

  @Test
  void readItemsRethrowsNonTransientFeignException() {
    PriceReadService priceReadService = new PriceReadService(failingReader(new FeignException(400)));

    assertThatThrownBy(() -> priceReadService.readItems("100", LocalDate.of(2024, 1, 15)))
        .isInstanceOf(FeignException.class);
  }

  private PriceReader failingReader(RuntimeException exception) {
    return (itemCategoryCode, regDay) -> {
      throw exception;
    };
  }
}
