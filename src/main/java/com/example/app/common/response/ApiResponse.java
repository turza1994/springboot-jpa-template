package com.example.app.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;
    private T data;
    private ApiError error;
    private String requestId;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .requestId(requestId)
            .timestamp(Instant.now())
            .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    public static <T> ApiResponse<T> error(ApiError error, String requestId) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(error)
            .requestId(requestId)
            .timestamp(Instant.now())
            .build();
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return error(error, null);
    }

}