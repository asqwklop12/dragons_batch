package model.price;

import java.time.LocalDate;
import java.util.List;

public interface PriceDataRepository {

  List<PriceData> findByRegDay(LocalDate regDay);

  List<PriceData> findByItemNameContaining(String itemName);

  List<PriceData> findLatest(int limit);
}
