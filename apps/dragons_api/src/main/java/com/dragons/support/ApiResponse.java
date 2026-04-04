package com.dragons.support;

public record ApiResponse<T>(
    boolean success,
    T data,
    String message
) {

  public static <T> ApiResponse<T> successResponse(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static ApiResponse<Void> successResponse() {
    return new ApiResponse<>(true, null, null);
  }

  public static ApiResponse<Void> failResponse(String message) {
    return new ApiResponse<>(false, null, message);
  }
}
