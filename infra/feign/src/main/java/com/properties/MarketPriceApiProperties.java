package com.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kamis.api")
public class MarketPriceApiProperties {

  @NotBlank
  private String baseUrl;

  @NotBlank
  private String certKey;

  @NotBlank
  private String certId;

  private boolean mockMode;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getCertKey() {
    return certKey;
  }

  public void setCertKey(String certKey) {
    this.certKey = certKey;
  }

  public String getCertId() {
    return certId;
  }

  public void setCertId(String certId) {
    this.certId = certId;
  }

  public boolean isMockMode() {
    return mockMode;
  }

  public void setMockMode(boolean mockMode) {
    this.mockMode = mockMode;
  }
}
