package com.workflow.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.workflow.model.Priority;
import com.workflow.model.Task;
import com.workflow.model.TaskStatus;

/**
 * Handles persistence of {@link Task} objects to and from a local,
 * pipe-delimited flat file using standard blocking I/O
 * ({@link BufferedReader} / {@link BufferedWriter}).
 * <p>
 * The on-disk format is one task per line:
 * </p>
 * <pre>
 * id|title|description|priority|status|createdAt|dueDate
 * </pre>
 * <p>
 * Field values are escaped so that pipe characters, backslashes, and
 * newlines embedded in user-supplied text cannot corrupt the record
 * boundaries. The {@code dueDate} field may be the literal string
 * {@code "-"} to represent {@code null} (no due date).
 * </p>
 *
 * @author Principal Java Architect
 */
public class FileStorage {

    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";
    private static final String NULL_MARKER = "-";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Path storagePath;

    /**
     * @param storagePath the file used for persistence; created on first save
     *                    if it does not already exist.
     */
    public FileStorage(Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * @param fileName the name of the storage file, resolved relative to the
     *                 current working directory.
     */
    public FileStorage(String fileName) {
        this(Path.of(fileName));
    }

    /**
     * Persists the given collection of tasks to disk, overwriting any
     * previous contents. The current directory is created if necessary.
     *
     * @param tasks the tasks to write; must not be {@code null}
     * @throws IOException if the file cannot be written
     */
    public void saveTasks(List<Task> tasks) throws IOException {
        if (storagePath.getParent() != null) {
            Files.createDirectories(storagePath.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                storagePath, StandardCharsets.UTF_8)) {
            for (Task task : tasks) {
                writer.write(serialize(task));
                writer.newLine();
            }
        }
    }

    /**
     * Loads all tasks from disk. If the storage file does not yet exist,
     * an empty list is returned rather than throwing, since that simply
     * represents a fresh installation with no prior state.
     *
     * @return the list of tasks read from disk, in file order
     * @throws IOException if the file exists but cannot be read
     */
    public List<Task> loadTasks() throws IOException {
        List<Task> tasks = new ArrayList<>();
        if (!Files.exists(storagePath)) {
            return tasks;
        }
        try (BufferedReader reader = Files.newBufferedReader(
                storagePath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    tasks.add(deserialize(line));
                } catch (RuntimeException malformed) {
                    throw new IOException(
                            "Corrupt task record on line " + lineNumber + " of " + storagePath + ": " + line,
                            malformed);
                }
            }
        }
        return tasks;
    }

    /**
     * @return the underlying storage path, primarily for diagnostic display.
     */
    public Path getStoragePath() {
        return storagePath;
    }

    // ------------------------------------------------------------------
    // Serialization helpers
    // ------------------------------------------------------------------

    private String serialize(Task task) {
        return String.join(DELIMITER,
                String.valueOf(task.getId()),
                escape(task.getTitle()),
                escape(task.getDescription()),
                task.getPriority().name(),
                task.getStatus().name(),
                task.getCreatedAt().format(TIMESTAMP_FORMAT),
                task.getDueDate() == null ? NULL_MARKER : task.getDueDate().format(TIMESTAMP_FORMAT));
    }

    private Task deserialize(String line) {
        String[] fields = line.split(DELIMITER_REGEX, -1);
        if (fields.length != 7) {
            throw new IllegalArgumentException(
                    "Expected 7 fields but found " + fields.length + " in record: " + line);
        }
        int id = Integer.parseInt(fields[0].trim());
        String title = unescape(fields[1]);
        String description = unescape(fields[2]);
        Priority priority = Priority.valueOf(fields[3].trim());
        TaskStatus status = TaskStatus.valueOf(fields[4].trim());
        LocalDateTime createdAt = LocalDateTime.parse(fields[5].trim(), TIMESTAMP_FORMAT);
        LocalDateTime dueDate = NULL_MARKER.equals(fields[6].trim())
                ? null
                : LocalDateTime.parse(fields[6].trim(), TIMESTAMP_FORMAT);
        return new Task(id, title, description, priority, status, createdAt, dueDate);
    }

    /**
     * Escapes backslashes, the field delimiter, and newlines so that
     * user-entered text can never be mistaken for a record boundary.
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(i + 1);
                switch (next) {
                    case '\\' -> { result.append('\\'); i++; }
                    case 'p' -> { result.append('|'); i++; }
                    case 'n' -> { result.append('\n'); i++; }
                    case 'r' -> { result.append('\r'); i++; }
                    default -> result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
