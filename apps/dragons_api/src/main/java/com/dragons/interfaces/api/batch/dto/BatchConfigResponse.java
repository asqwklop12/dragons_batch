package com.dragons.interfaces.api.batch.dto;

public record BatchConfigResponse(
    boolean apiConfigured,
    boolean mockMode,
    String baseUrl,
    boolean certKeySet,
    boolean certIdSet
) {
}
