package com.workflow.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.workflow.exception.InvalidTaskException;

/**
 * Immutable identity, mutable state model representing a single unit of work
 * inside the Task &amp; Workflow Engine.
 * <p>
 * A {@code Task} encapsulates all data associated with a piece of work: its
 * title, description, {@link Priority}, {@link TaskStatus}, creation
 * timestamp, and an optional due date. The class implements
 * {@link Comparable} so that instances can be placed directly into a
 * {@link java.util.PriorityQueue}; tasks are ordered first by descending
 * priority (most urgent first) and then by ascending due date (soonest
 * first), with tasks lacking a due date sorted last.
 * </p>
 *
 * @author Principal Java Architect
 */
public class Task implements Comparable<Task> {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final int id;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime dueDate;

    /**
     * Constructs a new task in {@link TaskStatus#PENDING} state with the
     * creation timestamp set to now.
     *
     * @param id          the unique, manager-assigned identifier
     * @param title       a short, non-blank title
     * @param description a longer free-text description (may be blank)
     * @param priority    the urgency level; must not be {@code null}
     * @param dueDate     an optional due date; may be {@code null}
     * @throws InvalidTaskException if {@code title} is blank or {@code priority} is {@code null}
     */
    public Task(int id, String title, String description, Priority priority, LocalDateTime dueDate) {
        this(id, title, description, priority, TaskStatus.PENDING, LocalDateTime.now(), dueDate);
    }

    /**
     * Full constructor, primarily used when rehydrating a {@code Task} from
     * persistent storage where the original {@code createdAt} and
     * {@code status} must be preserved.
     *
     * @param id          the unique identifier
     * @param title       a short, non-blank title
     * @param description a longer free-text description (may be blank)
     * @param priority    the urgency level; must not be {@code null}
     * @param status      the lifecycle status; must not be {@code null}
     * @param createdAt   the original creation timestamp; must not be {@code null}
     * @param dueDate     an optional due date; may be {@code null}
     * @throws InvalidTaskException if any required field is invalid
     */
    public Task(int id, String title, String description, Priority priority,
                TaskStatus status, LocalDateTime createdAt, LocalDateTime dueDate) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("Task title cannot be blank.");
        }
        if (priority == null) {
            throw new InvalidTaskException("Task priority cannot be null.");
        }
        if (status == null) {
            throw new InvalidTaskException("Task status cannot be null.");
        }
        if (createdAt == null) {
            throw new InvalidTaskException("Task creation timestamp cannot be null.");
        }
        this.id = id;
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
    }

    // ------------------------------------------------------------------
    // Accessors / Mutators
    // ------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("Task title cannot be blank.");
        }
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        if (priority == null) {
            throw new InvalidTaskException("Task priority cannot be null.");
        }
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        if (status == null) {
            throw new InvalidTaskException("Task status cannot be null.");
        }
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * @return {@code true} if this task has a due date in the past and is
     *         not yet {@link TaskStatus#COMPLETED} or {@link TaskStatus#CANCELLED}.
     */
    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        boolean terminal = status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED;
        return !terminal && dueDate.isBefore(LocalDateTime.now());
    }

    /**
     * Defines the natural ordering used by the {@link java.util.PriorityQueue}
     * inside {@link com.workflow.service.TaskManager}: higher {@link Priority}
     * weight comes first, ties are broken by the earlier due date, and tasks
     * without a due date sort after tasks that have one.
     */
    @Override
    public int compareTo(Task other) {
        int priorityComparison = Integer.compare(other.priority.getWeight(), this.priority.getWeight());
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        if (this.dueDate == null && other.dueDate == null) {
            return Integer.compare(this.id, other.id);
        }
        if (this.dueDate == null) {
            return 1;
        }
        if (other.dueDate == null) {
            return -1;
        }
        int dueDateComparison = this.dueDate.compareTo(other.dueDate);
        if (dueDateComparison != 0) {
            return dueDateComparison;
        }
        return Integer.compare(this.id, other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String dueDateText = dueDate == null ? "None" : dueDate.format(DISPLAY_FORMAT);
        return String.format(
                "[#%d] %-30s | Priority: %-8s | Status: %-11s | Due: %-16s | Created: %s%s",
                id, truncate(title, 30), priority.getDisplayName(), status.getDisplayName(),
                dueDateText, createdAt.format(DISPLAY_FORMAT), isOverdue() ? "  ** OVERDUE **" : "");
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
