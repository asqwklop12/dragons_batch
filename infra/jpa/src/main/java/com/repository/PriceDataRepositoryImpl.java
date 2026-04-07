package com.repository;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceDataRepository;
import model.price.PriceData;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PriceDataRepositoryImpl implements PriceDataRepository {

  private final JpaPriceDataRepository repository;

  @Override
  public List<PriceData> pricesOn(LocalDate regDay) {
    return repository.findAllByRegDayOrderByCreatedAtDescIdDesc(regDay);
  }

  @Override
  public List<PriceData> pricesMatchingItemName(String itemName) {
    return repository.findAllByItemNameContainingIgnoreCaseOrderByCreatedAtDescIdDesc(itemName);
  }

  @Override
  public List<PriceData> latestPrices(int limit) {
    if (limit <= 0) {
      return List.of();
    }
    return repository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, limit));
  }
}
