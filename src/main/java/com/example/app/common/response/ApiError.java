package com.example.app.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private List<ValidationError> validationErrors;
    private Map<String, Object> details;

    public static ApiError of(String code, String message) {
        return ApiError.builder()
            .code(code)
            .message(message)
            .build();
    }

    public static ApiError of(String code, String message, List<ValidationError> validationErrors) {
        return ApiError.builder()
            .code(code)
            .message(message)
            .validationErrors(validationErrors)
            .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String field;
        private String message;
        private Object rejectedValue;

    }

}