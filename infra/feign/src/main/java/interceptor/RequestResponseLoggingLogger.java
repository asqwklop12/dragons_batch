package interceptor;

import constant.Constants;
import feign.Logger;
import feign.Request;
import feign.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class RequestResponseLoggingLogger extends Logger {

  private static final Set<String> SENSITIVE_QUERY_PARAMS = Set.of(
      "p_cert_key", "p_cert_id", "cert", "key", "id"
  );

  @Override
  protected void log(String configKey, String format, Object... args) {
    // no-op
  }

  @Override
  protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime)
      throws IOException {

    Request request = response.request();
    byte[] responseBody = readResponseBody(response);

    log.info("REQ_RES method={} uri={} status={} time={}ms requestId={}",
        request.httpMethod(),
        sanitizeUrl(request.url()),
        response.status(),
        elapsedTime,
        resolveRequestId(request));

    if (!shouldSkipBodyLogging(request, response)) {
      log.info("REQ_BODY={} RES_BODY={}",
          truncate(readRequestBody(request), 1024),
          truncate(readResponseBody(responseBody, response.charset()), 1024));
    }

    if (response.body() == null) {
      return response;
    }

    return response.toBuilder().body(responseBody).build();
  }

  @Override
  protected IOException logIOException(String configKey, Level logLevel, IOException ioe, long elapsedTime) {
    log.warn("REQ_RES_IO_EXCEPTION configKey={} time={}ms message={}", configKey, elapsedTime, ioe.getMessage());
    return ioe;
  }

  private String resolveRequestId(Request request) {
    String requestId = firstHeaderValue(request.headers(), Constants.HEADER_EXTRA_REQUEST_ID);
    if (requestId == null || requestId.isBlank()) {
      requestId = MDC.get(Constants.EXTRA_REQUEST_ID);
    }
    return requestId;
  }

  private byte[] readResponseBody(Response response) throws IOException {
    if (response.body() == null) {
      return new byte[0];
    }

    return response.body().asInputStream().readAllBytes();
  }

  private String readRequestBody(Request request) {
    if (request.body() == null || request.body().length == 0 || request.isBinary()) {
      return "";
    }

    Charset charset = request.charset() == null ? StandardCharsets.UTF_8 : request.charset();
    return new String(request.body(), charset);
  }

  private String readResponseBody(byte[] responseBody, Charset charset) {
    if (responseBody == null || responseBody.length == 0) {
      return "";
    }

    Charset bodyCharset = charset == null ? StandardCharsets.UTF_8 : charset;
    return new String(responseBody, bodyCharset);
  }

  private boolean shouldSkipBodyLogging(Request request, Response response) {
    String requestContentType = firstHeaderValue(request.headers(), "Content-Type");
    String responseContentType = firstHeaderValue(response.headers(), "Content-Type");

    return isBinaryContentType(requestContentType) || isBinaryResponseContentType(responseContentType);
  }

  private boolean isBinaryContentType(String contentType) {
    return contentType != null && (contentType.contains("multipart") || contentType.contains("octet-stream"));
  }

  private boolean isBinaryResponseContentType(String contentType) {
    return contentType != null && (contentType.contains("image") || contentType.contains("octet-stream"));
  }

  private String firstHeaderValue(Map<String, Collection<String>> headers, String headerName) {
    Collection<String> values = headers.get(headerName);
    if (values == null || values.isEmpty()) {
      return null;
    }

    return values.iterator().next();
  }

  private String truncate(String str, int maxLength) {
    if (str == null || str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength) + "...(truncated)";
  }

  /**
   * Sanitizes URL by masking sensitive query parameter values.
   *
   * @param url The URL to sanitize
   * @return The sanitized URL with sensitive query param values replaced with "****"
   */
  private String sanitizeUrl(String url) {
    if (url == null || !url.contains("?")) {
      return url;
    }

    int queryStart = url.indexOf('?');
    String baseUrl = url.substring(0, queryStart + 1);
    String queryString = url.substring(queryStart + 1);

    String[] params = queryString.split("&");
    StringBuilder sanitized = new StringBuilder(baseUrl);

    for (int i = 0; i < params.length; i++) {
      if (i > 0) {
        sanitized.append("&");
      }

      String param = params[i];
      int equalsIndex = param.indexOf('=');

      if (equalsIndex == -1) {
        sanitized.append(param);
        continue;
      }

      String key = param.substring(0, equalsIndex);
      String value = param.substring(equalsIndex + 1);

      sanitized.append(key).append("=");
      if (SENSITIVE_QUERY_PARAMS.contains(key)) {
        sanitized.append("****");
      } else {
        sanitized.append(value);
      }
    }

    return sanitized.toString();
  }
}