package com.dragons.interfaces.api.price.dto;

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
}
