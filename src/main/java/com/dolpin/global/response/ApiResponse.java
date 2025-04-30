package com.dolpin.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final String message;
    private final T data;
    private final String code;

    private ApiResponse(String message, T data, String code) {
        this.message = message;
        this.data = data;
        this.code = code;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, null);
    }

    public static <T> ApiResponse<T> success(ResponseStatus status, T data) {
        return new ApiResponse<>(status.getMessage(), data, status.name());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null, null);
    }

    public static <T> ApiResponse<T> error(ResponseStatus status) {
        return new ApiResponse<>(status.getMessage(), null, status.name());
    }
}