package com.workflow.exception;

/**
 * Checked exception thrown when a requested {@link com.workflow.model.Task}
 * cannot be located inside {@link com.workflow.service.TaskManager}.
 * <p>
 * This is deliberately a <b>checked</b> exception: callers such as the CLI
 * layer ({@link com.workflow.Main}) are expected to explicitly handle the
 * "task does not exist" case as part of normal, expected control flow
 * (a user may easily type an ID that no longer exists), rather than let it
 * propagate as an unexpected runtime failure.
 * </p>
 *
 * @author Principal Java Architect
 */
public class TaskNotFoundException extends Exception {

    private final int taskId;

    /**
     * @param taskId the identifier that could not be resolved to a task
     */
    public TaskNotFoundException(int taskId) {
        super("No task exists with ID #" + taskId + ".");
        this.taskId = taskId;
    }

    /**
     * @param taskId  the identifier that could not be resolved to a task
     * @param message a custom message overriding the default text
     */
    public TaskNotFoundException(int taskId, String message) {
        super(message);
        this.taskId = taskId;
    }

    /**
     * @return the ID that triggered this exception, useful for logging or
     *         constructing user-facing recovery prompts.
     */
    public int getTaskId() {
        return taskId;
    }
}
