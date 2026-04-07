package com.properties;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feign")
public class FeignProperties {

  private static final String DEFAULT_POLICY = "default";
  private Map<String, FeignProperty> policies = new LinkedHashMap<>();

  public FeignProperties() {
    policies.put(DEFAULT_POLICY, FeignProperty.DEFAULT);
  }

  public Map<String, FeignProperty> getPolicies() {
    return policies;
  }

  public void setPolicies(Map<String, FeignProperty> policies) {
    this.policies = new LinkedHashMap<>();
    this.policies.put(DEFAULT_POLICY, FeignProperty.DEFAULT);
    if (policies != null) {
      this.policies.putAll(policies);
    }
  }

  public record FeignProperty(
      long connectTimeout,
      long readTimeout) {

    public static final FeignProperty DEFAULT = new FeignProperty(3, 30);

    public FeignProperty {
      connectTimeout = connectTimeout > 0 ? connectTimeout : 3;
      readTimeout = readTimeout > 0 ? readTimeout : 30;
    }
  }
}
