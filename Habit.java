import java.io.Serializable;

public class Habit implements Serializable {
    private String name;
    private String frequency;  // Daily / Weekly
    private int totalDays;
    private int completedDays;
    private String reminderTime; // "HH:mm" or empty

    public Habit(String name, String frequency, int totalDays, String reminderTime) {
        this.name = name;
        this.frequency = frequency;
        this.totalDays = totalDays;
        this.completedDays = 0;
        this.reminderTime = (reminderTime == null) ? "" : reminderTime;
    }

    public String getName() { return name; }
    public String getFrequency() { return frequency; }
    public int getTotalDays() { return totalDays; }
    public int getCompletedDays() { return completedDays; }
    public String getReminderTime() { return reminderTime; }

    public void setReminderTime(String rt) { this.reminderTime = rt; }

    public void markComplete() {
        if (completedDays < totalDays) completedDays++;
    }

    public double getProgress() {
        if (totalDays == 0) return 0;
        return (completedDays * 100.0) / totalDays;
    }

    @Override
    public String toString() {
        return name + " (" + frequency + ") - " + completedDays + "/" + totalDays +
               " done (" + String.format("%.1f", getProgress()) + "%)  [" + reminderTime + "]";
    }

    public String toFileString() {
        // escape commas are not handled - keep habit names simple (no commas)
        return name + "," + frequency + "," + totalDays + "," + completedDays + "," + reminderTime;
    }

    public static Habit fromFileString(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length >= 5) {
            try {
                Habit h = new Habit(parts[0], parts[1], Integer.parseInt(parts[2]), parts[4]);
                h.completedDays = Integer.parseInt(parts[3]);
                return h;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}