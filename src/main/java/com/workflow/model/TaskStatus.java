package com.workflow.model;

/**
 * Represents the lifecycle state of a {@link Task}.
 * <p>
 * Valid transitions are enforced by {@link com.workflow.service.TaskManager},
 * not by this enum itself; this type is intentionally a simple, immutable
 * classification of state.
 * </p>
 *
 * @author Principal Java Architect
 */
public enum TaskStatus {

    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves a {@link TaskStatus} from a case-insensitive string, accepting
     * either the enum name (e.g. {@code "IN_PROGRESS"}) or the display name
     * (e.g. {@code "In Progress"}).
     *
     * @param input the raw user-supplied text
     * @return the matching {@link TaskStatus}
     * @throws com.workflow.exception.InvalidTaskException if no status matches
     */
    public static TaskStatus fromString(String input) {
        if (input == null || input.isBlank()) {
            throw new com.workflow.exception.InvalidTaskException(
                    "Status value cannot be empty. Valid options: PENDING, IN_PROGRESS, COMPLETED, CANCELLED.");
        }
        String normalized = input.trim().toUpperCase().replace(' ', '_');
        for (TaskStatus status : values()) {
            if (status.name().equals(normalized) || status.displayName.equalsIgnoreCase(input.trim())) {
                return status;
            }
        }
        throw new com.workflow.exception.InvalidTaskException(
                "Unrecognized status '" + input + "'. Valid options: PENDING, IN_PROGRESS, COMPLETED, CANCELLED.");
    }

    @Override
    public String toString() {
        return displayName;
    }
}
