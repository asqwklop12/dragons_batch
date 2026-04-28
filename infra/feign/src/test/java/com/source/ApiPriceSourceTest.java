package com.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.client.MarketPriceClient;
import com.dto.MarketPriceDailyResponse;
import com.dto.MarketPriceMonthlyResponse;
import com.properties.MarketPriceApiProperties;
import java.time.LocalDate;
import java.time.YearMonth;
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

  @Test
  void readInMonthUsesMonthlySalesParametersAndMapsAvailableDays() {
    MarketPriceClient marketPriceClient = mock(MarketPriceClient.class);
    MarketPriceApiProperties properties = new MarketPriceApiProperties();
    properties.setCertKey("test-key");
    properties.setCertId("test-id");
    ApiPriceSource apiPriceSource = new ApiPriceSource(marketPriceClient, properties);
    YearMonth yearMonth = YearMonth.of(2024, 1);

    given(marketPriceClient.fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        "test-key",
        "test-id",
        "2024",
        "01",
        "200"
    )).willReturn(new MarketPriceMonthlyResponse(
        new MarketPriceMonthlyResponse.MarketPriceMonthlyData(
            List.of(
                new MarketPriceMonthlyResponse.MarketPriceMonthlyItem(
                    "111",
                    "배추",
                    "01",
                    "일반",
                    "100",
                    "서울",
                    "01",
                    "상품",
                    "10kg",
                    "2024/01/15",
                    "12,000",
                    "2024/01/16",
                    "11,500",
                    "01"
                )
            )
        )
    ));

    List<PriceReadItem> result = apiPriceSource.readInMonth("200", yearMonth);

    verify(marketPriceClient).fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        "test-key",
        "test-id",
        "2024",
        "01",
        "200"
    );
    assertThat(result).hasSize(2);
    assertThat(result).extracting(PriceReadItem::regDay)
        .containsExactly(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16));
  }

  @Test
  void readInMonthReturnsSingleItemWhenSecondDayPairIsBlank() {
    MarketPriceClient marketPriceClient = mock(MarketPriceClient.class);
    MarketPriceApiProperties properties = new MarketPriceApiProperties();
    properties.setCertKey("test-key");
    properties.setCertId("test-id");
    ApiPriceSource apiPriceSource = new ApiPriceSource(marketPriceClient, properties);
    YearMonth yearMonth = YearMonth.of(2024, 1);

    given(marketPriceClient.fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        "test-key",
        "test-id",
        "2024",
        "01",
        "200"
    )).willReturn(new MarketPriceMonthlyResponse(
        new MarketPriceMonthlyResponse.MarketPriceMonthlyData(
            List.of(
                new MarketPriceMonthlyResponse.MarketPriceMonthlyItem(
                    "111", "배추", "01", "일반", "100", "서울", "01", "상품", "10kg",
                    "2024/01/15", "12,000", "", "", "01"
                )
            )
        )
    ));

    List<PriceReadItem> result = apiPriceSource.readInMonth("200", yearMonth);

    assertThat(result).singleElement().satisfies(item -> assertThat(item.regDay()).isEqualTo(LocalDate.of(2024, 1, 15)));
  }

  @Test
  void readInMonthFiltersOutDaysOutsideRequestedMonth() {
    MarketPriceClient marketPriceClient = mock(MarketPriceClient.class);
    MarketPriceApiProperties properties = new MarketPriceApiProperties();
    properties.setCertKey("test-key");
    properties.setCertId("test-id");
    ApiPriceSource apiPriceSource = new ApiPriceSource(marketPriceClient, properties);
    YearMonth yearMonth = YearMonth.of(2024, 1);

    given(marketPriceClient.fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        "test-key",
        "test-id",
        "2024",
        "01",
        "200"
    )).willReturn(new MarketPriceMonthlyResponse(
        new MarketPriceMonthlyResponse.MarketPriceMonthlyData(
            List.of(
                new MarketPriceMonthlyResponse.MarketPriceMonthlyItem(
                    "111", "배추", "01", "일반", "100", "서울", "01", "상품", "10kg",
                    "2024/01/15", "12,000", "2024/02/01", "11,500", "01"
                )
            )
        )
    ));

    List<PriceReadItem> result = apiPriceSource.readInMonth("200", yearMonth);

    assertThat(result).singleElement().satisfies(item -> assertThat(item.regDay()).isEqualTo(LocalDate.of(2024, 1, 15)));
  }

  @Test
  void readInMonthSkipsMalformedDayValuesInsteadOfThrowing() {
    MarketPriceClient marketPriceClient = mock(MarketPriceClient.class);
    MarketPriceApiProperties properties = new MarketPriceApiProperties();
    properties.setCertKey("test-key");
    properties.setCertId("test-id");
    ApiPriceSource apiPriceSource = new ApiPriceSource(marketPriceClient, properties);
    YearMonth yearMonth = YearMonth.of(2024, 1);

    given(marketPriceClient.fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        "test-key",
        "test-id",
        "2024",
        "01",
        "200"
    )).willReturn(new MarketPriceMonthlyResponse(
        new MarketPriceMonthlyResponse.MarketPriceMonthlyData(
            List.of(
                new MarketPriceMonthlyResponse.MarketPriceMonthlyItem(
                    "111", "배추", "01", "일반", "100", "서울", "01", "상품", "10kg",
                    "-", "12,000", "2024/01/16", "11,500", "01"
                ),
                new MarketPriceMonthlyResponse.MarketPriceMonthlyItem(
                    "112", "무", "01", "일반", "100", "서울", "01", "상품", "20kg",
                    "N/A", "8,000", "2024/13/40", "7,500", "01"
                )
            )
        )
    ));

    List<PriceReadItem> result = apiPriceSource.readInMonth("200", yearMonth);

    assertThat(result).singleElement().satisfies(item -> {
      assertThat(item.regDay()).isEqualTo(LocalDate.of(2024, 1, 16));
      assertThat(item.marketCode()).isEqualTo("200");
      assertThat(item.marketName()).isEqualTo("채소류");
    });
  }

  @Test
  void readInMonthReturnsEmptyWhenMonthlyDataIsNull() {
    MarketPriceClient marketPriceClient = mock(MarketPriceClient.class);
    MarketPriceApiProperties properties = new MarketPriceApiProperties();
    properties.setCertKey("test-key");
    properties.setCertId("test-id");
    ApiPriceSource apiPriceSource = new ApiPriceSource(marketPriceClient, properties);

    given(marketPriceClient.fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        "test-key",
        "test-id",
        "2024",
        "01",
        "200"
    )).willReturn(new MarketPriceMonthlyResponse(null));

    assertThat(apiPriceSource.readInMonth("200", YearMonth.of(2024, 1))).isEmpty();
  }

  @Test
  void readInMonthReturnsEmptyWhenMonthlyItemsAreNull() {
    MarketPriceClient marketPriceClient = mock(MarketPriceClient.class);
    MarketPriceApiProperties properties = new MarketPriceApiProperties();
    properties.setCertKey("test-key");
    properties.setCertId("test-id");
    ApiPriceSource apiPriceSource = new ApiPriceSource(marketPriceClient, properties);

    given(marketPriceClient.fetchMonthlyPricesInternal(
        "monthlySalesList",
        "json",
        "test-key",
        "test-id",
        "2024",
        "01",
        "200"
    )).willReturn(new MarketPriceMonthlyResponse(new MarketPriceMonthlyResponse.MarketPriceMonthlyData(null)));

    assertThat(apiPriceSource.readInMonth("200", YearMonth.of(2024, 1))).isEmpty();
  }
}
