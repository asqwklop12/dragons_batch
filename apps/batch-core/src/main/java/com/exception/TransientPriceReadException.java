package com.exception;

public class TransientPriceReadException extends RuntimeException {

  public TransientPriceReadException(String message, Throwable cause) {
    super(message, cause);
  }
}
