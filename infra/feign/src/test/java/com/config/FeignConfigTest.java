package com.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.dto.MarketPriceDailyResponse;
import feign.Request;
import feign.Response;
import feign.codec.Decoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeignConfigTest {

  @Test
  void decoderReadsJsonBodiesReturnedAsTextPlain() throws Exception {
    FeignConfig feignConfig = new FeignConfig();
    Decoder decoder = feignConfig.feignDecoder();
    Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(
            Request.HttpMethod.GET,
            "https://www.kamis.or.kr/service/price/xml.do",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            null
        ))
        .headers(Map.of("Content-Type", List.of("text/plain;charset=utf-8")))
        .body(
            """
                {
                  "error_code": "000",
                  "price": [
                    {
                      "productno": "111",
                      "item_name": "배추",
                      "product_cls_code": "01",
                      "product_cls_name": "일반",
                      "category_code": "100",
                      "category_name": "서울",
                      "unit": "10kg",
                      "dpr1": "12,000"
                    }
                  ]
                }
                """,
            StandardCharsets.UTF_8
        )
        .build();

    MarketPriceDailyResponse decoded = (MarketPriceDailyResponse) decoder.decode(response, MarketPriceDailyResponse.class);

    assertThat(decoded.price()).singleElement().satisfies(item -> {
      assertThat(item.productNo()).isEqualTo("111");
      assertThat(item.itemName()).isEqualTo("배추");
      assertThat(item.dpr1()).isEqualTo("12,000");
    });
  }

  @Test
  void decoderHandlesArrayValuesInsideDailyItems() throws Exception {
    FeignConfig feignConfig = new FeignConfig();
    Decoder decoder = feignConfig.feignDecoder();
    Response response = Response.builder()
        .status(200)
        .reason("OK")
        .request(Request.create(
            Request.HttpMethod.GET,
            "https://www.kamis.or.kr/service/price/xml.do",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            null
        ))
        .headers(Map.of("Content-Type", List.of("text/plain;charset=utf-8")))
        .body(
            """
                {
                  "error_code": "000",
                  "price": [
                    {
                      "productno": "111",
                      "item_name": "배추",
                      "product_cls_code": "01",
                      "product_cls_name": "일반",
                      "category_code": "100",
                      "category_name": "서울",
                      "unit": "10kg",
                      "dpr1": "12,000",
                      "dpr2": ["11,500"]
                    }
                  ]
                }
                """,
            StandardCharsets.UTF_8
        )
        .build();

    MarketPriceDailyResponse decoded = (MarketPriceDailyResponse) decoder.decode(response, MarketPriceDailyResponse.class);

    assertThat(decoded.price().getFirst().dpr2()).isEqualTo("11,500");
  }
}
