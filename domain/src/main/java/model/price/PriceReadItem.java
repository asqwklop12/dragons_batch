package model.price;

import java.time.LocalDate;

public record PriceReadItem(
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
    LocalDate regDay
) {
}
