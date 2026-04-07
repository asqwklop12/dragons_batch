package com.dragons.interfaces.api.price.dto;

public record LatestPricesRequest(
    Integer limit
) {
  public LatestPricesRequest {
    if (limit == null) {
      limit = 50;
    }
  }
}
