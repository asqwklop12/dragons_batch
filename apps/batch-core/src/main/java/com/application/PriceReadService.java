package com.application;

import java.time.LocalDate;
import java.util.List;
import com.exception.TransientPriceReadException;
import lombok.RequiredArgsConstructor;
import model.price.PriceReadItem;
import model.price.PriceReader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceReadService {

  private final PriceReader priceReader;

  public List<PriceReadItem> readItems(String itemCategoryCode, LocalDate regDay) {
    try {
      return priceReader.readOn(itemCategoryCode, regDay);
    } catch (RuntimeException exception) {
      if (TransientPriceReadExceptionMapper.isRetryable(exception)) {
        throw new TransientPriceReadException("KAMIS 일별 가격 API 호출 중 일시 오류가 발생했습니다.", exception);
      }
      throw exception;
    }
  }
}
