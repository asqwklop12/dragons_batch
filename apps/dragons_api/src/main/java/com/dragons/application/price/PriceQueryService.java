package com.dragons.application.price;

import com.dragons.interfaces.api.price.dto.PriceItemResponse;
import com.dragons.interfaces.api.price.dto.PriceListResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import model.price.PriceData;
import model.price.PriceDataRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceQueryService {

  private final PriceDataRepository priceDataRepository;

  public PriceListResponse getPricesByDate(String regDay) {
    List<PriceItemResponse> items = priceDataRepository.findByRegDay(LocalDate.parse(regDay)).stream()
        .map(this::toResponse)
        .toList();
    return PriceListResponse.of(items);
  }

  public PriceListResponse searchPrices(String itemName) {
    List<PriceItemResponse> items = priceDataRepository.findByItemNameContaining(itemName).stream()
        .map(this::toResponse)
        .toList();
    return PriceListResponse.of(items);
  }

  public PriceListResponse getLatestPrices(Integer limit) {
    int safeLimit = Math.max(limit, 0);
    List<PriceItemResponse> items = priceDataRepository.findLatest(safeLimit).stream()
        .map(this::toResponse)
        .toList();
    return PriceListResponse.of(items);
  }

  private PriceItemResponse toResponse(PriceData priceData) {
    return new PriceItemResponse(
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
