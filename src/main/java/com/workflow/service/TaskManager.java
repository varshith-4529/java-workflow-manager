package com.workflow.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.workflow.exception.TaskNotFoundException;
import com.workflow.model.Priority;
import com.workflow.model.Task;
import com.workflow.model.TaskStatus;
import com.workflow.util.FileStorage;

/**
 * Core business-logic layer for the Task &amp; Workflow Engine.
 * <p>
 * Internally, {@code TaskManager} maintains two complementary views of the
 * same data:
 * </p>
 * <ul>
 *   <li>a {@link HashMap} keyed by task ID, giving O(1) average lookup,
 *       update, and removal by ID; and</li>
 *   <li>a {@link PriorityQueue} ordered by {@link Task#compareTo(Task)},
 *       giving O(log n) insertion and O(1) access to the single most urgent
 *       task via {@link #peekNextTask()}.</li>
 * </ul>
 * <p>
 * Because {@link PriorityQueue} does not support efficient arbitrary
 * removal or re-prioritization, the queue is treated as a
 * <b>derived, rebuildable index</b>: any operation that changes a task's
 * priority or removes a task rebuilds the queue from the authoritative
 * {@code HashMap} in O(n log n). This keeps the two structures consistent
 * without the complexity of a custom indexed heap, which is an appropriate
 * trade-off for a single-user CLI tool.
 * </p>
 *
 * @author Principal Java Architect
 */
public class TaskManager {

    private final Map<Integer, Task> tasksById;
    private PriorityQueue<Task> priorityQueue;
    private final AtomicInteger idSequence;
    private final FileStorage fileStorage;

    /**
     * Constructs a manager backed by the given {@link FileStorage}, loading
     * any previously persisted tasks immediately.
     *
     * @param fileStorage the persistence handler to load from / save to
     * @throws IOException if the storage file exists but cannot be read
     */
    public TaskManager(FileStorage fileStorage) throws IOException {
        this.fileStorage = fileStorage;
        this.tasksById = new HashMap<>();
        this.priorityQueue = new PriorityQueue<>();
        this.idSequence = new AtomicInteger(0);
        loadFromDisk();
    }

    // ------------------------------------------------------------------
    // Mutating operations
    // ------------------------------------------------------------------

    /**
     * Creates and stores a new task, assigning it the next available ID.
     *
     * @param title       non-blank task title
     * @param description free-text description (may be blank)
     * @param priority    the urgency level
     * @param dueDate     an optional due date, or {@code null}
     * @return the newly created, fully populated {@link Task}
     */
    public Task addTask(String title, String description, Priority priority, LocalDateTime dueDate) {
        int newId = idSequence.incrementAndGet();
        Task task = new Task(newId, title, description, priority, dueDate);
        tasksById.put(newId, task);
        priorityQueue.add(task);
        return task;
    }

    /**
     * Updates the {@link TaskStatus} of an existing task.
     *
     * @param id        the target task's ID
     * @param newStatus the status to apply
     * @return the updated task
     * @throws TaskNotFoundException if no task exists with the given ID
     */
    public Task updateStatus(int id, TaskStatus newStatus) throws TaskNotFoundException {
        Task task = getTaskOrThrow(id);
        task.setStatus(newStatus);
        return task;
    }

    /**
     * Updates the {@link Priority} of an existing task and rebuilds the
     * internal priority queue to reflect the new ordering.
     *
     * @param id          the target task's ID
     * @param newPriority the priority to apply
     * @return the updated task
     * @throws TaskNotFoundException if no task exists with the given ID
     */
    public Task updatePriority(int id, Priority newPriority) throws TaskNotFoundException {
        Task task = getTaskOrThrow(id);
        task.setPriority(newPriority);
        rebuildQueue();
        return task;
    }

    /**
     * Updates the title, description, and/or due date of an existing task.
     * Any parameter passed as {@code null} leaves the corresponding field
     * unchanged, except {@code dueDate}, where {@code null} explicitly
     * clears the due date (use {@link #getTask(int)} first if you need to
     * distinguish "no change" from "clear").
     *
     * @param id             the target task's ID
     * @param newTitle       replacement title, or {@code null} to leave unchanged
     * @param newDescription replacement description, or {@code null} to leave unchanged
     * @param clearDueDate   if {@code true}, clears the due date instead of setting it
     * @param newDueDate     replacement due date, ignored if {@code clearDueDate} is {@code true}
     * @return the updated task
     * @throws TaskNotFoundException if no task exists with the given ID
     */
    public Task editTask(int id, String newTitle, String newDescription,
                          boolean clearDueDate, LocalDateTime newDueDate) throws TaskNotFoundException {
        Task task = getTaskOrThrow(id);
        if (newTitle != null) {
            task.setTitle(newTitle);
        }
        if (newDescription != null) {
            task.setDescription(newDescription);
        }
        if (clearDueDate) {
            task.setDueDate(null);
        } else if (newDueDate != null) {
            task.setDueDate(newDueDate);
        }
        return task;
    }

    /**
     * Permanently removes a task.
     *
     * @param id the target task's ID
     * @throws TaskNotFoundException if no task exists with the given ID
     */
    public void deleteTask(int id) throws TaskNotFoundException {
        Task removed = tasksById.remove(id);
        if (removed == null) {
            throw new TaskNotFoundException(id);
        }
        rebuildQueue();
    }

    // ------------------------------------------------------------------
    // Query operations
    // ------------------------------------------------------------------

    /**
     * @param id the target task's ID
     * @return the matching task
     * @throws TaskNotFoundException if no task exists with the given ID
     */
    public Task getTask(int id) throws TaskNotFoundException {
        return getTaskOrThrow(id);
    }

    /**
     * @return all tasks ordered by priority (highest first), then by due date
     */
    public List<Task> listAllTasksByPriority() {
        List<Task> snapshot = new ArrayList<>(tasksById.values());
        snapshot.sort(Comparator.naturalOrder());
        return snapshot;
    }

    /**
     * @return all tasks ordered by ID (i.e. creation order)
     */
    public List<Task> listAllTasksById() {
        return tasksById.values().stream()
                .sorted(Comparator.comparingInt(Task::getId))
                .collect(Collectors.toList());
    }

    /**
     * @param status the status to filter by
     * @return all tasks with the given status, ordered by priority
     */
    public List<Task> listByStatus(TaskStatus status) {
        return tasksById.values().stream()
                .filter(task -> task.getStatus() == status)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    /**
     * @param priority the priority to filter by
     * @return all tasks with the given priority, ordered by due date
     */
    public List<Task> listByPriority(Priority priority) {
        return tasksById.values().stream()
                .filter(task -> task.getPriority() == priority)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    /**
     * @return all non-terminal tasks whose due date has passed, ordered by
     *         how overdue they are (most overdue first)
     */
    public List<Task> listOverdueTasks() {
        return tasksById.values().stream()
                .filter(Task::isOverdue)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    /**
     * Performs a case-insensitive substring search across each task's title
     * and description.
     *
     * @param keyword the text to search for
     * @return matching tasks, ordered by priority
     */
    public List<Task> searchTasks(String keyword) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase();
        return tasksById.values().stream()
                .filter(task -> task.getTitle().toLowerCase().contains(needle)
                        || task.getDescription().toLowerCase().contains(needle))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    /**
     * Returns, without removing, the single highest-priority task currently
     * tracked, i.e. the task at the head of the internal
     * {@link PriorityQueue}.
     *
     * @return the most urgent task, or {@code null} if no tasks exist
     */
    public Task peekNextTask() {
        return priorityQueue.peek();
    }

    /**
     * @return summary counts of tasks grouped by {@link TaskStatus}, computed
     *         via the Streams API
     */
    public Map<TaskStatus, Long> getStatusSummary() {
        return tasksById.values().stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
    }

    /**
     * @return the total number of tasks currently tracked
     */
    public int size() {
        return tasksById.size();
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    /**
     * Persists the current in-memory state to disk immediately.
     *
     * @throws IOException if the write fails
     */
    public void saveToDisk() throws IOException {
        fileStorage.saveTasks(new ArrayList<>(tasksById.values()));
    }

    private void loadFromDisk() throws IOException {
        List<Task> loaded = fileStorage.loadTasks();
        int maxId = 0;
        for (Task task : loaded) {
            tasksById.put(task.getId(), task);
            maxId = Math.max(maxId, task.getId());
        }
        idSequence.set(maxId);
        rebuildQueue();
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private Task getTaskOrThrow(int id) throws TaskNotFoundException {
        Task task = tasksById.get(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    private void rebuildQueue() {
        this.priorityQueue = new PriorityQueue<>(Math.max(tasksById.size(), 1));
        this.priorityQueue.addAll(tasksById.values());
    }
}
