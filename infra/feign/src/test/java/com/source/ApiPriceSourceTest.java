package com.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.client.MarketPriceClient;
import com.dto.MarketPriceDailyResponse;
import com.properties.MarketPriceApiProperties;
import java.time.LocalDate;
import java.util.List;
import model.price.PriceReadItem;
import org.junit.jupiter.api.Test;

class ApiPriceSourceTest {

  @Test
  void readOnUsesCurrentDailySalesParametersAndMapsItems() {
    MarketPriceClient marketPriceClient = mock(MarketPriceClient.class);
    MarketPriceApiProperties properties = new MarketPriceApiProperties();
    properties.setCertKey("test-key");
    properties.setCertId("test-id");
    ApiPriceSource apiPriceSource = new ApiPriceSource(marketPriceClient, properties);
    LocalDate regDay = LocalDate.of(2024, 1, 15);

    given(marketPriceClient.fetchDailyPricesInternal(
        "dailySalesList",
        "test-key",
        "test-id",
        "json",
        "200",
        "3",
        "N",
        "2024",
        "111",
        "05",
        "2",
        "1101"
    )).willReturn(new MarketPriceDailyResponse(
        null,
        List.of(),
        List.of(
            new MarketPriceDailyResponse.MarketPriceDailyItem(
                "111",
                "배추",
                "01",
                "일반",
                "100",
                "서울",
                null,
                "10kg",
                "2024/01/15",
                "12,000",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        )
    ));

    List<PriceReadItem> result = apiPriceSource.readOn("200", regDay);

    verify(marketPriceClient).fetchDailyPricesInternal(
        "dailySalesList",
        "test-key",
        "test-id",
        "json",
        "200",
        "3",
        "N",
        "2024",
        "111",
        "05",
        "2",
        "1101"
    );
    assertThat(result).singleElement().satisfies(item -> {
      assertThat(item.itemCode()).isEqualTo("111");
      assertThat(item.itemName()).isEqualTo("배추");
      assertThat(item.price()).isEqualTo(12000);
      assertThat(item.regDay()).isEqualTo(regDay);
    });
  }
}
