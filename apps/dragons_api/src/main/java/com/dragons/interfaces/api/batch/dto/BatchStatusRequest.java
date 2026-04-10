package com.dragons.interfaces.api.batch.dto;

public record BatchStatusRequest(
    Integer limit
) {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 20;

  public int resolvedLimit() {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.clamp(limit, 0, MAX_LIMIT);
  }
}
