package com.dragons.feign;

import com.config.FeignConfig;
import com.config.InfraClientConfiguration;
import config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = {FeignConfig.class, InfraClientConfiguration.class, JacksonConfig.class},
    properties = {
        "kamis.api.base-url=https://www.kamis.or.kr",
        "kamis.api.cert-key=test-key",
        "kamis.api.cert-id=test-id"
    }
)
class FeignApplicationTests {

  @Test
  void contextLoads() {
  }

}
