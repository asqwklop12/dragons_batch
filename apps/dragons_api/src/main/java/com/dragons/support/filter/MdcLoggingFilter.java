package com.dragons.support.filter;

import constant.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {


  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(Constants.HEADER_REQUEST_ID);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }

    MDC.put(Constants.HEADER_REQUEST_ID, requestId);
    response.setHeader(Constants.HEADER_REQUEST_ID, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
