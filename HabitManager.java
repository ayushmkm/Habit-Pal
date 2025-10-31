import javax.swing.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class HabitManager {
    private ArrayList<Habit> habits = new ArrayList<>();
    private final String FILE_NAME = "habits.txt";
    private final String USER_FILE = "user.txt";
    // Keep timers to cancel/reschedule if needed
    private Map<Habit, java.util.Timer> timers = new HashMap<>();

    public HabitManager() {
        loadHabits();
        scheduleAllReminders();
    }

    // CRUD
    public void addHabit(Habit h) {
        habits.add(h);
        saveHabits();
        scheduleReminder(h);
    }

    public List<Habit> getHabits() { return habits; }

    public void deleteHabit(int idx) {
        if (idx >= 0 && idx < habits.size()) {
            Habit h = habits.remove(idx);
            java.util.Timer t = timers.remove(h);
            if (t != null) t.cancel();
            saveHabits();
        }
    }

    public void markHabitCompleteByIndex(int idx) {
        if (idx >= 0 && idx < habits.size()) {
            habits.get(idx).markComplete();
            saveHabits();
        }
    }

    public void markHabitComplete(Habit h) {
        h.markComplete();
        saveHabits();
    }

    // Persistence
    public void saveHabits() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Habit h : habits) {
                bw.write(h.toFileString());
                bw.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving habits: " + e.getMessage());
        }
    }

    public void loadHabits() {
        habits.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                Habit h = Habit.fromFileString(line);
                if (h != null) habits.add(h);
            }
        } catch (IOException ignored) { }
    }

    // Simple CSV export
    public void exportCSV(String outFile) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            bw.write("Name,Frequency,TotalDays,CompletedDays,Progress,ReminderTime");
            bw.newLine();
            for (Habit h : habits) {
                bw.write(h.getName() + "," + h.getFrequency() + "," + h.getTotalDays() + "," +
                         h.getCompletedDays() + "," + String.format("%.1f", h.getProgress()) + "," +
                         h.getReminderTime());
                bw.newLine();
            }
            JOptionPane.showMessageDialog(null, "Exported to " + outFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Export error: " + e.getMessage());
        }
    }

    // Reminders
    private long millisUntilNext(String hhmm) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm");
            LocalTime target = LocalTime.parse(hhmm, fmt);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = LocalDateTime.of(now.toLocalDate(), target);
            if (next.isBefore(now) || next.equals(now)) next = next.plusDays(1);
            return Duration.between(now, next).toMillis();
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    public void scheduleAllReminders() {
        // cancel existing timers
        for (java.util.Timer t : timers.values()) t.cancel();
        timers.clear();
        for (Habit h : habits) {
            scheduleReminder(h);
        }
    }

    public void scheduleReminder(Habit h) {
        String rt = h.getReminderTime();
        if (rt == null || rt.trim().isEmpty()) return;
        long delay = millisUntilNext(rt);
        if (delay < 0) return;

        java.util.Timer timer = new java.util.Timer(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // show dialog on EDT
                javax.swing.SwingUtilities.invokeLater(() -> showReminderDialog(h));
                // schedule next occurrence in 24 hours after executing
                java.util.Timer taskTimer = new java.util.Timer(true);
                taskTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        javax.swing.SwingUtilities.invokeLater(() -> showReminderDialog(h));
                    }
                }, 24L * 60 * 60 * 1000); // next day
            }
        };
        timer.schedule(task, delay);
        timers.put(h, timer);
    }

    private void showReminderDialog(Habit h) {
        String msg = "Time for habit:\n" + h.getName() + "\n[" + h.getFrequency() + "]\n\nChoose:";
        String[] options = {"Mark Done", "Remind 10 min", "Skip"};
        int choice = JOptionPane.showOptionDialog(null, msg, "Habit Reminder",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) { // Mark Done
            h.markComplete();
            saveHabits();
            JOptionPane.showMessageDialog(null, "Marked '" + h.getName() + "' as completed!");
        } else if (choice == 1) { // Remind 10 min
            java.util.Timer t = new java.util.Timer(true);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    javax.swing.SwingUtilities.invokeLater(() -> showReminderDialog(h));
                }
            }, 10L * 60 * 1000); // 10 minutes
        } else {
            // skip: nothing to do
        }
    }

    // Simple profile storage
    public void saveUserProfile(String name, String email, String gender) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
            bw.write(name + "," + email + "," + gender);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving profile: " + e.getMessage());
        }
    }

    public String[] loadUserProfile() {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line = br.readLine();
            if (line != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) return new String[]{parts[0], parts[1], parts[2]};
            }
        } catch (IOException ignored) {}
        return null;
    }
}