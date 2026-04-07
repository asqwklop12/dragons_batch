package com.client;

import com.config.FeignConfig;
import com.dto.MarketPriceDailyResponse;
import com.dto.MarketPriceMonthlyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "marketPriceClient",
    url = "${kamis.api.base-url}",
    configuration = FeignConfig.class)
public interface MarketPriceClient {

  @GetMapping(value = "/service/price/xml.do", produces = "application/json")
  MarketPriceDailyResponse fetchDailyPricesInternal(
      @RequestParam("action") String requestAction,
      @RequestParam("p_returntype") String responseFormat,
      @RequestParam("p_cert_key") String certificationKey,
      @RequestParam("p_cert_id") String certificationId,
      @RequestParam("p_item_category_code") String itemCategoryCode,
      @RequestParam("p_regday") String regDay,
      @RequestParam("p_convert_kg_yn") String convertKgYn
  );

  @GetMapping(value = "/service/price/xml.do", produces = "application/json")
  MarketPriceMonthlyResponse fetchMonthlyPricesInternal(
      @RequestParam("action") String requestAction,
      @RequestParam("p_returntype") String responseFormat,
      @RequestParam("p_cert_key") String certificationKey,
      @RequestParam("p_cert_id") String certificationId,
      @RequestParam("p_yyyy") String targetYear,
      @RequestParam("p_mm") String targetMonth,
      @RequestParam("p_item_category_code") String itemCategoryCode
  );
}
