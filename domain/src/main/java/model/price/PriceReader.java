package model.price;

import java.time.LocalDate;
import java.util.List;

public interface PriceReader {

  List<PriceReadItem> readOn(String itemCategoryCode, LocalDate regDay);
}
