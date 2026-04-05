package config;

import constant.Constants;
import feign.Feign;
import feign.Request;
import interceptor.MdcInterceptor;
import interceptor.RequestResponseLoggingLogger;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import properties.FeignProperties;
import properties.FeignProperties.FeignProperty;

@Configuration
@EnableConfigurationProperties(FeignProperties.class)
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
  public Feign.Builder feignBuilder(
      FeignProperties properties) {
    return createFeignBuilder(Constants.DEFAULT, properties);
  }
}
