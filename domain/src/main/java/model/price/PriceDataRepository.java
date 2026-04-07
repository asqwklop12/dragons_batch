package model.price;

import java.time.LocalDate;
import java.util.List;

public interface PriceDataRepository {

  List<PriceData> pricesOn(LocalDate regDay);

  List<PriceData> pricesMatchingItemName(String itemName);

  List<PriceData> latestPrices(int limit);
}
