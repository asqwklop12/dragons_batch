package com.dragons.application.price;

import com.dragons.application.price.dto.PriceItemResult;
import com.dragons.application.price.dto.PriceListResult;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceDataRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceQueryService {

  private final PriceDataRepository priceDataRepository;

  public PriceListResult getPricesByDate(String regDay) {
    List<PriceItemResult> items = priceDataRepository.pricesOn(LocalDate.parse(regDay)).stream()
        .map(PriceItemResult::from)
        .toList();
    return PriceListResult.of(items);
  }

  public PriceListResult searchPrices(String itemName) {
    List<PriceItemResult> items = priceDataRepository.pricesMatchingItemName(itemName).stream()
        .map(PriceItemResult::from)
        .toList();
    return PriceListResult.of(items);
  }

  public PriceListResult getLatestPrices(Integer limit) {
    int safeLimit = Math.max(limit, 0);
    List<PriceItemResult> items = priceDataRepository.latestPrices(safeLimit).stream()
        .map(PriceItemResult::from)
        .toList();
    return PriceListResult.of(items);
  }
}
