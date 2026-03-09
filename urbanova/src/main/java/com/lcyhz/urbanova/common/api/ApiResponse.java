package com.lcyhz.urbanova.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiError error;
    private ApiMeta meta;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, ApiMeta.now());
    }

    public static ApiResponse<Void> failure(String code, String message, Object details) {
        return new ApiResponse<>(false, null, new ApiError(code, message, details), ApiMeta.now());
    }
}

