package com.config;

import com.dto.MarketPriceDailyResponse;
import com.dto.MarketPriceMonthlyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interceptor.MdcInterceptor;
import com.interceptor.RequestResponseLoggingLogger;
import com.properties.FeignProperties;
import com.properties.FeignProperties.FeignProperty;
import constant.Constants;
import feign.Feign;
import feign.Request;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

  private Feign.Builder createFeignBuilder(
      String policyName,
      FeignProperties properties) {

    FeignProperty property = properties.getPolicies().get(policyName);

    if (property == null) {
      throw new IllegalStateException("Policy not found: " + policyName);
    }

    return Feign.builder()
        .options(
            new Request.Options(
                Duration.ofSeconds(property.connectTimeout()),
                Duration.ofSeconds(property.readTimeout()),
                true))
        .requestInterceptor(new MdcInterceptor())
        .logger(new RequestResponseLoggingLogger());
  }

  @Bean
  @ConditionalOnMissingBean(ObjectMapper.class)
  public ObjectMapper feignObjectMapper() {
    return new config.JacksonConfig().objectMapper();
  }

  @Bean
  public Feign.Builder feignBuilder(
      FeignProperties properties) {
    return createFeignBuilder(Constants.DEFAULT, properties);
  }

  @Bean
  public Decoder feignDecoder(ObjectMapper objectMapper) {
    return (response, type) -> {
      byte[] responseBody = readResponseBody(response);
      if (responseBody.length == 0) {
        return null;
      }

      String body = new String(responseBody, StandardCharsets.UTF_8).trim();
      if (body.startsWith("<")) {
        throw new DecodeException(
            response.status(),
            "KAMIS returned HTML instead of JSON: " + truncate(body, 200),
            response.request()
        );
      }

      try {
        if (type == MarketPriceDailyResponse.class) {
          return objectMapper.readValue(responseBody, MarketPriceDailyResponse.class);
        }
        if (type == MarketPriceMonthlyResponse.class) {
          return objectMapper.readValue(responseBody, MarketPriceMonthlyResponse.class);
        }
      } catch (IOException exception) {
        throw new DecodeException(
            response.status(),
            "Failed to decode KAMIS response: " + exception.getMessage(),
            response.request(),
            exception
        );
      }

      throw new DecodeException(
          response.status(),
          "Unsupported response type: " + type.getTypeName(),
          response.request()
      );
    };
  }

  private byte[] readResponseBody(Response response) throws IOException {
    if (response.body() == null) {
      return new byte[0];
    }

    try (InputStream inputStream = response.body().asInputStream()) {
      return inputStream.readAllBytes();
    }
  }

  private String truncate(String value, int maxLength) {
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength) + "...(truncated)";
  }
}
