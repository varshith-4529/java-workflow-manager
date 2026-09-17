package com.workflow;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.workflow.exception.InvalidTaskException;
import com.workflow.exception.TaskNotFoundException;
import com.workflow.model.Priority;
import com.workflow.model.Task;
import com.workflow.model.TaskStatus;
import com.workflow.service.TaskManager;
import com.workflow.util.FileStorage;

/**
 * Command-line entry point for the Task &amp; Workflow Engine.
 * <p>
 * This class owns the interactive read-menu-execute loop, all
 * {@link Scanner}-based input parsing and validation, and CLI-formatted
 * output. It deliberately contains no business logic of its own; every
 * command delegates to {@link TaskManager}, keeping presentation and
 * domain logic cleanly separated.
 * </p>
 *
 * @author Principal Java Architect
 */
public final class Main {

    private static final String STORAGE_FILE_NAME = "tasks.dat";
    private static final DateTimeFormatter INPUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner scanner;
    private final TaskManager taskManager;

    private Main(Scanner scanner, TaskManager taskManager) {
        this.scanner = scanner;
        this.taskManager = taskManager;
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   TASK & WORKFLOW ENGINE  --  CLI Edition v1.0");
        System.out.println("==================================================");

        Scanner scanner = new Scanner(System.in);
        FileStorage fileStorage = new FileStorage(STORAGE_FILE_NAME);

        TaskManager taskManager;
        try {
            taskManager = new TaskManager(fileStorage);
        } catch (IOException e) {
            System.err.println("FATAL: Could not load persisted tasks from '"
                    + fileStorage.getStoragePath() + "': " + e.getMessage());
            System.err.println("Resolve or remove the corrupt file and restart the application.");
            return;
        }

        System.out.println("Loaded " + taskManager.size() + " task(s) from " + fileStorage.getStoragePath());

        Main app = new Main(scanner, taskManager);
        app.run();

        scanner.close();
    }

    /**
     * Runs the primary menu loop until the user chooses to exit.
     */
    private void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = promptLine("Select an option: ").trim();
            try {
                switch (choice) {
                    case "1" -> addTaskFlow();
                    case "2" -> listAllTasksFlow();
                    case "3" -> viewTaskFlow();
                    case "4" -> updateStatusFlow();
                    case "5" -> updatePriorityFlow();
                    case "6" -> editTaskFlow();
                    case "7" -> deleteTaskFlow();
                    case "8" -> searchTasksFlow();
                    case "9" -> filterByStatusFlow();
                    case "10" -> listOverdueFlow();
                    case "11" -> showNextTaskFlow();
                    case "12" -> showSummaryFlow();
                    case "13" -> saveFlow();
                    case "0" -> running = !confirmExit();
                    default -> System.out.println("Unrecognized option '" + choice + "'. Please try again.");
                }
            } catch (InvalidTaskException e) {
                System.out.println("[Validation Error] " + e.getMessage());
            } catch (TaskNotFoundException e) {
                System.out.println("[Not Found] " + e.getMessage());
            } catch (IOException e) {
                System.out.println("[I/O Error] " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("--------------------------------------------------");
        System.out.println(" 1. Add a new task");
        System.out.println(" 2. List all tasks (by priority)");
        System.out.println(" 3. View task details");
        System.out.println(" 4. Update task status");
        System.out.println(" 5. Update task priority");
        System.out.println(" 6. Edit task (title / description / due date)");
        System.out.println(" 7. Delete a task");
        System.out.println(" 8. Search tasks by keyword");
        System.out.println(" 9. Filter tasks by status");
        System.out.println("10. List overdue tasks");
        System.out.println("11. Show next most urgent task");
        System.out.println("12. Show status summary");
        System.out.println("13. Save now");
        System.out.println(" 0. Save and exit");
        System.out.println("--------------------------------------------------");
    }

    // ------------------------------------------------------------------
    // Command flows
    // ------------------------------------------------------------------

    private void addTaskFlow() {
        System.out.println("\n-- Add New Task --");
        String title = promptNonBlank("Title: ");
        String description = promptLine("Description (optional): ");
        Priority priority = promptPriority("Priority [LOW/MEDIUM/HIGH/CRITICAL]: ");
        LocalDateTime dueDate = promptOptionalDateTime(
                "Due date (yyyy-MM-dd HH:mm), leave blank for none: ");

        Task task = taskManager.addTask(title, description, priority, dueDate);
        System.out.println("Created task:");
        System.out.println("  " + task);
    }

    private void listAllTasksFlow() {
        System.out.println("\n-- All Tasks (by priority) --");
        printTaskList(taskManager.listAllTasksByPriority());
    }

    private void viewTaskFlow() throws TaskNotFoundException {
        int id = promptInt("Task ID: ");
        Task task = taskManager.getTask(id);
        System.out.println("\n-- Task Details --");
        System.out.println("  " + task);
    }

    private void updateStatusFlow() throws TaskNotFoundException {
        int id = promptInt("Task ID: ");
        TaskStatus status = promptStatus("New status [PENDING/IN_PROGRESS/COMPLETED/CANCELLED]: ");
        Task updated = taskManager.updateStatus(id, status);
        System.out.println("Updated: " + updated);
    }

    private void updatePriorityFlow() throws TaskNotFoundException {
        int id = promptInt("Task ID: ");
        Priority priority = promptPriority("New priority [LOW/MEDIUM/HIGH/CRITICAL]: ");
        Task updated = taskManager.updatePriority(id, priority);
        System.out.println("Updated: " + updated);
    }

    private void editTaskFlow() throws TaskNotFoundException {
        int id = promptInt("Task ID: ");
        taskManager.getTask(id); // validates existence early with a clear error

        String title = promptLine("New title (blank = keep current): ");
        String description = promptLine("New description (blank = keep current): ");
        String clearChoice = promptLine("Clear due date? (y/N): ").trim().toLowerCase();
        boolean clearDueDate = clearChoice.equals("y") || clearChoice.equals("yes");

        LocalDateTime newDueDate = null;
        if (!clearDueDate) {
            newDueDate = promptOptionalDateTime(
                    "New due date (yyyy-MM-dd HH:mm), blank = keep current: ");
        }

        Task updated = taskManager.editTask(
                id,
                title.isBlank() ? null : title,
                description.isBlank() ? null : description,
                clearDueDate,
                newDueDate);
        System.out.println("Updated: " + updated);
    }

    private void deleteTaskFlow() throws TaskNotFoundException {
        int id = promptInt("Task ID to delete: ");
        Task existing = taskManager.getTask(id);
        String confirm = promptLine("Delete \"" + existing.getTitle() + "\"? (y/N): ").trim().toLowerCase();
        if (confirm.equals("y") || confirm.equals("yes")) {
            taskManager.deleteTask(id);
            System.out.println("Task #" + id + " deleted.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void searchTasksFlow() {
        String keyword = promptLine("Search keyword: ");
        List<Task> results = taskManager.searchTasks(keyword);
        System.out.println("\n-- Search Results (" + results.size() + ") --");
        printTaskList(results);
    }

    private void filterByStatusFlow() {
        TaskStatus status = promptStatus("Filter by status [PENDING/IN_PROGRESS/COMPLETED/CANCELLED]: ");
        List<Task> results = taskManager.listByStatus(status);
        System.out.println("\n-- Tasks with status " + status.getDisplayName() + " --");
        printTaskList(results);
    }

    private void listOverdueFlow() {
        List<Task> overdue = taskManager.listOverdueTasks();
        System.out.println("\n-- Overdue Tasks --");
        printTaskList(overdue);
    }

    private void showNextTaskFlow() {
        Task next = taskManager.peekNextTask();
        System.out.println("\n-- Next Most Urgent Task --");
        if (next == null) {
            System.out.println("  No tasks tracked.");
        } else {
            System.out.println("  " + next);
        }
    }

    private void showSummaryFlow() {
        Map<TaskStatus, Long> summary = taskManager.getStatusSummary();
        System.out.println("\n-- Status Summary --");
        System.out.println("  Total tasks: " + taskManager.size());
        for (TaskStatus status : TaskStatus.values()) {
            long count = summary.getOrDefault(status, 0L);
            System.out.printf("  %-12s : %d%n", status.getDisplayName(), count);
        }
    }

    private void saveFlow() throws IOException {
        taskManager.saveToDisk();
        System.out.println("State saved to " + STORAGE_FILE_NAME + ".");
    }

    private boolean confirmExit() {
        try {
            taskManager.saveToDisk();
            System.out.println("State saved to " + STORAGE_FILE_NAME + ".");
        } catch (IOException e) {
            System.out.println("[I/O Error] Could not save before exit: " + e.getMessage());
            String proceed = promptLine("Exit anyway without saving? (y/N): ").trim().toLowerCase();
            return proceed.equals("y") || proceed.equals("yes");
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Display helpers
    // ------------------------------------------------------------------

    private void printTaskList(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("  (no tasks to display)");
            return;
        }
        for (Task task : tasks) {
            System.out.println("  " + task);
        }
    }

    // ------------------------------------------------------------------
    // Input helpers (validation lives here, at the CLI boundary)
    // ------------------------------------------------------------------

    private String promptLine(String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine();
        return line == null ? "" : line;
    }

    private String promptNonBlank(String prompt) {
        while (true) {
            String value = promptLine(prompt);
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("  This field cannot be blank. Please try again.");
        }
    }

    private int promptInt(String prompt) {
        while (true) {
            String value = promptLine(prompt).trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("  '" + value + "' is not a valid integer. Please try again.");
            }
        }
    }

    private Priority promptPriority(String prompt) {
        while (true) {
            String value = promptLine(prompt).trim();
            try {
                return Priority.fromString(value);
            } catch (InvalidTaskException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private TaskStatus promptStatus(String prompt) {
        while (true) {
            String value = promptLine(prompt).trim();
            try {
                return TaskStatus.fromString(value);
            } catch (InvalidTaskException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private LocalDateTime promptOptionalDateTime(String prompt) {
        while (true) {
            String value = promptLine(prompt).trim();
            if (value.isBlank()) {
                return null;
            }
            try {
                return LocalDateTime.parse(value, INPUT_DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("  '" + value + "' does not match the expected format yyyy-MM-dd HH:mm. "
                        + "Example: 2026-09-20 17:30. Please try again.");
            }
        }
    }
}
