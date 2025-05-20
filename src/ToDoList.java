import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.Timer;
import java.time.LocalDate;

public class ToDoList {
    private ToDoListApp app = new ToDoListApp(); // Backend reference
    private JFrame frame;
    private DefaultTableModel incompleteTableModel;
    private DefaultTableModel completedTableModel;

    public ToDoList() {
        initializeUI();
        loadTasksFromFile();
        sortTasks();
        frame.setVisible(true);
    }

    // UI Initialization
    private void initializeUI() {
        setupFrame();
        setupTabs();
    }

    private void setupFrame() {
        frame = new JFrame("To-Do List");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);
    }

    private void setupTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Incomplete Tasks", createIncompleteTasksTab());
        tabbedPane.add("Completed Tasks", createCompletedTasksTab());
        frame.add(tabbedPane);
    }

    private JPanel createIncompleteTasksTab() {
        JPanel panel = new JPanel(new BorderLayout());
        incompleteTableModel = new DefaultTableModel(new String[]{"Name", "Category", "Due Date", "Priority"}, 0);
        JTable table = new JTable(incompleteTableModel);
        configureTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(createIncompleteButtonPanel(table), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCompletedTasksTab() {
        JPanel panel = new JPanel(new BorderLayout());
        completedTableModel = new DefaultTableModel(new String[]{"Name", "Category", "Due Date", "Priority"}, 0);
        JTable table = new JTable(completedTableModel);
        configureTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(createCompletedButtonPanel(table), BorderLayout.SOUTH);
        return panel;
    }

    private void configureTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.setRowHeight(30);
    }

    // Button Panels
    private JPanel createIncompleteButtonPanel(JTable table) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        addButton(panel, "Sort Tasks", e -> sortTasks());
        addButton(panel, "Add Task", e -> addTask());
        addButton(panel, "Delete Task", e -> deleteTask(table.getSelectedRow()));
        addButton(panel, "Edit Task", e -> editTask(table.getSelectedRow(), incompleteTableModel));
        addButton(panel, "Mark Complete", e -> completeTask(table.getSelectedRow()));
        addButton(panel, "Search By Month", e -> searchTasksByMonth(false));
        addButton(panel, "Search By Category", e -> searchTasksByCategory(false));
        addButton(panel, "Show All Tasks", e -> refreshAllTables());

        return panel;
    }

    private JPanel createCompletedButtonPanel(JTable table) {
        JPanel panel = new JPanel();

        addButton(panel, "Edit Task", e -> editTask(table.getSelectedRow(), completedTableModel));
        addButton(panel, "Search By Month", e -> searchTasksByMonth(true));
        addButton(panel, "Search By Category", e -> searchTasksByCategory(true));
        addButton(panel, "Mark Incomplete", e -> markTaskIncomplete(table.getSelectedRow()));
        addButton(panel, "Show All Tasks", e -> refreshAllTables());

        return panel;
    }

    private void addButton(JPanel panel, String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        panel.add(button);
    }

    // Task Management
    private void sortTasks() {
        List<Task> tasks = collectAllTasks();
        tasks.sort(createTaskComparator());
        updateTableModels(tasks);
    }

    private List<Task> collectAllTasks() {
        List<Task> tasks = new ArrayList<>();
        app.taskMap.values().forEach(tasks::addAll);
        return tasks;
    }

    private Comparator<Task> createTaskComparator() {
        return Comparator
                .comparing((Task task) -> parseDueDate(task.dueDate))
                .thenComparing(task -> parsePriority(task.priority));
    }

    private LocalDate parseDueDate(String dueDate) {
        try {
            return LocalDate.parse(dueDate);
        } catch (Exception e) {
            return LocalDate.MAX;
        }
    }

    private int parsePriority(String priority) {
        switch (priority.toLowerCase()) {
            case "high": return 1;
            case "medium": return 2;
            case "low": return 3;
            default: return Integer.MAX_VALUE;
        }
    }

    private void updateTableModels(List<Task> tasks) {
        updateTableModel(incompleteTableModel, tasks, false);
        updateTableModel(completedTableModel, tasks, true);
    }

    private void updateTableModel(DefaultTableModel model, List<Task> tasks, boolean isComplete) {
        model.setRowCount(0);
        for (Task task : tasks) {
            if (task.isComplete == isComplete) {
                model.addRow(new Object[]{task.name, task.category, task.dueDate, task.priority});
            }
        }
    }

    private void addTask() {
        String name = JOptionPane.showInputDialog("Enter Task Name:");
        boolean validDueDate = false;
        String dueDate = null;
        if (name != null) {
            String category = JOptionPane.showInputDialog("Enter Category:");
            while (!validDueDate) {
                dueDate = JOptionPane.showInputDialog("Enter Due Date (YYYY-MM-DD):");
                if (dueDate == null || dueDate.trim().isEmpty()) {
                    showErrorDialog("Due date cannot be empty!", "Warning");
                    return; // Exit if no valid due date is entered
                }
                if (dueDate != null && Pattern.matches("\\d{4}-\\d{2}-\\d{2}", dueDate)) {
                    validDueDate = true;
                } else {
                    showErrorDialog("Invalid date format! Please use YYYY-MM-DD.", "Error");
                }
            }
            String priority = JOptionPane.showInputDialog("Enter Priority (High/Medium/Low):");
            app.addTask(name, category, dueDate, priority);
            saveTasksToFile();
            refreshAllTables();
        }
    }

    private void deleteTask(int selectedRow) {
        if (selectedRow != -1) {
            String name = (String) incompleteTableModel.getValueAt(selectedRow, 0);
            app.deleteTask(name);
            saveTasksToFile();
            refreshAllTables();
        } else {
            showErrorDialog("Please select a task to delete.", "Error");
        }
    }

    private void editTask(int selectedRow, DefaultTableModel model) {
        if (selectedRow != -1) {
            String originalName = (String) model.getValueAt(selectedRow, 0);
            String newName = JOptionPane.showInputDialog("Enter New Task Name:", originalName);
            if (newName != null) {
                String newCategory = JOptionPane.showInputDialog("Enter New Category:", model.getValueAt(selectedRow, 1));
                String newDueDate = JOptionPane.showInputDialog("Enter New Due Date (YYYY-MM-DD):", model.getValueAt(selectedRow, 2));
                String newPriority = JOptionPane.showInputDialog("Enter New Priority (High/Medium/Low):", model.getValueAt(selectedRow, 3));
                app.editTask(originalName, newName, newCategory, newDueDate, newPriority);
                saveTasksToFile();
                refreshAllTables();
            }
        } else {
            showErrorDialog("Please select a task to edit.", "Error");
        }
    }

    private void completeTask(int selectedRow) {
        if (selectedRow != -1) {
            String name = (String) incompleteTableModel.getValueAt(selectedRow, 0);
            app.markTaskComplete(name);
            saveTasksToFile();
            refreshAllTables();
            showBreakTimer(15);
        } else {
            showErrorDialog("Please select a task to mark as complete.", "Error");
        }
    }

    private void markTaskIncomplete(int selectedRow) {
        if (selectedRow != -1) {
            String name = (String) completedTableModel.getValueAt(selectedRow, 0);
            app.markTaskIncomplete(name);
            saveTasksToFile();
            refreshAllTables();
        } else {
            showErrorDialog("Please select a task to mark as incomplete.", "Error");
        }
    }

    private void refreshAllTables() {
        updateIncompleteTable();
        updateCompletedTable();
    }

    private void updateIncompleteTable() {
        List<Task> tasks = collectAllTasks();
        updateTableModel(incompleteTableModel, tasks, false);
    }

    private void updateCompletedTable() {
        List<Task> tasks = collectAllTasks();
        updateTableModel(completedTableModel, tasks, true);
    }

    // Search Functionality
    private void searchTasksByMonth(boolean isComplete) {
        String input = JOptionPane.showInputDialog("Enter Month (1-12):");
        if (input != null) {
            try {
                int month = Integer.parseInt(input);
                if (month < 1 || month > 12) throw new NumberFormatException();

                List<Task> filteredTasks = filterTasksByMonth(month, isComplete);
                if (isComplete) {
                    updateTableModel(completedTableModel, filteredTasks, true);
                } else {
                    updateTableModel(incompleteTableModel, filteredTasks, false);
                }
            } catch (NumberFormatException e) {
                showErrorDialog("Invalid input. Please enter a valid month (1-12).", "Error");
            }
        }
    }

    private List<Task> filterTasksByMonth(int month, boolean isComplete) {
        List<Task> filteredTasks = new ArrayList<>();
        for (List<Task> taskList : app.taskMap.values()) {
            for (Task task : taskList) {
                if (task.isComplete == isComplete && app.getMonthFromDueDate(task.dueDate) == month) {
                    filteredTasks.add(task);
                }
            }
        }
        return filteredTasks;
    }

    private void searchTasksByCategory(boolean isComplete) {
        String category = JOptionPane.showInputDialog("Enter Category to Search:");
        if (category != null && !category.isEmpty()) {
            // Directly retrieve tasks for the specified category using the HashMap
            List<Task> tasksInCategory = app.taskMap.get(category);

            if (tasksInCategory != null) {
                // Filter tasks based on completion status
                List<Task> filteredTasks = new ArrayList<>();
                for (Task task : tasksInCategory) {
                    if (task.isComplete == isComplete) {
                        filteredTasks.add(task);
                    }
                }

                // Update the appropriate table with filtered tasks
                if (isComplete) {
                    updateTableModel(completedTableModel, filteredTasks, true);
                } else {
                    updateTableModel(incompleteTableModel, filteredTasks, false);
                }
            } else {
                showErrorDialog("No tasks found for the specified category.", "Error");
            }
        } else {
            showErrorDialog("Please enter a valid category.", "Error");
        }
    }


    private List<Task> filterTasksByCategory(String category, boolean isComplete) {
        List<Task> filteredTasks = new ArrayList<>();
        for (List<Task> taskList : app.taskMap.values()) {
            for (Task task : taskList) {
                if (task.isComplete == isComplete && category.equalsIgnoreCase(task.category)) {
                    filteredTasks.add(task);
                }
            }
        }
        return filteredTasks;
    }

    // Utility Methods
    private void saveTasksToFile() {
        app.saveTasks();
    }

    private void loadTasksFromFile() {
        app.loadTasks();
        refreshAllTables();
    }

    private void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showBreakTimer(int minutes) {
        JFrame timerFrame = new JFrame("Take a Break!");
        timerFrame.setSize(400, 200);
        timerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel timerLabel = new JLabel("15:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 40));
        timerFrame.add(timerLabel, BorderLayout.CENTER);

        JButton skipButton = new JButton("Skip Break");
        skipButton.addActionListener(e -> timerFrame.dispose());
        timerFrame.add(skipButton, BorderLayout.SOUTH);

        timerFrame.setLocationRelativeTo(null);
        timerFrame.setVisible(true);

        int[] totalSeconds = {minutes * 60};
        Timer timer = new Timer(1000, null);
        timer.addActionListener(e -> {
            if (totalSeconds[0] <= 0) {
                timer.stop();
                timerFrame.dispose();
                JOptionPane.showMessageDialog(frame, "Break time is over! Get ready for your next task.");
            } else {
                totalSeconds[0]--; // Decrement the countdown
                int mins = totalSeconds[0] / 60;
                int secs = totalSeconds[0] % 60;
                timerLabel.setText(String.format("%02d:%02d", mins, secs));
            }
        });
        timer.start();
    }


    public static void main(String[] args) {
        new ToDoList();
    }
}
