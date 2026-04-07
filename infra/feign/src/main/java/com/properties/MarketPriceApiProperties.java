package com.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kamis.api")
public class MarketPriceApiProperties {

  private String certKey;
  private String certId;

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
}
