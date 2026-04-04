package model.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PriceDataTest {

  @Test
  void matchesRegDayUsesIsoDateString() {
    PriceData priceData = PriceData.create(
        "111",
        "배추",
        "01",
        "일반",
        "100",
        "서울",
        "01",
        "상품",
        12000,
        "10kg",
        LocalDate.of(2024, 1, 15),
        LocalDateTime.of(2024, 1, 15, 10, 30)
    );

    assertThat(priceData.matchesRegDay("2024-01-15")).isTrue();
    assertThat(priceData.matchesRegDay("2024-01-16")).isFalse();
  }

  @Test
  void containsItemNameSupportsPartialMatch() {
    PriceData priceData = PriceData.create(
        "111",
        "배추",
        "01",
        "일반",
        "100",
        "서울",
        "01",
        "상품",
        12000,
        "10kg",
        LocalDate.of(2024, 1, 15),
        LocalDateTime.of(2024, 1, 15, 10, 30)
    );

    assertThat(priceData.containsItemName("배")).isTrue();
    assertThat(priceData.containsItemName("무")).isFalse();
  }
}
