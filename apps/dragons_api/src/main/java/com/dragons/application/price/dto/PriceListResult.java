package com.dragons.application.price.dto;

import java.util.List;

public record PriceListResult(
    int count,
    List<PriceItemResult> data
) {

  public static PriceListResult of(List<PriceItemResult> data) {
    return new PriceListResult(data.size(), data);
  }
}
