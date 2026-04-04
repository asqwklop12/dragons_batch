package com.dragons.interfaces.api.batch.dto;

import java.util.List;

public record BatchStatusResponse(
    int count,
    boolean mockMode,
    boolean apiConfigured,
    List<BatchStatusItemResponse> data
) {
}
