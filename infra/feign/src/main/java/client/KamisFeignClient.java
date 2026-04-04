package client;

import config.FeignConfig;
import dto.KamisMonthlyPriceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "kamisFeignClient",
    url = "${kamis.api.base-url}",
    configuration = FeignConfig.class)
public interface KamisFeignClient {

  @GetMapping(value = "/service/price/xml.do", produces = "application/json")
  KamisMonthlyPriceResponse fetchMonthlyPricesInternal(
      @RequestParam("action") String requestAction,
      @RequestParam("p_returntype") String responseFormat,
      @RequestParam("p_cert_key") String certificationKey,
      @RequestParam("p_cert_id") String certificationId,
      @RequestParam("p_yyyy") String targetYear,
      @RequestParam("p_mm") String targetMonth,
      @RequestParam("p_item_category_code") String itemCategoryCode
  );
}
