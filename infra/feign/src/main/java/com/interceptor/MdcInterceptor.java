package com.interceptor;

import constant.Constants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;

public class MdcInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate template) {
    String requestId = MDC.get(Constants.EXTRA_REQUEST_ID);

    if (requestId == null || requestId.isBlank()) {
      requestId = firstHeaderValue(template.headers(), Constants.HEADER_EXTRA_REQUEST_ID);
    }

    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }

    template.removeHeader(Constants.HEADER_EXTRA_REQUEST_ID);
    template.header(Constants.HEADER_EXTRA_REQUEST_ID, requestId);
  }

  private String firstHeaderValue(Map<String, Collection<String>> headers, String headerName) {
    Collection<String> values = headers.get(headerName);
    if (values == null || values.isEmpty()) {
      return null;
    }

    return values.iterator().next();
  }
}
