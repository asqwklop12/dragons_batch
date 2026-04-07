package com.dragons.application.price.dto;

import model.price.PriceData;

public record PriceItemResult(
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

  public static PriceItemResult from(PriceData priceData) {
    return new PriceItemResult(
        priceData.getId(),
        priceData.getItemCode(),
        priceData.getItemName(),
        priceData.getKindCode(),
        priceData.getKindName(),
        priceData.getMarketCode(),
        priceData.getMarketName(),
        priceData.getRankCode(),
        priceData.getRankName(),
        priceData.getPrice(),
        priceData.getUnit(),
        priceData.getRegDay().toString(),
        priceData.getCreatedAt().toString()
    );
  }
}
