package com.application;

import com.exception.TransientPriceReadException;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.MonthlyPriceReader;
import model.price.PriceReadItem;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlyPriceReadService {

  private final MonthlyPriceReader monthlyPriceReader;

  public List<PriceReadItem> readItems(String itemCategoryCode, YearMonth yearMonth) {
    try {
      return monthlyPriceReader.readInMonth(itemCategoryCode, yearMonth);
    } catch (RuntimeException exception) {
      if (TransientPriceReadExceptionMapper.isRetryable(exception)) {
        throw new TransientPriceReadException("KAMIS 월별 가격 API 호출 중 일시 오류가 발생했습니다.", exception);
      }
      throw exception;
    }
  }
}
