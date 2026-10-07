package com.unad.project_video_platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private Integer code;
    private String status;
    private String message;
    private String error;
    private T data;

    public static <T> ApiResponse<T> success(Integer code, String status, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .status(status)
                .message(message)
                .data(data)
                .error(null)
                .build();
    }

    public static <T> ApiResponse<T> error(Integer code, String status, String message, String error) {
        return ApiResponse.<T>builder()
                .code(code)
                .status(status)
                .message(message)
                .error(error)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success(200, "OK", "Operation successful", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return success(200, "OK", message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return success(201, "CREATED", message, data);
    }

    public static <T> ApiResponse<T> badRequest(String error) {
        return error(400, "BAD REQUEST", "Bad Request: Invalid input", error);
    }

    public static <T> ApiResponse<T> notFound(String error) {
        return error(404, "NOT FOUND", "NOT FOUND", error);
    }

    public static <T> ApiResponse<T> internalError(String detail) {
        org.slf4j.LoggerFactory.getLogger(ApiResponse.class).error("Error interno: {}", detail);
        return error(500, "INTERNAL SERVER ERROR", "INTERNAL SERVER ERROR", "Error interno del servidor");
    }

    public static <T> ApiResponse<T> unauthorized(String error) {
        return error(401, "UNAUTHORIZED", "Unauthorized: Invalid or missing token", error);
    }

    public static <T> ApiResponse<T> forbidden(String error) {
        return error(403, "FORBIDDEN", "Forbidden: You do not have permission", error);
    }
}
