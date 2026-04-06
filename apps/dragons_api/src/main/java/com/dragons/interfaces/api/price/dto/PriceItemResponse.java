package com.dragons.interfaces.api.price.dto;

import com.dragons.application.price.dto.PriceItemResult;

public record PriceItemResponse(
    Long id,
    String itemCode,
    String itemName,
    String kindCode,
    String kindName,
    String marketCode,
    String marketName,
    String rankCode,
    String rankName,
    int price,
    String unit,
    String regDay,
    String createdAt
) {

  public static PriceItemResponse from(PriceItemResult priceItemResult) {
    return new PriceItemResponse(
        priceItemResult.id(),
        priceItemResult.itemCode(),
        priceItemResult.itemName(),
        priceItemResult.kindCode(),
        priceItemResult.kindName(),
        priceItemResult.marketCode(),
        priceItemResult.marketName(),
        priceItemResult.rankCode(),
        priceItemResult.rankName(),
        priceItemResult.price(),
        priceItemResult.unit(),
        priceItemResult.regDay(),
        priceItemResult.createdAt()
    );
  }
}
