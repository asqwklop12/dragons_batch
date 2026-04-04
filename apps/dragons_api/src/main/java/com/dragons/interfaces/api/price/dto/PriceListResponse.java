package com.dragons.interfaces.api.price.dto;

import java.util.List;

public record PriceListResponse(
    int count,
    List<PriceItemResponse> data
) {

  public static PriceListResponse of(List<PriceItemResponse> data) {
    return new PriceListResponse(data.size(), data);
  }
}
