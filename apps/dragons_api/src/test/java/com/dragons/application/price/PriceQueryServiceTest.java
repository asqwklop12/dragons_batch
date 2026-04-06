package com.dragons.application.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.dragons.interfaces.api.price.dto.PriceListResponse;
import model.price.PriceData;
import model.price.PriceDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceQueryServiceTest {

  @Mock
  private PriceDataRepository priceDataRepository;

  @InjectMocks
  private PriceQueryService priceQueryService;

  @Test
  void getPricesByDateFiltersByRegDay() {
    given(priceDataRepository.pricesOn(LocalDate.parse("2024-01-15")))
        .willReturn(
            List.of(
                priceData(1L, "111", "배추", "2024-01-15", "2024-01-15T10:30:00"),
                priceData(2L, "112", "무", "2024-01-15", "2024-01-15T10:31:00")
            )
        );

    PriceListResponse response = priceQueryService.getPricesByDate("2024-01-15");

    assertThat(response.count()).isEqualTo(2);
    assertThat(response.data())
        .extracting(item -> item.itemName())
        .containsExactly("배추", "무");
  }

  @Test
  void searchPricesFindsMatchingItemName() {
    given(priceDataRepository.pricesMatchingItemName("사과"))
        .willReturn(List.of(priceData(3L, "211", "사과", "2024-01-16", "2024-01-16T09:10:00")));

    PriceListResponse response = priceQueryService.searchPrices("사과");

    assertThat(response.count()).isEqualTo(1);
    assertThat(response.data().getFirst().itemCode()).isEqualTo("211");
  }

  @Test
  void getLatestPricesAppliesRequestedLimitInCreatedAtOrder() {
    given(priceDataRepository.latestPrices(2))
        .willReturn(
            List.of(
                priceData(4L, "311", "양파", "2024-01-17", "2024-01-17T08:45:00"),
                priceData(3L, "211", "사과", "2024-01-16", "2024-01-16T09:10:00")
            )
        );

    PriceListResponse response = priceQueryService.getLatestPrices(2);

    assertThat(response.count()).isEqualTo(2);
    assertThat(response.data())
        .extracting(item -> item.itemName())
        .containsExactly("양파", "사과");
  }

  private PriceData priceData(Long id, String itemCode, String itemName, String regDay, String createdAt) {
    PriceData priceData = PriceData.create(
        itemCode,
        itemName,
        "01",
        "일반",
        "100",
        "서울",
        "01",
        "상품",
        1000,
        "10kg",
        LocalDate.parse(regDay),
        LocalDateTime.parse(createdAt)
    );
    priceData.setId(id);
    return priceData;
  }
}
