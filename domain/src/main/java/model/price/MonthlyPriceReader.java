package model.price;

import java.time.YearMonth;
import java.util.List;

public interface MonthlyPriceReader {

  List<PriceReadItem> readInMonth(String itemCategoryCode, YearMonth yearMonth);
}
