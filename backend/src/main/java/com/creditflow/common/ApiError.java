package com.creditflow.common;

import java.time.Instant;

/** Uniform error body returned to API clients. */
public record ApiError(Instant timestamp, int status, String error, String message) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message);
    }
}
