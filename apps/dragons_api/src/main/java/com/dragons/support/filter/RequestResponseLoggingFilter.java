package com.dragons.support.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
  private static final int REQUEST_BUFFER_SIZE = 1024 * 1024;

  @Value("${logging.request-response.enabled:true}")
  private boolean loggingEnabled;

  @Value("${logging.request-response.body-enabled:false}")
  private boolean bodyLoggingEnabled;

  @Value("${logging.request-response.max-body-size:1024}")
  private int maxBodySize;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
      throws ServletException, IOException {
    if (!loggingEnabled || !request.getRequestURI().startsWith("/api")) {
      filterChain.doFilter(request, response);
      return;
    }

    if (isBinaryRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    long start = System.currentTimeMillis();
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, REQUEST_BUFFER_SIZE);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      long elapsed = System.currentTimeMillis() - start;
      log.info("REQ_RES method={} uri={} status={} time={}ms",
          wrappedRequest.getMethod(),
          wrappedRequest.getRequestURI(),
          wrappedResponse.getStatus(),
          elapsed);

      if (bodyLoggingEnabled && !shouldSkipBodyLogging(wrappedRequest, wrappedResponse)) {
        String reqBody = truncate(new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8), maxBodySize);
        String resBody = truncate(new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8), maxBodySize);
        log.info("REQ_BODY={} RES_BODY={}", reqBody, resBody);
      }

      wrappedResponse.copyBodyToResponse();
    }
  }

  private boolean isBinaryRequest(HttpServletRequest request) {
    String ct = request.getContentType();
    return ct != null && (ct.contains("multipart") || ct.contains("octet-stream"));
  }

  private boolean shouldSkipBodyLogging(HttpServletRequest request, HttpServletResponse response) {
    String ct = request.getContentType();
    String responseCt = response.getContentType();
    return (ct != null && (ct.contains("multipart") || ct.contains("octet-stream")))
        || (responseCt != null && (responseCt.contains("image") || responseCt.contains("octet-stream")));
  }

  private String truncate(String str, int maxLength) {
    if (str == null || str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength) + "...(truncated)";
  }
}
