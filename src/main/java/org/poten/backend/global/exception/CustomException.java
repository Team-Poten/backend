package org.poten.backend.global.exception;

import lombok.Getter;
import org.poten.backend.global.error.ErrorCode;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
