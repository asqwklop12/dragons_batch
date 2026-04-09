package com.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import model.price.PriceData;
import model.price.PriceReadItem;
import org.junit.jupiter.api.Test;

class PriceItemProcessorTest {

  @Test
  void processMapsReadItemToPriceData() throws Exception {
    Clock fixedClock = Clock.fixed(Instant.parse("2024-01-15T01:30:00Z"), ZoneId.of("Asia/Seoul"));
    PriceItemProcessor processor = new PriceItemProcessor(fixedClock);
    PriceReadItem item = new PriceReadItem(
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
        LocalDate.of(2024, 1, 15)
    );

    PriceData result = processor.process(item);

    assertThat(result.getId()).isNull();
    assertThat(result.getItemCode()).isEqualTo("111");
    assertThat(result.getItemName()).isEqualTo("배추");
    assertThat(result.getKindCode()).isEqualTo("01");
    assertThat(result.getKindName()).isEqualTo("일반");
    assertThat(result.getMarketCode()).isEqualTo("100");
    assertThat(result.getMarketName()).isEqualTo("서울");
    assertThat(result.getRankCode()).isEqualTo("01");
    assertThat(result.getRankName()).isEqualTo("상품");
    assertThat(result.getPrice()).isEqualTo(12000);
    assertThat(result.getUnit()).isEqualTo("10kg");
    assertThat(result.getRegDay()).isEqualTo(LocalDate.of(2024, 1, 15));
    assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));
  }

  @Test
  void processReturnsNullWhenItemIsNull() throws Exception {
    PriceItemProcessor processor = new PriceItemProcessor(Clock.systemDefaultZone());

    assertThat(processor.process(null)).isNull();
  }
}
