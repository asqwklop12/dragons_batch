package com.dragons.interfaces.api.price.dto;

import com.dragons.application.price.dto.PriceListResult;
import java.util.List;

public record PriceListResponse(
    int count,
    List<PriceItemResponse> data
) {

  public static PriceListResponse of(List<PriceItemResponse> data) {
    return new PriceListResponse(data.size(), data);
  }

  public static PriceListResponse from(PriceListResult result) {
    return new PriceListResponse(
        result.count(),
        result.data().stream()
            .map(PriceItemResponse::from)
            .toList()
    );
  }
}
