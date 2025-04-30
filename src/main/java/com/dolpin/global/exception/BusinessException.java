package com.dolpin.global.exception;

import com.dolpin.global.response.ResponseStatus;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ResponseStatus responseStatus;

    public BusinessException(ResponseStatus responseStatus) {
        super(responseStatus.getMessage());
        this.responseStatus = responseStatus;
    }

    public BusinessException(ResponseStatus responseStatus, String message) {
        super(message);
        this.responseStatus = responseStatus.withMessage(message);
    }
}