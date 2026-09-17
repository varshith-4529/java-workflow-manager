package com.workflow.exception;

/**
 * Unchecked exception thrown when data supplied to construct or mutate a
 * {@link com.workflow.model.Task} violates the model's invariants (for
 * example, a blank title, a {@code null} priority, or an unparsable
 * priority/status string typed at the CLI).
 * <p>
 * This is deliberately an <b>unchecked</b> exception: it signals a
 * programming or input-validation error rather than an expected recoverable
 * condition, so callers are not forced to declare it in every method
 * signature. The CLI layer still catches it at the top of each command
 * handler to present a friendly error message instead of crashing.
 * </p>
 *
 * @author Principal Java Architect
 */
public class InvalidTaskException extends RuntimeException {

    public InvalidTaskException(String message) {
        super(message);
    }

    public InvalidTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
