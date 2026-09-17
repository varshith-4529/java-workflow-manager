# java-workflow-manager
Java-based core engine for task prioritization, state tracking, and local workflow persistence.

A headless, terminal-based Task and Workflow Management System written in
pure Java 17. The engine lets a user create, prioritize, track, search, and
persist tasks entirely from the command line — no GUI, no external services,
no database server.

Repository: `https://github.com/varshith-4529/java-workflow-manager`
---

## Project Overview

Task & Workflow Engine models the everyday problem of managing a personal or
team backlog: work items arrive with varying urgency, need to move through a
lifecycle (`PENDING` → `IN_PROGRESS` → `COMPLETED`/`CANCELLED`), and must
survive between sessions. The application demonstrates how a small set of
core Java tools — the Collections Framework, the Streams API, custom
exceptions, and file-based I/O — combine to build a robust, layered CLI
application without any third-party frameworks.

## System Features

- **Create, view, edit, and delete tasks** with title, description, priority,
  status, creation timestamp, and an optional due date.
- **Priority-aware scheduling** via a `PriorityQueue`, so the single most
  urgent task can always be retrieved in O(1).
- **O(1) average lookup by ID** via a `HashMap` index.
- **Status lifecycle management** (`PENDING`, `IN_PROGRESS`, `COMPLETED`,
  `CANCELLED`) with validation on every transition.
- **Overdue detection** — tasks with a past due date that are still open are
  flagged automatically.
- **Keyword search** and **status/priority filtering**, implemented with the
  Streams API.
- **Status summary dashboard** showing task counts per status.
- **Durable persistence** to a local flat file (`tasks.dat`) using
  `BufferedReader`/`BufferedWriter`, so state survives across runs.
- **Defensive input validation** at every CLI prompt, with retry-until-valid
  loops instead of crashes.
- **Custom exception hierarchy**: a checked `TaskNotFoundException` for
  expected "not found" conditions and an unchecked `InvalidTaskException`
  for malformed input/state.

## Architecture at a Glance

```
com.workflow
├── Main                       CLI loop, menus, input validation, display
├── model
│   ├── Priority                Enum: LOW, MEDIUM, HIGH, CRITICAL
│   ├── TaskStatus               Enum: PENDING, IN_PROGRESS, COMPLETED, CANCELLED
│   └── Task                     Domain model, Comparable<Task>
├── exception
│   ├── TaskNotFoundException     Checked — expected "not found" condition
│   └── InvalidTaskException      Unchecked — invalid data/state
├── util
│   └── FileStorage               Reads/writes tasks.dat via buffered I/O
└── service
    └── TaskManager                Business logic: HashMap + PriorityQueue
```

## Prerequisites

- **JDK 17** or later (verify with `java -version`)
- **Apache Maven 3.8+** (verify with `mvn -version`)
- A terminal / shell (bash, zsh, PowerShell, or cmd.exe all work)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/varshith-4529/java-workflow-manager.git
cd java-workflow-manager```

### 2. Compile the project

```bash
mvn clean package
```

This produces an executable jar at `target/app.jar` (the `finalName` is
fixed to `app` in `pom.xml`, and the manifest's `Main-Class` is set to
`com.workflow.Main`).

### 3. Run the application

```bash
java -jar target/app.jar
```

On first run, the engine will report that it loaded `0` tasks; a
`tasks.dat` file will be created in the current working directory the first
time you save.

### 4. (Optional) Run tests

```bash
mvn test
```

---

## Sample CLI Interaction

```
==================================================
   JAVA-WORKLOW-MANAGER  --  CLI Edition v1.0
==================================================
Loaded 0 task(s) from tasks.dat

--------------------------------------------------
 1. Add a new task
 2. List all tasks (by priority)
 3. View task details
 4. Update task status
 5. Update task priority
 6. Edit task (title / description / due date)
 7. Delete a task
 8. Search tasks by keyword
 9. Filter tasks by status
10. List overdue tasks
11. Show next most urgent task
12. Show status summary
13. Save now
 0. Save and exit
--------------------------------------------------
Select an option: 1

-- Add New Task --
Title: Finalize sprint retrospective slides
Description (optional): Summarize velocity, blockers, and action items
Priority [LOW/MEDIUM/HIGH/CRITICAL]: HIGH
Due date (yyyy-MM-dd HH:mm), leave blank for none: 2026-09-15 17:00
Created task:
  [#1] Finalize sprint retrospective slides | Priority: High     | Status: Pending     | Due: 2026-09-15 17:00 | Created: 2026-09-11 09:04

Select an option: 1

-- Add New Task --
Title: Fix flaky CI pipeline
Description (optional): Intermittent timeout in integration test suite
Priority [LOW/MEDIUM/HIGH/CRITICAL]: CRITICAL
Due date (yyyy-MM-dd HH:mm), leave blank for none: 2026-09-12 12:00
Created task:
  [#2] Fix flaky CI pipeline            | Priority: Critical | Status: Pending     | Due: 2026-09-12 12:00 | Created: 2026-09-11 09:05

Select an option: 11

-- Next Most Urgent Task --
  [#2] Fix flaky CI pipeline            | Priority: Critical | Status: Pending     | Due: 2026-09-12 12:00 | Created: 2026-09-11 09:05

Select an option: 4
Task ID: 2
New status [PENDING/IN_PROGRESS/COMPLETED/CANCELLED]: IN_PROGRESS
Updated: [#2] Fix flaky CI pipeline            | Priority: Critical | Status: In Progress | Due: 2026-09-12 12:00 | Created: 2026-09-11 09:05

Select an option: 2

-- All Tasks (by priority) --
  [#2] Fix flaky CI pipeline            | Priority: Critical | Status: In Progress | Due: 2026-09-12 12:00 | Created: 2026-09-11 09:05
  [#1] Finalize sprint retrospective slides | Priority: High     | Status: Pending     | Due: 2026-09-15 17:00 | Created: 2026-09-11 09:04

Select an option: 12

-- Status Summary --
  Total tasks: 2
  Pending      : 1
  In Progress  : 1
  Completed    : 0
  Cancelled    : 0

Select an option: 0
State saved to tasks.dat.
Goodbye!
```

Re-launching the application afterward reloads both tasks automatically:

```
==================================================
   JAVA-WORKFLOW-MANAGER  --  CLI Edition v1.0
==================================================
Loaded 2 task(s) from tasks.dat
```

---

## Storage Format

Tasks are persisted one per line in `tasks.dat` as pipe-delimited records:

```
id|title|description|priority|status|createdAt|dueDate
```

Pipe characters, backslashes, and newlines inside user-entered text are
escaped on write and restored on read, so free-text titles/descriptions can
never corrupt the record structure. A due date of `-` represents "no due
date".

## License

This project was produced as an academic coursework submission. See the
accompanying Project Report for the integrity declaration.
