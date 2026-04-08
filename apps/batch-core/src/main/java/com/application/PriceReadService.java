package com.application;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceReadItem;
import model.price.PriceReader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceReadService {

  private final PriceReader priceReader;

  public List<PriceReadItem> readItems(String itemCategoryCode, LocalDate regDay) {
    return priceReader.readOn(itemCategoryCode, regDay);
  }
}
