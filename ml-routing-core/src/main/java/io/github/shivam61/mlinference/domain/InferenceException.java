package io.github.shivam61.mlinference.domain;

/**
 * Base exception for all SDK-related errors.
 */
public class InferenceException extends RuntimeException {
    public InferenceException(String message) { super(message); }
    public InferenceException(String message, Throwable cause) { super(message, cause); }
}
