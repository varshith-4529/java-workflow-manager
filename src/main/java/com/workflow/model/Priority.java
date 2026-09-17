package com.workflow.model;

/**
 * Represents the urgency level assigned to a {@link Task}.
 * <p>
 * The natural ordering of this enum (its declaration order) is deliberately
 * arranged from lowest to highest severity so that {@code Priority.LOW.compareTo(Priority.HIGH) < 0}.
 * A numeric {@code weight} is also exposed so that consumers such as
 * {@link com.workflow.service.TaskManager}, which need "highest priority first"
 * ordering inside a {@link java.util.PriorityQueue}, can invert the comparison
 * without relying on enum ordinal tricks scattered across the codebase.
 * </p>
 *
 * @author Principal Java Architect
 */
public enum Priority {

    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");

    private final int weight;
    private final String displayName;

    Priority(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    /**
     * @return the numeric weight of this priority; higher values indicate
     *         a more urgent task.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @return a human-friendly label suitable for CLI output.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves a {@link Priority} from a case-insensitive string, accepting
     * either the enum name (e.g. {@code "HIGH"}) or the display name
     * (e.g. {@code "High"}).
     *
     * @param input the raw user-supplied text
     * @return the matching {@link Priority}
     * @throws com.workflow.exception.InvalidTaskException if no priority matches
     */
    public static Priority fromString(String input) {
        if (input == null || input.isBlank()) {
            throw new com.workflow.exception.InvalidTaskException(
                    "Priority value cannot be empty. Valid options: LOW, MEDIUM, HIGH, CRITICAL.");
        }
        String normalized = input.trim().toUpperCase();
        for (Priority priority : values()) {
            if (priority.name().equals(normalized) || priority.displayName.equalsIgnoreCase(input.trim())) {
                return priority;
            }
        }
        throw new com.workflow.exception.InvalidTaskException(
                "Unrecognized priority '" + input + "'. Valid options: LOW, MEDIUM, HIGH, CRITICAL.");
    }

    @Override
    public String toString() {
        return displayName;
    }
}
