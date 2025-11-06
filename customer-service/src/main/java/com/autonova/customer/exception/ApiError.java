package com.autonova.customer.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors,
        String path,
        OffsetDateTime timestamp
) {

    public record FieldError(String field, String message) { }
}
