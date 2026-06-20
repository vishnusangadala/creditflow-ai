package com.creditflow.common;

/** Thrown when a requested entity (e.g. a workflow) does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
