import java.io.*;
import java.util.*;

class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    String category;
    String dueDate;
    String priority;
    boolean isComplete;

    public Task(String name, String category, String dueDate, String priority) {
        this.name = name;
        this.category = category;
        this.dueDate = dueDate;
        this.priority = priority;
        this.isComplete = false;
    }

    public void markComplete() {
        isComplete = true;
    }

    public void markIncomplete() {
        isComplete = false;
    }

    @Override
    public String toString() {
        return "Task{name='" + name + '\'' + ", category='" + category + '\'' + ", dueDate='" + dueDate + '\'' + ", priority='" + priority + '\'' + ", isComplete=" + isComplete + '}';
    }
}

public class ToDoListApp {
    public Map<String, List<Task>> taskMap = new HashMap<>(); // Category -> List of Tasks
    private final String DATA_FILE = "tasks.dat";

    public ToDoListApp() {
        loadTasks();
    }

    public void addTask(String name, String category, String dueDate, String priority) {
        Task task = new Task(name, category, dueDate, priority);
        taskMap.computeIfAbsent(category, k -> new ArrayList<>()).add(task);
        saveTasks();
    }

    public void editTask(String oldName, String newName, String newCategory, String newDueDate, String newPriority) {
        Task task = findTask(oldName);
        if (task != null) {
            task.name = newName;
            task.category = newCategory;
            task.dueDate = newDueDate;
            task.priority = newPriority;
            saveTasks();
        } else {
            System.out.println("Task not found!");
        }
    }

    public void markTaskComplete(String name) {
        Task task = findTask(name);
        if (task != null) {
            task.markComplete();
            saveTasks();
        } else {
            System.out.println("Task not found!");
        }
    }
    public void markTaskIncomplete(String name) {
        Task task = findTask(name);
        if (task != null) {
            task.markIncomplete();
            saveTasks();
            System.out.println("Task marked as incomplete!");
        } else {
            System.out.println("Task not found!");
        }
    }
    public void deleteTask(String name) {
        for (List<Task> tasks : taskMap.values()) {
            if (tasks.removeIf(task -> task.name.equals(name))) {
                saveTasks();
                return;
            }
        }
    }
    public int getMonthFromDueDate(String dueDate) {
        try {
            return Integer.parseInt(dueDate.split("-")[1]);
        } catch (Exception e) {
            System.out.println("Invalid due date format for task: " + dueDate);
            return -1; // Return -1 for invalid dates
        }
    }



    private Task findTask(String name) {
        for (List<Task> tasks : taskMap.values()) {
            for (Task task : tasks) {
                if (task.name.equals(name)) {
                    return task;
                }
            }
        }
        return null;
    }


    public void saveTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(taskMap);
        } catch (IOException e) {
            System.out.println("Error saving tasks: " + e.getMessage());
        }
    }
    public void clearAllTasks() {
        taskMap.clear();
        saveTasks();
        System.out.println("All tasks have been deleted!");
    }
    @SuppressWarnings("unchecked")
    public void loadTasks() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            taskMap = (Map<String, List<Task>>) ois.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("No saved tasks found. Starting fresh.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        ToDoListApp app = new ToDoListApp();
        app.clearAllTasks();

    }
}
