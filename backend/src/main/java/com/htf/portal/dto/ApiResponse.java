package com.htf.portal.dto;

/**
 * The one JSON envelope every endpoint returns: { success, message, data }.
 * T is the type of the payload, e.g. ApiResponse<TeamView> or ApiResponse<List<Hackathon>>.
 * On errors, data holds an ErrorDetails with the HTTP status and an error code.
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<ErrorDetails> error(int status, String code, String message) {
        return new ApiResponse<>(false, message, new ErrorDetails(status, code, message));
    }
}
