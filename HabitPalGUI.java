import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

public class HabitPalGUI extends JFrame {
    private HabitManager manager;

    public HabitPalGUI() {
        manager = new HabitManager();
        setTitle("HabitPal");
        setSize(600, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Check profile
        String[] profile = manager.loadUserProfile();
        if (profile == null) {
            ProfileDialog pd = new ProfileDialog(this);
            pd.setVisible(true);
        }

        initUI();
    }

    private void initUI() {
        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("HabitPal — Build good habits", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        top.add(title, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 3, 10, 10));
        JButton addBtn = new JButton("Add Habit");
        JButton viewBtn = new JButton("View Habits");
        JButton exportBtn = new JButton("Export CSV");
        JButton refreshBtn = new JButton("Refresh Reminders");
        JButton profileBtn = new JButton("Profile");
        JButton exitBtn = new JButton("Save & Exit");

        buttons.add(addBtn); buttons.add(viewBtn); buttons.add(exportBtn);
        buttons.add(refreshBtn); buttons.add(profileBtn); buttons.add(exitBtn);

        add(top, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);

        // actions
        addBtn.addActionListener(e -> {
            AddHabitDialog ad = new AddHabitDialog(this);
            ad.setVisible(true);
        });

        viewBtn.addActionListener(e -> {
            ViewHabitsDialog vd = new ViewHabitsDialog(this);
            vd.setVisible(true);
        });

        exportBtn.addActionListener(e -> {
            manager.exportCSV("habit_report.csv");
        });

        refreshBtn.addActionListener(e -> {
            manager.scheduleAllReminders();
            JOptionPane.showMessageDialog(this, "Reminders refreshed.");
        });

        profileBtn.addActionListener(e -> {
            ProfileDialog pd = new ProfileDialog(this);
            pd.setVisible(true);
        });

        exitBtn.addActionListener(e -> {
            manager.saveHabits();
            JOptionPane.showMessageDialog(this, "Saved. Bye!");
            System.exit(0);
        });
    }

    // Profile dialog
    class ProfileDialog extends JDialog {
        JTextField nameF, emailF;
        JComboBox<String> genderC;
        public ProfileDialog(JFrame parent) {
            super(parent, "User Profile", true);
            setLayout(new GridLayout(4,2,8,8));
            add(new JLabel("Name:"));
            nameF = new JTextField();
            add(nameF);
            add(new JLabel("Email:"));
            emailF = new JTextField();
            add(emailF);
            add(new JLabel("Gender:"));
            genderC = new JComboBox<>(new String[]{"Male","Female","Other"});
            add(genderC);
            JButton save = new JButton("Save");
            add(new JLabel());
            add(save);
            setSize(350,180);
            setLocationRelativeTo(parent);

            // if already exists load
            String[] prof = manager.loadUserProfile();
            if (prof != null) {
                nameF.setText(prof[0]);
                emailF.setText(prof[1]);
                genderC.setSelectedItem(prof[2]);
            }

            save.addActionListener(e -> {
                String n = nameF.getText().trim();
                String em = emailF.getText().trim();
                String g = (String)genderC.getSelectedItem();
                if (n.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name required.");
                    return;
                }
                manager.saveUserProfile(n, em, g);
                JOptionPane.showMessageDialog(this, "Profile saved.");
                dispose();
            });
        }
    }

    // Add habit dialog
    class AddHabitDialog extends JDialog {
        public AddHabitDialog(JFrame parent) {
            super(parent, "Add Habit", true);
            setLayout(new GridLayout(6,2,6,6));
            add(new JLabel("Habit name:"));
            JTextField nameF = new JTextField();
            add(nameF);
            add(new JLabel("Frequency:"));
            JComboBox<String> freqC = new JComboBox<>(new String[]{"Daily","Weekly"});
            add(freqC);
            add(new JLabel("Total days to track:"));
            JSpinner daysS = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
            add(daysS);
            add(new JLabel("Reminder time (H:mm) e.g., 20:00 (optional):"));
            JTextField timeF = new JTextField();
            add(timeF);
            JButton addBtn = new JButton("Add");
            add(new JLabel());
            add(addBtn);
            setSize(480,260);
            setLocationRelativeTo(parent);

            addBtn.addActionListener(e -> {
                String nm = nameF.getText().trim();
                String freq = (String)freqC.getSelectedItem();
                int days = (Integer)daysS.getValue();
                String rt = timeF.getText().trim();
                if (nm.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter name"); return; }
                Habit h = new Habit(nm, freq, days, rt);
                manager.addHabit(h);
                JOptionPane.showMessageDialog(this, "Habit added.");
                dispose();
            });
        }
    }

    // View habits dialog
    class ViewHabitsDialog extends JDialog {
        DefaultTableModel model;
        JTable table;
        public ViewHabitsDialog(JFrame parent) {
            super(parent, "View Habits", true);
            String[] cols = {"#", "Name", "Freq", "Progress", "Reminder"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };
            table = new JTable(model);
            refreshTable();
            JScrollPane sp = new JScrollPane(table);
            add(sp, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            JButton markBtn = new JButton("Mark Selected Done");
            JButton delBtn = new JButton("Delete Selected");
            JButton closeBtn = new JButton("Close");
            bottom.add(markBtn); bottom.add(delBtn); bottom.add(closeBtn);
            add(bottom, BorderLayout.SOUTH);

            setSize(560, 320);
            setLocationRelativeTo(parent);

            markBtn.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r == -1) { JOptionPane.showMessageDialog(this, "Select a row"); return; }
                manager.markHabitCompleteByIndex(r);
                refreshTable();
                JOptionPane.showMessageDialog(this, "Marked done.");
            });
            delBtn.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r == -1) { JOptionPane.showMessageDialog(this, "Select a row"); return; }
                manager.deleteHabit(r);
                refreshTable();
            });
            closeBtn.addActionListener(e -> dispose());
        }

        private void refreshTable() {
            model.setRowCount(0);
            java.util.List<Habit> list = manager.getHabits();
            for (int i = 0; i < list.size(); i++) {
                Habit h = list.get(i);
                model.addRow(new Object[]{i, h.getName(), h.getFrequency(),
                        String.format("%.1f%% (%d/%d)", h.getProgress(), h.getCompletedDays(), h.getTotalDays()),
                        h.getReminderTime()});
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HabitPalGUI gui = new HabitPalGUI();
            gui.setVisible(true);
        });
    }
}