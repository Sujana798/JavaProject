package dashboard;

import auth.Login;

import java.io.File;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.io.PrintWriter;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.border.Border;

public class Dashboard extends JFrame {
    private final String username;
    private JPanel mainContentPanel;
    private Map<String, JPanel> pages = new HashMap<>();

    private DefaultListModel<String> courseListModel = new DefaultListModel<>();

    // Dashboard stat card references for dynamic updates
    private JLabel totalCoursesValue;
    private JLabel studyResourcesValue;
    private JLabel dueThisWeekValue;
    private JProgressBar totalCoursesProgress;
    private JProgressBar studyResourcesProgress;
    private JProgressBar dueThisWeekProgress;
    JProgressBar progressBar = new JProgressBar(0, 100);


    // Shared events map for calendar and assessments
    private Map<LocalDate, List<String[]>> events = new HashMap<>();

    // Assessment data storage
    private List<String[]> assessmentDataList = new ArrayList<>();

    // Components for Assessments panel
    private DefaultListModel<String> assessmentListModel;
    private JList<String> assessmentList;

    // Components for dashboard stats update
    private JLabel progressCardValueLabel;   // New JLabel for completion rate card -> link to progress card in stats panel
    private JProgressBar progressCardProgressBar; // New JProgressBar for completion card

    // For calendar navigation
    private JLabel calendarMonthLabel;
    private JPanel calendarGridPanel;
    private YearMonth calendarCurrentMonth;


    // Dynamic content areas
    private JPanel deadlinesContentPanel;
    private JPanel activityContentPanel;
    private DefaultListModel<String> deadlinesList = new DefaultListModel<>();
    private DefaultListModel<String> activityList = new DefaultListModel<>();


    private static class Resource {
        String name;
        String type;
        String pathOrUrl;

        Resource(String name, String type, String pathOrUrl) {
            this.name = name;
            this.type = type;
            this.pathOrUrl = pathOrUrl;
        }

        @Override
        public String toString() {
            return name + " (" + type + ")";
        }
    }

    // Deadline class for better management
    private static class Deadline {
        String title;
        String dueDate;
        String type;
        boolean urgent;

        Deadline(String title, String dueDate, String type, boolean urgent) {
            this.title = title;
            this.dueDate = dueDate;
            this.type = type;
            this.urgent = urgent;
        }

        @Override
        public String toString() {
            return title + "|" + dueDate + "|" + type + "|" + urgent;
        }

        public static Deadline fromString(String str) {
            String[] parts = str.split("\\|");
            if (parts.length == 4) {
                return new Deadline(parts[0], parts[1], parts[2], Boolean.parseBoolean(parts[3]));
            }
            return null;
        }
    }

    // Activity class for better management
    private static class Activity {
        String description;
        String time;
        String icon;
        String color;

        Activity(String description, String time, String icon, String color) {
            this.description = description;
            this.time = time;
            this.icon = icon;
            this.color = color;
        }

        @Override
        public String toString() {
            return description + "|" + time + "|" + icon + "|" + color;
        }

        public static Activity fromString(String str) {
            String[] parts = str.split("\\|");
            if (parts.length == 4) {
                return new Activity(parts[0], parts[1], parts[2], parts[3]);
            }
            return null;
        }
    }

    private Map<String, DefaultListModel<Resource>> courseResourcesMap = new HashMap<>();
    private List<Deadline> deadlines = new ArrayList<>();
    private List<Activity> activities = new ArrayList<>();

    private JTextArea dashboardMyCoursesArea;
    private JTextArea dashboardResourcesDueArea;

    // File names for this user
    private final String coursesFile;
    private final String resourcesFile;
    private final String deadlinesFile;
    private final String activitiesFile;

    public Dashboard(String username) {
        this.username = username;
        coursesFile = "courses_" + username + ".txt";
        resourcesFile = "resources_" + username + ".txt";
        deadlinesFile = "deadlines_" + username + ".txt";
        activitiesFile = "activities_" + username + ".txt";

        setTitle("Study Resource Management Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        mainContentPanel = new JPanel(new BorderLayout());

        loadCoursesAndResources();
        loadDeadlines();
        loadActivities();

        pages.put("Dashboard", createDashboardPanel());
        pages.put("Courses", createCoursesPanel());
        pages.put("Assessments", createAssessmentsPanel());
        pages.put("Classes", createClassesPanel());
        pages.put("Students", createStudentsPanel());
        pages.put("Calendar", createCalendarPanel());
        pages.put("Reports", createReportsPanel());

        add(createSidebar(), BorderLayout.WEST);
        add(createTopPanel(), BorderLayout.NORTH);
        add(mainContentPanel, BorderLayout.CENTER);

        showPage("Dashboard");
        this.revalidate();
        this.repaint();

        setVisible(true);
    }

    // --- Enhanced Persistence methods ---

    private void loadCoursesAndResources() {
        courseListModel.clear();
        courseResourcesMap.clear();

        // Load courses
        File cFile = new File(coursesFile);
        if (cFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(cFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        courseListModel.addElement(line.trim());
                        courseResourcesMap.put(line.trim(), new DefaultListModel<>());
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading courses: " + e.getMessage());
            }
        }

        // Load resources
        File rFile = new File(resourcesFile);
        if (rFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(rFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        // Format: courseName|resourceName|resourceType|resourcePathOrUrl
                        String[] parts = line.split("\\|");
                        if (parts.length == 4) {
                            String courseName = parts[0];
                            String resourceName = parts[1];
                            String resourceType = parts[2];
                            String resourcePath = parts[3];

                            Resource resource = new Resource(resourceName, resourceType, resourcePath);
                            courseResourcesMap.computeIfAbsent(courseName, k -> new DefaultListModel<>()).addElement(resource);
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading resources: " + e.getMessage());
            }
        }
    }

    private void loadDeadlines() {
        deadlines.clear();
        File file = new File(deadlinesFile);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        Deadline deadline = Deadline.fromString(line.trim());
                        if (deadline != null) {
                            deadlines.add(deadline);
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading deadlines: " + e.getMessage());
            }
        }
    }

    private void loadActivities() {
        activities.clear();
        File file = new File(activitiesFile);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        Activity activity = Activity.fromString(line.trim());
                        if (activity != null) {
                            activities.add(activity);
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading activities: " + e.getMessage());
            }
        }
    }

    private void saveCourses() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(coursesFile))) {
            for (int i = 0; i < courseListModel.size(); i++) {
                bw.write(courseListModel.get(i));
                bw.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving courses: " + e.getMessage());
        }
    }

    private void saveResources() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(resourcesFile))) {
            for (String course : courseResourcesMap.keySet()) {
                DefaultListModel<Resource> resources = courseResourcesMap.get(course);
                for (int i = 0; i < resources.size(); i++) {
                    Resource r = resources.get(i);
                    bw.write(course + "|" + r.name + "|" + r.type + "|" + r.pathOrUrl);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving resources: " + e.getMessage());
        }
    }

    private void saveDeadlines() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(deadlinesFile))) {
            for (Deadline deadline : deadlines) {
                bw.write(deadline.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving deadlines: " + e.getMessage());
        }
    }

    private void saveActivities() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(activitiesFile))) {
            for (Activity activity : activities) {
                bw.write(activity.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving activities: " + e.getMessage());
        }
    }

    // --- UI Creation methods remain the same until createStatsPanel ---

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(280, getHeight()));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(45, 45, 45));

        String[] buttons = {"Dashboard", "Courses", "Assessments", "Classes", "Students", "Calendar", "Reports", "Logout"};
        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(250, 50));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btn.setBackground(new Color(60, 63, 65));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> {
                if (text.equals("Logout")) {
                    saveCourses();
                    saveResources();
                    saveDeadlines();
                    saveActivities();
                    dispose();
                    new Login();
                } else {
                    showPage(text);
                }
            });

            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
            sidebar.add(btn);
        }

        return sidebar;
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 240, 240));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel welcomeLabel = new JLabel("Welcome back, " + username + "!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        topPanel.add(welcomeLabel, BorderLayout.CENTER);

        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(300, 40));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        topPanel.add(searchField, BorderLayout.EAST);

        return topPanel;
    }

    private JLabel createColorPanel(String text, Color bgColor) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 20));
        label.setBorder(BorderFactory.createEmptyBorder(40, 10, 40, 10));
        return label;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        // Create main scroll pane for the entire dashboard
        JScrollPane mainScrollPane = new JScrollPane();
        mainScrollPane.setBorder(null);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(new Color(245, 247, 250));
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header Section
        JPanel headerPanel = createHeaderPanel();
        mainContent.add(headerPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 25)));

        // Stats Cards Section
        JPanel statsPanel = createStatsPanel();
        mainContent.add(statsPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 25)));

        // Quick Actions Section
        JPanel quickActionsPanel = createQuickActionsPanel();
        mainContent.add(quickActionsPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 25)));

        // Content Grid Section
        JPanel contentGrid = createContentGrid();
        mainContent.add(contentGrid);
        mainContent.add(Box.createRigidArea(new Dimension(0, 25)));

        // Study Streak Section
        JPanel studyStreakPanel = createStudyStreakPanel();
        mainContent.add(studyStreakPanel);

        mainScrollPane.setViewportView(mainContent);
        panel.add(mainScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(245, 247, 250));
        header.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel titleLabel = new JLabel("Study Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(new Color(51, 51, 51));

        JLabel subtitleLabel = new JLabel("Welcome back! Here's your learning progress overview", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(102, 102, 102));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(new Color(245, 247, 250));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        titlePanel.add(subtitleLabel);

        header.add(titlePanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel createStatsPanel() {

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(new Color(245, 247, 250));

        // Course Stats Card
        JPanel coursesCard = createStatCard("📚", "Total Courses", "0", new Color(76, 175, 80), 0);

        // Resources Stats Card
        JPanel resourcesCard = createStatCard("📄", "Study Resources", "0", new Color(33, 150, 243), 0);

        // Assessments Stats Card
        JPanel assessmentsCard = createStatCard("📝", "Due This Week", "0", new Color(255, 152, 0), 0);

        // Progress Stats Card - Completion Rate
        JPanel progressCard = createStatCard("⭐", "Completion Rate", "0%", new Color(156, 39, 176), 0); // Initial 0%

        statsPanel.add(coursesCard);
        statsPanel.add(resourcesCard);
        statsPanel.add(assessmentsCard);
        statsPanel.add(progressCard);

        return statsPanel;
    }

    private JPanel createStatCard(String icon, String label, String value, Color accentColor, int progressValue) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(15, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(50, 50));

        JLabel valueLabel = new JLabel(value, SwingConstants.RIGHT);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(new Color(51, 51, 51));

        // Create a NEW progress bar for EACH card
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(progressValue);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 8));
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(240, 240, 240));
        progressBar.setForeground(accentColor);

        // Save references accordingly
        if (label.equals("Total Courses")) {
            totalCoursesProgress = progressBar;
            totalCoursesValue = valueLabel;
        } else if (label.equals("Study Resources")) {
            studyResourcesProgress = progressBar;
            studyResourcesValue = valueLabel;
        } else if (label.equals("Due This Week")) {
            dueThisWeekProgress = progressBar;
            dueThisWeekValue = valueLabel;
        } else if (label.equals("Completion Rate")) {
            progressCardProgressBar = progressBar;
            progressCardValueLabel = valueLabel;
        }

        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(valueLabel, BorderLayout.CENTER);

        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelText.setForeground(new Color(102, 102, 102));

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBackground(Color.WHITE);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        progressPanel.add(progressBar, BorderLayout.CENTER); // Always add the current progressBar

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(labelText, BorderLayout.CENTER);
        card.add(progressPanel, BorderLayout.SOUTH);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(248, 249, 250));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
            }
        });

        return card;
    }


    private JPanel createQuickActionsPanel() {
        JPanel quickActions = new JPanel(new BorderLayout());
        quickActions.setBackground(new Color(102, 126, 234));
        quickActions.setBorder(new RoundedBorder(15, new Color(102, 126, 234)));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(102, 126, 234));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel titleLabel = new JLabel("Quick Actions");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        buttonsPanel.setBackground(new Color(102, 126, 234));

        JButton addCourseBtn = createQuickActionButton("➕ Add Course");
        JButton addResourceBtn = createQuickActionButton("📎 Add Resource");
        JButton addDeadlineBtn = createQuickActionButton("📝 Add Deadline");
        JButton addActivityBtn = createQuickActionButton("📈 Add Activity");

        // Add action listeners
        addCourseBtn.addActionListener(e -> showPage("Courses"));
        addResourceBtn.addActionListener(e -> showPage("Courses"));
        addDeadlineBtn.addActionListener(e -> showAddDeadlineDialog());
        addActivityBtn.addActionListener(e -> showAddActivityDialog());

        buttonsPanel.add(addCourseBtn);
        buttonsPanel.add(addResourceBtn);
        buttonsPanel.add(addDeadlineBtn);
        buttonsPanel.add(addActivityBtn);

        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(buttonsPanel, BorderLayout.CENTER);

        quickActions.add(contentPanel);
        return quickActions;
    }

    private void showAddDeadlineDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        JTextField titleField = new JTextField();
        JTextField dueDateField = new JTextField("YYYY-MM-DD");
        String[] types = {"📝 Assignment", "📊 Quiz", "💻 Project", "📋 Exam"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        JCheckBox urgentCheck = new JCheckBox("Urgent");

        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Due Date:"));
        panel.add(dueDateField);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Urgent:"));
        panel.add(urgentCheck);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Deadline", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String dueDate = dueDateField.getText().trim();
            String type = typeCombo.getSelectedItem().toString();
            boolean urgent = urgentCheck.isSelected();

            if (!title.isEmpty() && !dueDate.isEmpty()) {
                deadlines.add(new Deadline(title, dueDate, type, urgent));
                saveDeadlines();
                updateDashboardData();
                refreshDeadlinesPanel();
            }
        }
    }

    private void showAddActivityDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        JTextField descField = new JTextField();
        JTextField timeField = new JTextField("Just now");
        String[] icons = {"+", "✓", "✏", "📚", "🎯", "💡"};
        JComboBox<String> iconCombo = new JComboBox<>(icons);
        String[] colors = {"76,175,80", "156,39,176", "33,150,243", "255,152,0", "244,67,54", "96,125,139"};
        JComboBox<String> colorCombo = new JComboBox<>(new String[]{"Green", "Purple", "Blue", "Orange", "Red", "Gray"});

        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Time:"));
        panel.add(timeField);
        panel.add(new JLabel("Icon:"));
        panel.add(iconCombo);
        panel.add(new JLabel("Color:"));
        panel.add(colorCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Activity", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String description = descField.getText().trim();
            String time = timeField.getText().trim();
            String icon = iconCombo.getSelectedItem().toString();
            String color = colors[colorCombo.getSelectedIndex()];

            if (!description.isEmpty()) {
                activities.add(new Activity(description, time, icon, color));
                saveActivities();
                refreshActivityPanel();
            }
        }
    }

    private JButton createQuickActionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(255, 255, 255, 40));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 2),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(255, 255, 255, 60));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(255, 255, 255, 40));
            }
        });

        return button;
    }

    private JPanel createContentGrid() {
        JPanel contentGrid = new JPanel(new GridLayout(2, 2, 20, 20));
        contentGrid.setBackground(new Color(245, 247, 250));

        // My Courses Panel
        JPanel coursesPanel = createSectionCard("📚 My Courses", createCoursesListPanel());

        // Upcoming Deadlines Panel
        JPanel deadlinesPanel = createSectionCard("🎯 Upcoming Deadlines", createDeadlinesPanel());

        // Recent Activity Panel
        JPanel activityPanel = createSectionCard("📈 Recent Activity", createActivityPanel());

        // Recent Resources Panel
        JPanel resourcesPanel = createSectionCard("📚 Recent Resources", createResourcesListPanel());

        contentGrid.add(coursesPanel);
        contentGrid.add(deadlinesPanel);
        contentGrid.add(activityPanel);
        contentGrid.add(resourcesPanel);

        return contentGrid;
    }

    private JPanel createSectionCard(String title, JPanel content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new RoundedBorder(15, new Color(230, 230, 230)));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(51, 51, 51));

        // Add management buttons for deadlines and activities
        if (title.contains("Deadlines")) {
            JButton manageBtn = new JButton("Manage");
            manageBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            manageBtn.addActionListener(e -> showManageDeadlinesDialog());
            headerPanel.add(manageBtn, BorderLayout.EAST);
        } else if (title.contains("Activity")) {
            JButton manageBtn = new JButton("Manage");
            manageBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            manageBtn.addActionListener(e -> showManageActivitiesDialog());
            headerPanel.add(manageBtn, BorderLayout.EAST);
        }

        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 25));
        contentPanel.add(content, BorderLayout.CENTER);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);

        return card;
    }

    private void showManageDeadlinesDialog() {
        JDialog dialog = new JDialog(this, "Manage Deadlines", true);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Deadline deadline : deadlines) {
            listModel.addElement(deadline.title + " - Due: " + deadline.dueDate);
        }

        JList<String> list = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex >= 0) {
                deadlines.remove(selectedIndex);
                listModel.remove(selectedIndex);
                saveDeadlines();
                updateDashboardData();
                refreshDeadlinesPanel();
            }
        });

        buttonPanel.add(deleteBtn);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showManageActivitiesDialog() {
        JDialog dialog = new JDialog(this, "Manage Activities", true);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Activity activity : activities) {
            listModel.addElement(activity.description + " - " + activity.time);
        }

        JList<String> list = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex >= 0) {
                activities.remove(selectedIndex);
                listModel.remove(selectedIndex);
                saveActivities();
                refreshActivityPanel();
            }
        });

        buttonPanel.add(deleteBtn);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createCoursesListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        dashboardMyCoursesArea = new JTextArea();
        dashboardMyCoursesArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dashboardMyCoursesArea.setEditable(false);
        dashboardMyCoursesArea.setBackground(Color.WHITE);
        dashboardMyCoursesArea.setBorder(null);

        JScrollPane scrollPane = new JScrollPane(dashboardMyCoursesArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setBackground(Color.WHITE);

        panel.add(scrollPane);
        return panel;
    }

    private JPanel createDeadlinesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        deadlinesContentPanel = new JPanel();
        deadlinesContentPanel.setLayout(new BoxLayout(deadlinesContentPanel, BoxLayout.Y_AXIS));
        deadlinesContentPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(deadlinesContentPanel);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setBackground(Color.WHITE);

        panel.add(scrollPane);
        refreshDeadlinesPanel();
        return panel;
    }

    private void refreshDeadlinesPanel() {
        if (deadlinesContentPanel != null) {
            deadlinesContentPanel.removeAll();

            if (deadlines.isEmpty()) {
                JLabel noDeadlinesLabel = new JLabel("No upcoming deadlines");
                noDeadlinesLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                noDeadlinesLabel.setForeground(new Color(136, 136, 136));
                deadlinesContentPanel.add(noDeadlinesLabel);
            } else {
                for (int i = 0; i < Math.min(deadlines.size(), 5); i++) { // Show only first 5
                    Deadline deadline = deadlines.get(i);
                    deadlinesContentPanel.add(createDeadlineItem(deadline.title, deadline.dueDate, deadline.type, deadline.urgent));
                    if (i < Math.min(deadlines.size(), 5) - 1) {
                        deadlinesContentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                    }
                }
            }

            deadlinesContentPanel.revalidate();
            deadlinesContentPanel.repaint();
        }
    }

    private JPanel createDeadlineItem(String title, String dueDate, String type, boolean urgent) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(urgent ? new Color(255, 235, 238) : new Color(241, 248, 233));

        // Set left border
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, urgent ? new Color(244, 67, 54) : new Color(76, 175, 80)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(51, 51, 51));

        JPanel metaPanel = new JPanel(new BorderLayout());
        metaPanel.setBackground(item.getBackground());

        JLabel dueDateLabel = new JLabel("Due: " + dueDate);
        dueDateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dueDateLabel.setForeground(new Color(102, 102, 102));

        JLabel typeLabel = new JLabel(type);
        typeLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        metaPanel.add(dueDateLabel, BorderLayout.WEST);
        metaPanel.add(typeLabel, BorderLayout.EAST);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(item.getBackground());
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(metaPanel);

        item.add(contentPanel, BorderLayout.CENTER);
        return item;
    }

    private JPanel createActivityPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        activityContentPanel = new JPanel();
        activityContentPanel.setLayout(new BoxLayout(activityContentPanel, BoxLayout.Y_AXIS));
        activityContentPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(activityContentPanel);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setBackground(Color.WHITE);

        panel.add(scrollPane);
        refreshActivityPanel();
        return panel;
    }

    private void refreshActivityPanel() {
        if (activityContentPanel != null) {
            activityContentPanel.removeAll();

            if (activities.isEmpty()) {
                JLabel noActivityLabel = new JLabel("No recent activities");
                noActivityLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                noActivityLabel.setForeground(new Color(136, 136, 136));
                activityContentPanel.add(noActivityLabel);
            } else {
                for (int i = 0; i < Math.min(activities.size(), 6); i++) { // Show only first 6
                    Activity activity = activities.get(i);
                    String[] colorParts = activity.color.split(",");
                    Color iconColor = new Color(
                            Integer.parseInt(colorParts[0]),
                            Integer.parseInt(colorParts[1]),
                            Integer.parseInt(colorParts[2])
                    );
                    activityContentPanel.add(createActivityItem(activity.description, activity.time, activity.icon, iconColor));
                    if (i < Math.min(activities.size(), 6) - 1) {
                        activityContentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                    }
                }
            }

            activityContentPanel.revalidate();
            activityContentPanel.repaint();
        }
    }

    private JPanel createActivityItem(String title, String time, String icon, Color iconColor) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // Icon panel
        JPanel iconPanel = new JPanel();
        iconPanel.setBackground(iconColor);
        iconPanel.setPreferredSize(new Dimension(35, 35));
        iconPanel.setLayout(new BorderLayout());
        iconPanel.setBorder(new RoundedBorder(17, iconColor));

        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        iconLabel.setForeground(Color.WHITE);
        iconPanel.add(iconLabel, BorderLayout.CENTER);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(51, 51, 51));

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(new Color(136, 136, 136));

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        contentPanel.add(timeLabel);

        item.add(iconPanel, BorderLayout.WEST);
        item.add(contentPanel, BorderLayout.CENTER);

        return item;
    }

    private JPanel createResourcesListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        dashboardResourcesDueArea = new JTextArea();
        dashboardResourcesDueArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dashboardResourcesDueArea.setEditable(false);
        dashboardResourcesDueArea.setBackground(Color.WHITE);
        dashboardResourcesDueArea.setBorder(null);

        JScrollPane scrollPane = new JScrollPane(dashboardResourcesDueArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setBackground(Color.WHITE);

        panel.add(scrollPane);
        return panel;
    }

    private JPanel createStudyStreakPanel() {
        JPanel streakPanel = new JPanel(new BorderLayout());
        streakPanel.setBackground(new Color(255, 107, 107));
        streakPanel.setBorder(new RoundedBorder(15, new Color(255, 107, 107)));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(255, 107, 107));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel streakNumber = new JLabel("🔥 7", SwingConstants.CENTER);
        streakNumber.setFont(new Font("Segoe UI", Font.BOLD, 48));
        streakNumber.setForeground(Color.WHITE);

        JLabel streakText = new JLabel("Day Study Streak! Keep it up!", SwingConstants.CENTER);
        streakText.setFont(new Font("Segoe UI", Font.BOLD, 16));
        streakText.setForeground(Color.WHITE);

        contentPanel.add(streakNumber);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(streakText);

        streakPanel.add(contentPanel, BorderLayout.CENTER);
        return streakPanel;
    }

    private void updateDashboardData() {
        // Update courses count
        int courseCount = courseListModel.getSize();
        if (totalCoursesValue != null) {
            totalCoursesValue.setText(String.valueOf(courseCount));
            totalCoursesProgress.setValue(Math.min(courseCount * 20, 100)); // Max 5 courses for 100%
        }

        // Update resources count
        int totalResources = courseResourcesMap.values().stream().mapToInt(DefaultListModel::getSize).sum();
        if (studyResourcesValue != null) {
            studyResourcesValue.setText(String.valueOf(totalResources));
            studyResourcesProgress.setValue(Math.min(totalResources * 10, 100)); // Max 10 resources for 100%
        }

        // Calculate deadlines due this week
        int weeklyDeadlines = calculateWeeklyDeadlines();
        if (dueThisWeekValue != null) {
            dueThisWeekValue.setText(String.valueOf(weeklyDeadlines));
            dueThisWeekProgress.setValue(Math.min(weeklyDeadlines * 25, 100)); // Max 4 deadlines for 100%
        }

        // Update courses text area
        StringBuilder coursesText = new StringBuilder();
        if (courseListModel.isEmpty()) {
            coursesText.append("No courses added yet. Click 'Add Course' to get started!");
        } else {
            for (int i = 0; i < courseListModel.size(); i++) {
                coursesText.append("• ").append(courseListModel.get(i)).append("\n");
            }
        }
        dashboardMyCoursesArea.setText(coursesText.toString());

        // Update resources text area
        StringBuilder resourcesText = new StringBuilder();
        if (totalResources == 0) {
            resourcesText.append("No resources added yet. Add courses and resources to see them here!");
        } else {
            int count = 0;
            for (String course : courseResourcesMap.keySet()) {
                DefaultListModel<Resource> resources = courseResourcesMap.get(course);
                for (int i = 0; i < resources.size() && count < 10; i++, count++) { // Show max 10 resources
                    resourcesText.append("• ").append(resources.get(i).toString())
                            .append(" (").append(course).append(")").append("\n");
                }
                if (count >= 10) break;
            }
        }
        dashboardResourcesDueArea.setText(resourcesText.toString());

        // Refresh dynamic panels
        refreshDeadlinesPanel();
        refreshActivityPanel();
    }

    private int calculateWeeklyDeadlines() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        int weeklyCount = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Deadline deadline : deadlines) {
            try {
                LocalDate dueDate = LocalDate.parse(deadline.dueDate, formatter);
                if (!dueDate.isBefore(today) && !dueDate.isAfter(weekEnd)) {
                    weeklyCount++;
                }
            } catch (DateTimeParseException e) {
                // Skip invalid dates
            }
        }

        return weeklyCount;
    }

    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10,10));

        JLabel label = new JLabel("Courses");
        label.setFont(new Font("Segoe UI", Font.BOLD, 28));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.NORTH);

        DefaultListModel<String> coursesModel = courseListModel;
        JList<String> coursesList = new JList<>(coursesModel);
        coursesList.setFont(new Font("Segoe UI", Font.BOLD, 20));

        coursesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Segoe UI", Font.BOLD, 20));
                return label;
            }
        });

        JScrollPane coursesScroll = new JScrollPane(coursesList);
        coursesScroll.setPreferredSize(new Dimension(320, 0));

        JButton addCourseBtn = new JButton("Add Course");
        JButton editCourseBtn = new JButton("Edit Course");
        JButton deleteCourseBtn = new JButton("Delete Course");

        Dimension bigButtonSize = new Dimension(160, 45);
        Font buttonFont = new Font("Segoe UI", Font.BOLD, 18);

        for (JButton b : new JButton[]{addCourseBtn, editCourseBtn, deleteCourseBtn}) {
            b.setPreferredSize(bigButtonSize);
            b.setFont(buttonFont);
        }

        JPanel courseButtonsPanel = new JPanel();
        courseButtonsPanel.add(addCourseBtn);
        courseButtonsPanel.add(editCourseBtn);
        courseButtonsPanel.add(deleteCourseBtn);

        JPanel leftPanel = new JPanel(new BorderLayout(5,5));
        leftPanel.add(coursesScroll, BorderLayout.CENTER);
        leftPanel.add(courseButtonsPanel, BorderLayout.SOUTH);

        panel.add(leftPanel, BorderLayout.WEST);

        DefaultListModel<Resource> resourcesModel = new DefaultListModel<>();
        JList<Resource> resourcesList = new JList<>(resourcesModel);
        resourcesList.setFont(new Font("Segoe UI", Font.BOLD, 20));

        resourcesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Segoe UI", Font.BOLD, 20));
                return label;
            }
        });

        JScrollPane resourcesScroll = new JScrollPane(resourcesList);

        JButton addResourceBtn = new JButton("Add Resource");
        JButton deleteResourceBtn = new JButton("Delete Resource");

        for (JButton b : new JButton[]{addResourceBtn, deleteResourceBtn}) {
            b.setPreferredSize(bigButtonSize);
            b.setFont(buttonFont);
        }

        JPanel resourceButtonsPanel = new JPanel();
        resourceButtonsPanel.add(addResourceBtn);
        resourceButtonsPanel.add(deleteResourceBtn);

        JPanel rightPanel = new JPanel(new BorderLayout(5,5));
        JLabel resourcesLabel = new JLabel("Resources");
        resourcesLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        rightPanel.add(resourcesLabel, BorderLayout.NORTH);
        rightPanel.add(resourcesScroll, BorderLayout.CENTER);
        rightPanel.add(resourceButtonsPanel, BorderLayout.SOUTH);

        panel.add(rightPanel, BorderLayout.CENTER);

        coursesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedCourse = coursesList.getSelectedValue();
                resourcesModel.clear();
                if (selectedCourse != null && courseResourcesMap.containsKey(selectedCourse)) {
                    DefaultListModel<Resource> resList = courseResourcesMap.get(selectedCourse);
                    for (int i = 0; i < resList.size(); i++) {
                        resourcesModel.addElement(resList.get(i));
                    }
                }
            }
        });

        addCourseBtn.addActionListener(e -> {
            String newCourse = JOptionPane.showInputDialog(this, "Enter Course Name:");
            if (newCourse != null && !newCourse.trim().isEmpty()) {
                if (courseListModel.contains(newCourse.trim())) {
                    JOptionPane.showMessageDialog(this, "Course already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                courseListModel.addElement(newCourse.trim());
                courseResourcesMap.put(newCourse.trim(), new DefaultListModel<>());
                saveCourses();
                saveResources();
                updateDashboardData();

                // Add activity for course addition
                activities.add(0, new Activity("Added new course: " + newCourse.trim(), "Just now", "+", "76,175,80"));
                saveActivities();
            }
        });

        editCourseBtn.addActionListener(e -> {
            String selectedCourse = coursesList.getSelectedValue();
            if (selectedCourse == null) {
                JOptionPane.showMessageDialog(this, "Select a course to edit!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String newName = JOptionPane.showInputDialog(this, "Edit Course Name:", selectedCourse);
            if (newName != null && !newName.trim().isEmpty()) {
                if (courseListModel.contains(newName.trim()) && !newName.trim().equals(selectedCourse)) {
                    JOptionPane.showMessageDialog(this, "Course name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int idx = courseListModel.indexOf(selectedCourse);
                courseListModel.set(idx, newName.trim());

                DefaultListModel<Resource> res = courseResourcesMap.remove(selectedCourse);
                courseResourcesMap.put(newName.trim(), res);

                saveCourses();
                saveResources();
                updateDashboardData();

                // Add activity for course edit
                activities.add(0, new Activity("Updated course: " + newName.trim(), "Just now", "✏", "33,150,243"));
                saveActivities();
            }
        });

        deleteCourseBtn.addActionListener(e -> {
            String selectedCourse = coursesList.getSelectedValue();
            if (selectedCourse == null) {
                JOptionPane.showMessageDialog(this, "Select a course to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete course: " + selectedCourse + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                courseListModel.removeElement(selectedCourse);
                courseResourcesMap.remove(selectedCourse);
                resourcesModel.clear();
                saveCourses();
                saveResources();
                updateDashboardData();

                // Add activity for course deletion
                activities.add(0, new Activity("Deleted course: " + selectedCourse, "Just now", "✓", "244,67,54"));
                saveActivities();
            }
        });

        addResourceBtn.addActionListener(e -> {
            String selectedCourse = coursesList.getSelectedValue();
            if (selectedCourse == null) {
                JOptionPane.showMessageDialog(this, "Select a course to add resource!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JPanel resourcePanel = new JPanel(new GridLayout(3, 2, 5, 5));
            JTextField resourceNameField = new JTextField();
            String[] resourceTypes = {"Link", "PDF", "Slides", "Pictures"};
            JComboBox<String> typeCombo = new JComboBox<>(resourceTypes);
            JTextField pathField = new JTextField();

            resourcePanel.add(new JLabel("Resource Name:"));
            resourcePanel.add(resourceNameField);
            resourcePanel.add(new JLabel("Resource Type:"));
            resourcePanel.add(typeCombo);
            resourcePanel.add(new JLabel("Path or URL:"));
            resourcePanel.add(pathField);

            int result = JOptionPane.showConfirmDialog(this, resourcePanel, "Add Resource", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                String rName = resourceNameField.getText().trim();
                String rType = (String) typeCombo.getSelectedItem();
                String rPath = pathField.getText().trim();

                if (rName.isEmpty() || rPath.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name and Path/URL cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Resource newResource = new Resource(rName, rType, rPath);
                DefaultListModel<Resource> resources = courseResourcesMap.get(selectedCourse);
                resources.addElement(newResource);
                resourcesModel.addElement(newResource);

                saveResources();
                updateDashboardData();

                // Add activity for resource addition
                activities.add(0, new Activity("Added new resource: " + rName, "Just now", "+", "76,175,80"));
                saveActivities();
            }
        });

        deleteResourceBtn.addActionListener(e -> {
            Resource selectedResource = resourcesList.getSelectedValue();
            String selectedCourse = coursesList.getSelectedValue();
            if (selectedResource == null) {
                JOptionPane.showMessageDialog(this, "Select a resource to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete resource: " + selectedResource.name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                DefaultListModel<Resource> resources = courseResourcesMap.get(selectedCourse);
                resources.removeElement(selectedResource);
                resourcesModel.removeElement(selectedResource);
                saveResources();
                updateDashboardData();

                // Add activity for resource deletion
                activities.add(0, new Activity("Deleted resource: " + selectedResource.name, "Just now", "✓", "244,67,54"));
                saveActivities();
            }
        });

        // Double click on resource to open link or file
        resourcesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    Resource selectedResource = resourcesList.getSelectedValue();
                    if (selectedResource != null) {
                        openResource(selectedResource);
                    }
                }
            }
        });

        return panel;
    }

    private void openResource(Resource resource) {
        try {
            Desktop desktop = Desktop.getDesktop();
            String pathOrUrl = resource.pathOrUrl;

            if (resource.type.equalsIgnoreCase("Link")) {
                // Open link in default browser
                desktop.browse(new URI(pathOrUrl));
            } else {
                // Try to open local file or URL for other types
                File file = new File(pathOrUrl);
                if (file.exists()) {
                    desktop.open(file);
                } else {
                    // Try as URL
                    desktop.browse(new URI(pathOrUrl));
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to open resource: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCalendar(JPanel calendarGrid, JLabel monthLabel, Map<LocalDate, List<String[]>> events) {
        calendarGrid.removeAll();

        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.of(now.getYear(), now.getMonth());

        monthLabel.setText(currentMonth.getMonth().name() + " " + currentMonth.getYear());

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String dayName : days) {
            JLabel dayLabel = new JLabel(dayName, SwingConstants.CENTER);
            dayLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            // Make Friday and Saturday red
            if (dayName.equals("Fri") || dayName.equals("Sat")) {
                dayLabel.setForeground(Color.RED);
            } else {
                dayLabel.setForeground(Color.DARK_GRAY);
            }
            calendarGrid.add(dayLabel);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        // Adjust startDay so Sunday=0, Monday=1 ... Saturday=6
        int startDay = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentMonth.lengthOfMonth();

        // Add empty labels for days before the first day
        for (int i = 0; i < startDay; i++) {
            calendarGrid.add(new JLabel(""));
        }

        for (int dayCounter = 1; dayCounter <= daysInMonth; dayCounter++) {
            LocalDate date = firstOfMonth.plusDays(dayCounter - 1);
            JButton dayButton = new JButton(String.valueOf(dayCounter));
            dayButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
            dayButton.setFocusPainted(false);

            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
                dayButton.setForeground(Color.RED);
            } else {
                dayButton.setForeground(Color.BLACK);
            }

            if (events.containsKey(date)) {
                for (String[] evt : events.get(date)) {
                    switch (evt[1].toLowerCase()) {
                        case "quiz": dayButton.setBackground(new Color(255, 223, 186)); break;
                        case "mid": dayButton.setBackground(new Color(186, 255, 201)); break;
                        case "lab": dayButton.setBackground(new Color(186, 225, 255)); break;
                        case "exam": dayButton.setBackground(new Color(255, 186, 186)); break;
                    }
                    dayButton.setToolTipText(evt[0] + " (" + evt[1] + ")");
                }
            }
            calendarGrid.add(dayButton);
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    // --- Placeholder panels for other pages ---
    private JPanel createAssessmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Assessments", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        panel.add(title, BorderLayout.NORTH);

        assessmentListModel = new DefaultListModel<>();
        assessmentList = new JList<>(assessmentListModel);
        assessmentList.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JScrollPane scrollPane = new JScrollPane(assessmentList);

        // Load assessments from file
        assessmentDataList = loadAssessmentsFromFile();
        refreshAssessmentStatuses(); // update statuses and list model

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));

        JButton addBtn = new JButton("Add Assessment");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        addBtn.setBackground(new Color(40, 167, 69));
        addBtn.setForeground(Color.WHITE);

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        deleteBtn.setBackground(new Color(220, 53, 69));
        deleteBtn.setForeground(Color.WHITE);

        buttonsPanel.add(addBtn);
        buttonsPanel.add(deleteBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add Assessment action
        addBtn.addActionListener(e -> {
            JTextField titleField = new JTextField();
            JTextField courseField = new JTextField();
            JTextField dueDateField = new JTextField("yyyy-MM-dd");

            Object[] form = {
                    "Assessment Title:", titleField,
                    "Related Course:", courseField,
                    "Due Date (yyyy-MM-dd):", dueDateField
            };

            int result = JOptionPane.showConfirmDialog(panel, form, "Add New Assessment", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String t = titleField.getText().trim();
                String c = courseField.getText().trim();
                String d = dueDateField.getText().trim();

                if (t.isEmpty() || c.isEmpty() || d.isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDate dueDate;
                try {
                    dueDate = LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(panel, "Invalid date format! Use yyyy-MM-dd.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String status = "Pending"; // default, will be refreshed
                String[] newAssessment = new String[] {t, c, d, status};
                assessmentDataList.add(newAssessment);

                saveAssessmentsToFile(assessmentDataList);

                // Sync with calendar events
                events.computeIfAbsent(dueDate, k -> new ArrayList<>()).add(new String[]{t, "Assessment"});
                saveEvents(events);

                refreshAssessmentStatuses();
                updateDashboardCompletionRate();
                updateCalendar(calendarGridPanel, calendarMonthLabel, events);
            }
        });

        // Delete Assessment action
        deleteBtn.addActionListener(e -> {
            int idx = assessmentList.getSelectedIndex();
            if (idx >= 0 && idx < assessmentDataList.size()) {
                String[] removed = assessmentDataList.remove(idx);

                // Remove event from calendar
                try {
                    LocalDate dueDate = LocalDate.parse(removed[2], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    List<String[]> dayEvents = events.get(dueDate);
                    if (dayEvents != null) {
                        dayEvents.removeIf(ev -> ev[0].equals(removed[0]) && "Assessment".equalsIgnoreCase(ev[1]));
                        if (dayEvents.isEmpty()) {
                            events.remove(dueDate);
                        }
                        saveEvents(events);
                    }
                } catch (DateTimeParseException ignored) { }

                saveAssessmentsToFile(assessmentDataList);
                refreshAssessmentStatuses();
                updateDashboardCompletionRate();
                updateCalendar(calendarGridPanel, calendarMonthLabel, events);
            } else {
                JOptionPane.showMessageDialog(panel, "Select an assessment to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });

        return panel;
    }

    private void refreshAssessmentStatuses() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        assessmentListModel.clear();

        int dueThisWeekCount = 0;

        for (int i = 0; i < assessmentDataList.size(); i++) {
            String[] a = assessmentDataList.get(i);
            try {
                LocalDate dueDate = LocalDate.parse(a[2], formatter);

                // Update status based on due date
                if (dueDate.isBefore(today)) {
                    a[3] = "Completed";
                } else if (dueDate.isEqual(today)) {
                    a[3] = "In Progress";
                    notifyAssessmentDue(a);
                } else {
                    a[3] = "Pending";
                    if (dueDate.equals(today.plusDays(1))) {
                        notifyAssessmentDueSoon(a);
                    }
                }

                // Count for Due This Week Stat card
                if (!dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(7))) {
                    dueThisWeekCount++;
                }

                assessmentListModel.addElement("• " + a[0] + " | " + a[1] + " | Due: " + a[2] + " | Status: " + a[3]);

            } catch (DateTimeParseException ignored) {
                assessmentListModel.addElement("• " + a[0] + " | " + a[1] + " | Due: " + a[2] + " | Status: " + a[3]);
            }
        }

        // Update due this week stat card
        if (dueThisWeekValue != null && dueThisWeekProgress != null) {
            dueThisWeekValue.setText(String.valueOf(dueThisWeekCount));
            dueThisWeekProgress.setValue(Math.min(dueThisWeekCount * 25, 100)); // assuming max 4 events = 100%
        }
    }

    private void notifyAssessmentDue(String[] assessment) {
        // Popup reminder for due today
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                "Assessment Due Today: " + assessment[0],
                "Reminder", JOptionPane.INFORMATION_MESSAGE));
    }

    private void notifyAssessmentDueSoon(String[] assessment) {
        // You can expand this into tray notification or dashboard notification later
        System.out.println("Reminder: Assessment due tomorrow: " + assessment[0]);
    }

    private List<String[]> loadAssessmentsFromFile() {
        List<String[]> list = new ArrayList<>();
        File file = new File("data/assessments.txt");
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String l;
            while ((l = reader.readLine()) != null) {
                String[] parts = l.split("\\|");
                if (parts.length == 4) {
                    list.add(parts);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void saveAssessmentsToFile(List<String[]> list) {
        try {
            File dataDir = new File("data");
            if (!dataDir.exists()) dataDir.mkdirs();

            try (PrintWriter writer = new PrintWriter(new FileWriter("data/assessments.txt"))) {
                for (String[] a : list) {
                    writer.println(String.join("|", a));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JPanel createClassesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Classes Section");
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Students Section");
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBackground(Color.WHITE);

        calendarMonthLabel = new JLabel("", SwingConstants.CENTER);
        calendarMonthLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(calendarMonthLabel, BorderLayout.NORTH);

        calendarGridPanel = new JPanel(new GridLayout(0,7,5,5));
        calendarGridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(calendarGridPanel, BorderLayout.CENTER);

        // Load events once
        events = loadEvents();

        // Start month at January 2025
        calendarCurrentMonth = YearMonth.of(2025, 1);
        updateCalendar(calendarGridPanel, calendarMonthLabel, events, calendarCurrentMonth);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");

        prevBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nextBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));

        navPanel.add(prevBtn);
        navPanel.add(nextBtn);

        panel.add(navPanel, BorderLayout.SOUTH);

        prevBtn.addActionListener(e -> {
            calendarCurrentMonth = calendarCurrentMonth.minusMonths(1);
            if (calendarCurrentMonth.getYear() != 2025) {
                calendarCurrentMonth = calendarCurrentMonth.plusMonths(1); // keep in 2025
                return;
            }
            updateCalendar(calendarGridPanel, calendarMonthLabel, events, calendarCurrentMonth);
        });

        nextBtn.addActionListener(e -> {
            calendarCurrentMonth = calendarCurrentMonth.plusMonths(1);
            if (calendarCurrentMonth.getYear() != 2025) {
                calendarCurrentMonth = calendarCurrentMonth.minusMonths(1); // keep in 2025
                return;
            }
            updateCalendar(calendarGridPanel, calendarMonthLabel, events, calendarCurrentMonth);
        });

        return panel;
    }

    private void updateCalendar(JPanel calendarGrid, JLabel monthLabel, Map<LocalDate, List<String[]>> events, YearMonth month) {
        calendarGrid.removeAll();

        monthLabel.setText(month.getMonth().name() + " " + month.getYear());

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String dayName : days) {
            JLabel dayLabel = new JLabel(dayName, SwingConstants.CENTER);
            dayLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            if (dayName.equals("Fri") || dayName.equals("Sat")) {
                dayLabel.setForeground(Color.RED);
            } else {
                dayLabel.setForeground(Color.DARK_GRAY);
            }
            calendarGrid.add(dayLabel);
        }

        LocalDate firstOfMonth = month.atDay(1);
        int startDay = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday=0

        int daysInMonth = month.lengthOfMonth();

        for (int i = 0; i < startDay; i++) {
            calendarGrid.add(new JLabel(""));
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = month.atDay(day);
            JButton dayBtn = new JButton(String.valueOf(day));
            dayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            dayBtn.setMargin(new Insets(0,0,0,0));
            dayBtn.setFocusPainted(false);

            // Highlight Friday and Saturday red
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) {
                dayBtn.setForeground(Color.RED);
            }

            // Highlight days with events
            if (events.containsKey(date)) {
                dayBtn.setBackground(new Color(135, 206, 235)); // light blue
                List<String[]> evts = events.get(date);
                String tooltip = "";
                for (String[] ev : evts) {
                    tooltip += ev[0] + " (" + ev[1] + ")\n";
                }
                dayBtn.setToolTipText("<html>" + tooltip.replace("\n", "<br>") + "</html>");
            } else {
                dayBtn.setBackground(Color.WHITE);
            }

            // On click, show events for day
            dayBtn.addActionListener(e -> {
                List<String[]> evts = events.get(date);
                if (evts == null || evts.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No events on " + date.toString());
                } else {
                    String msg = "";
                    for (String[] ev : evts) {
                        msg += ev[1] + ": " + ev[0] + "\n";
                    }
                    JOptionPane.showMessageDialog(this, msg, "Events on " + date.toString(), JOptionPane.INFORMATION_MESSAGE);
                }
            });

            calendarGrid.add(dayBtn);
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private Map<LocalDate, List<String[]>> loadEvents() {
        Map<LocalDate, List<String[]>> map = new HashMap<>();
        File file = new File("data/events.txt");
        if (!file.exists()) return map;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    LocalDate date = LocalDate.parse(parts[0], formatter);
                    String title = parts[1];
                    String type = parts[2];
                    map.computeIfAbsent(date, k -> new ArrayList<>()).add(new String[]{title, type});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private void saveEvents(Map<LocalDate, List<String[]>> events) {
        try {
            File dataDir = new File("data");
            if (!dataDir.exists()) dataDir.mkdirs();

            try (PrintWriter writer = new PrintWriter(new FileWriter("data/events.txt"))) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (Map.Entry<LocalDate, List<String[]>> entry : events.entrySet()) {
                    String dateStr = formatter.format(entry.getKey());
                    for (String[] ev : entry.getValue()) {
                        writer.println(dateStr + "|" + ev[0] + "|" + ev[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Reports Section");
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void showPage(String name) {
        mainContentPanel.removeAll();
        mainContentPanel.add(pages.get(name), BorderLayout.CENTER);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();

        if (name.equals("Dashboard")) {
            updateDashboardData();
            updateDashboardCompletionRate();
        } else if (name.equals("Assessments")) {
            refreshAssessmentStatuses();
            updateDashboardCompletionRate();
        }
    }


    private void updateDashboardCompletionRate() {
        if (assessmentDataList == null || assessmentDataList.isEmpty()) {
            if (progressCardValueLabel != null) progressCardValueLabel.setText("0%");
            if (progressCardProgressBar != null) progressCardProgressBar.setValue(0);
            return;
        }

        long completedCount = assessmentDataList.stream().filter(a -> "Completed".equalsIgnoreCase(a[3])).count();
        int percent = (int) ((completedCount * 100) / assessmentDataList.size());

        if (progressCardValueLabel != null)
            progressCardValueLabel.setText(percent + "%");
        if (progressCardProgressBar != null)
            progressCardProgressBar.setValue(percent);
    }


    // Custom rounded border class
    private static class RoundedBorder implements Border {
        private int radius;
        private Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2d.dispose();
        }
    }
}