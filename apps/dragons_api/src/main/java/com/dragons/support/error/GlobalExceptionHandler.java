package com.dragons.support.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.dragons.support.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {



  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadParameter(MethodArgumentTypeMismatchException e) {
    String value = e.getValue() == null ? "null" : String.valueOf(e.getValue());
    String message = String.format("요청 파라미터 '%s' 값 '%s'이(가) 올바르지 않습니다.", e.getName(), value);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.failResponse(message));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.failResponse("존재하지 않는 API입니다."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.failResponse("일시적인 오류가 발생했습니다."));
  }
}
