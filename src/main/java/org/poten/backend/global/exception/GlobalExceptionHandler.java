package org.poten.backend.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.poten.backend.global.error.ErrorResponse;
import org.poten.backend.global.error.GlobalErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException: {}", e.getErrorCode());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException: {}", e.getMessage());
        return ResponseEntity
                .status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }
}
