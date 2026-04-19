package com.application;

import java.util.List;

public record BatchStatusResult(
    int count,
    List<BatchStatusItemResult> items
) {

  public BatchStatusResult {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
