package com.bdsystems.bdsystems_api.auth.exceptions;

public record ApiError(
    int status,
    String message,
    String field
) {
}
