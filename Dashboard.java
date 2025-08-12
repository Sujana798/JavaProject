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

    private Map<String, DefaultListModel<Resource>> courseResourcesMap = new HashMap<>();

    private JTextArea dashboardMyCoursesArea;
    private JTextArea dashboardResourcesDueArea;

    // File names for this user
    private final String coursesFile;
    private final String resourcesFile;

    public Dashboard(String username) {
        this.username = username;
        coursesFile = "courses_" + username + ".txt";
        resourcesFile = "resources_" + username + ".txt";

        setTitle("Study Resource Management Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());



        mainContentPanel = new JPanel(new BorderLayout());

        loadCoursesAndResources();

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

    // --- Persistence methods ---

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

    // --- rest of the UI code remains similar, with small updates to call saveCourses() and saveResources() after changes ---

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

    private void showPage(String name) {
        mainContentPanel.removeAll();
        mainContentPanel.add(pages.get(name), BorderLayout.CENTER);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();

        if (name.equals("Dashboard")) {
            updateDashboardData();
        }
    }

    /*private JPanel createDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel redPanel = createColorPanel("Courses: 0", new Color(255, 99, 71));
        JLabel greenPanel = createColorPanel("Resources : 0", new Color(50, 205, 50));
        JLabel bluePanel = createColorPanel("Last Reviewed: None", new Color(70, 130, 180));

        statsPanel.add(redPanel);
        statsPanel.add(greenPanel);
        statsPanel.add(bluePanel);

        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        lowerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        dashboardMyCoursesArea = new JTextArea();
        dashboardMyCoursesArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        dashboardMyCoursesArea.setEditable(false);
        JPanel myCoursesPanel = new JPanel(new BorderLayout());
        JLabel myCoursesTitle = new JLabel("My Courses");
        myCoursesTitle.setFont(new Font("Segoe UI", Font.BOLD, 28)); // Larger font
        myCoursesTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        myCoursesPanel.add(myCoursesTitle, BorderLayout.NORTH);
        myCoursesPanel.add(new JScrollPane(dashboardMyCoursesArea), BorderLayout.CENTER);

        dashboardResourcesDueArea = new JTextArea();
        dashboardResourcesDueArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        dashboardResourcesDueArea.setEditable(false);
        JPanel resourcesDuePanel = new JPanel(new BorderLayout());
        JLabel resourcesDueTitle = new JLabel("Resources ");
        resourcesDueTitle.setFont(new Font("Segoe UI", Font.BOLD, 28)); // Larger font
        resourcesDueTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        resourcesDuePanel.add(resourcesDueTitle, BorderLayout.NORTH);
        resourcesDuePanel.add(new JScrollPane(dashboardResourcesDueArea), BorderLayout.CENTER);

        lowerPanel.add(myCoursesPanel);
        lowerPanel.add(resourcesDuePanel);

        panel.add(statsPanel, BorderLayout.NORTH);
        panel.add(lowerPanel, BorderLayout.CENTER);

        return panel;
    }

   private JLabel createColorPanel(String text, Color bgColor) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 20));
        label.setBorder(BorderFactory.createEmptyBorder(40, 10, 40, 10));
        return label;
    }*/

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
        JPanel coursesCard = createStatCard("📚", "Total Courses", "0", new Color(76, 175, 80), 75);

        // Resources Stats Card
        JPanel resourcesCard = createStatCard("📄", "Study Resources", "0", new Color(33, 150, 243), 60);

        // Assessments Stats Card
        JPanel assessmentsCard = createStatCard("📝", "Due This Week", "0", new Color(255, 152, 0), 40);

        // Progress Stats Card
        JPanel progressCard = createStatCard("⭐", "Completion Rate", "87%", new Color(156, 39, 176), 87);

        statsPanel.add(coursesCard);
        statsPanel.add(resourcesCard);
        statsPanel.add(assessmentsCard);
        statsPanel.add(progressCard);

        return statsPanel;
    }

    private JPanel createStatCard(String icon, String label, String value, Color accentColor, int progressValue) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Add rounded corners effect
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(15, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Header with icon and value
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(50, 50));

        JLabel valueLabel = new JLabel(value, SwingConstants.RIGHT);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(new Color(51, 51, 51));

        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(valueLabel, BorderLayout.CENTER);

        // Label
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelText.setForeground(new Color(102, 102, 102));

        // Progress bar
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBackground(Color.WHITE);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(progressValue);
        progressBar.setStringPainted(false);
        progressBar.setBackground(new Color(240, 240, 240));
        progressBar.setForeground(accentColor);
        progressBar.setPreferredSize(new Dimension(0, 8));
        progressBar.setBorderPainted(false);

        progressPanel.add(progressBar, BorderLayout.CENTER);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(labelText, BorderLayout.CENTER);
        card.add(progressPanel, BorderLayout.SOUTH);

        // Add hover effect
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
        JButton addAssessmentBtn = createQuickActionButton("📝 Add Assessment");
        JButton viewCalendarBtn = createQuickActionButton("📅 View Calendar");

        // Add action listeners
        addCourseBtn.addActionListener(e -> showPage("Courses"));
        addResourceBtn.addActionListener(e -> showPage("Courses"));
        addAssessmentBtn.addActionListener(e -> showPage("Assessments"));
        viewCalendarBtn.addActionListener(e -> showPage("Calendar"));

        buttonsPanel.add(addCourseBtn);
        buttonsPanel.add(addResourceBtn);
        buttonsPanel.add(addAssessmentBtn);
        buttonsPanel.add(viewCalendarBtn);

        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(buttonsPanel, BorderLayout.CENTER);

        quickActions.add(contentPanel);
        return quickActions;
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

        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 25));
        contentPanel.add(content, BorderLayout.CENTER);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);

        return card;
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

        // Sample upcoming deadlines
        panel.add(createDeadlineItem("Machine Learning Assignment", "Due in 2 days", "📝", true));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createDeadlineItem("Database Quiz", "Due in 3 days", "📊", true));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createDeadlineItem("Software Engineering Project", "Due in 1 week", "💻", false));

        return panel;
    }

    private JPanel createDeadlineItem(String title, String dueDate, String icon, boolean urgent) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(urgent ? new Color(255, 235, 238) : new Color(241, 248, 233));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(urgent ? new Color(244, 67, 54) : new Color(76, 175, 80), 0, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

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

        JLabel dueDateLabel = new JLabel(dueDate);
        dueDateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dueDateLabel.setForeground(new Color(102, 102, 102));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        metaPanel.add(dueDateLabel, BorderLayout.WEST);
        metaPanel.add(iconLabel, BorderLayout.EAST);

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

        // Sample activities
        panel.add(createActivityItem("Added new resource: Python Tutorial", "2 hours ago", "+", new Color(76, 175, 80)));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createActivityItem("Completed: Data Structures Quiz", "Yesterday", "✓", new Color(156, 39, 176)));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createActivityItem("Updated course: Machine Learning", "2 days ago", "✏", new Color(33, 150, 243)));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createActivityItem("Added new course: Web Development", "3 days ago", "+", new Color(76, 175, 80)));

        return panel;
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
        // Update stats cards
        updateStatCard(0, "Total Courses", String.valueOf(courseListModel.getSize()));

        int totalResources = courseResourcesMap.values().stream().mapToInt(DefaultListModel::getSize).sum();
        updateStatCard(1, "Study Resources", String.valueOf(totalResources));

        // Count due assessments (you can implement this based on your assessment data)
        updateStatCard(2, "Due This Week", "3");

        // Update courses text area
        StringBuilder coursesText = new StringBuilder();
        for (int i = 0; i < courseListModel.size(); i++) {
            coursesText.append("• ").append(courseListModel.get(i)).append("\n");
        }
        dashboardMyCoursesArea.setText(coursesText.toString());

        // Update resources text area
        StringBuilder resourcesText = new StringBuilder();
        for (String course : courseResourcesMap.keySet()) {
            DefaultListModel<Resource> resources = courseResourcesMap.get(course);
            for (int i = 0; i < resources.size(); i++) {
                resourcesText.append("• ").append(resources.get(i).toString()).append("\n");
            }
        }
        dashboardResourcesDueArea.setText(resourcesText.toString());
    }

    private void updateStatCard(int cardIndex, String label, String value) {
        // This method would update the stat cards with real data
        // Implementation depends on how you store references to the stat cards
        // You might want to store them in instance variables for easy access
    }


    /*private void updateDashboardData() {
        JLabel coursesLabel = (JLabel)((JPanel)((JPanel)pages.get("Dashboard")).getComponent(0)).getComponent(0);
        coursesLabel.setText("Courses: " + courseListModel.getSize());

        int totalResources = courseResourcesMap.values().stream().mapToInt(DefaultListModel::getSize).sum();
        JLabel resourcesLabel = (JLabel)((JPanel)((JPanel)pages.get("Dashboard")).getComponent(0)).getComponent(1);
        resourcesLabel.setText("Resources : " + totalResources);

        JLabel lastReviewLabel = (JLabel)((JPanel)((JPanel)pages.get("Dashboard")).getComponent(0)).getComponent(2);
        lastReviewLabel.setText("Last Reviewed: Today");

        StringBuilder coursesText = new StringBuilder();
        for (int i = 0; i < courseListModel.size(); i++) {
            coursesText.append("- ").append(courseListModel.get(i)).append("\n");
        }
        dashboardMyCoursesArea.setText(coursesText.toString());

        StringBuilder resourcesText = new StringBuilder();
        for (String course : courseResourcesMap.keySet()) {
            DefaultListModel<Resource> resources = courseResourcesMap.get(course);
            for (int i = 0; i < resources.size(); i++) {
                resourcesText.append("- ").append(resources.get(i).toString()).append("\n");
            }
        }
        dashboardResourcesDueArea.setText(resourcesText.toString());
    }*/



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


    private void saveAssessmentsToFile(List<String[]> assessments) {
        try (PrintWriter writer = new PrintWriter("assessments.txt")) {
            for (String[] data : assessments) {
                writer.println(data[0] + "|" + data[1] + "|" + data[2] + "|" + data[3]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String[]> loadAssessmentsFromFile() {
        List<String[]> assessments = new ArrayList<>();
        File file = new File("assessments.txt");
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String[] parts = scanner.nextLine().split("\\|");
                    if (parts.length == 4) {
                        assessments.add(parts);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return assessments;
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


    private void saveEvents(Map<LocalDate, List<String[]>> events) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data/events.txt"))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            for (Map.Entry<LocalDate, List<String[]>> entry : events.entrySet()) {
                for (String[] evt : entry.getValue()) {
                    writer.write(formatter.format(entry.getKey()) + "," + evt[0] + "," + evt[1]);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<LocalDate, List<String[]>> loadEvents() {
        Map<LocalDate, List<String[]>> events = new HashMap<>();
        File file = new File("data/events.txt");
        if (!file.exists()) return events;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    LocalDate date = LocalDate.parse(parts[0], formatter);
                    String title = parts[1];
                    String type = parts[2];
                    events.computeIfAbsent(date, k -> new ArrayList<>()).add(new String[]{title, type});
                }
            }
        } catch (IOException | DateTimeParseException e) {
            e.printStackTrace();
        }
        return events;
    }


    // --- Placeholder panels for other pages ---
    private JPanel createAssessmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Assessments", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        panel.add(title, BorderLayout.NORTH);

        DefaultListModel<String> assessmentModel = new DefaultListModel<>();
        JList<String> assessmentList = new JList<>(assessmentModel);
        assessmentList.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JScrollPane scrollPane = new JScrollPane(assessmentList);

        // Load assessments from file on startup
        List<String[]> assessmentDataList = loadAssessmentsFromFile();
        for (String[] a : assessmentDataList) {
            assessmentModel.addElement("\u2022 " + a[0] + " | " + a[1] + " | Due: " + a[2] + " | Status: " + a[3]);
        }

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        JLabel label1 = new JLabel("Assessment Title:");
        label1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JTextField nameField = new JTextField();
        nameField.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel label2 = new JLabel("Related Course:");
        label2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JTextField courseField = new JTextField();
        courseField.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel label3 = new JLabel("Due Date:");
        label3.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JTextField dueDateField = new JTextField();
        dueDateField.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        String[] statuses = {"Pending", "In Progress", "Completed"};
        JComboBox<String> statusBox = new JComboBox<>(statuses);
        statusBox.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JButton addBtn = new JButton("Add Assessment");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        addBtn.setBackground(new Color(100, 149, 237));
        addBtn.setForeground(Color.WHITE);

        addBtn.addActionListener(e -> {
            String titleText = nameField.getText().trim();
            String courseText = courseField.getText().trim();
            String dueText = dueDateField.getText().trim();
            String statusText = (String) statusBox.getSelectedItem();

            if (!titleText.isEmpty() && !courseText.isEmpty() && !dueText.isEmpty()) {
                String entry = "\u2022 " + titleText + " | " + courseText + " | Due: " + dueText + " | Status: " + statusText;
                assessmentModel.addElement(entry);

                // Save new assessment to list and file
                assessmentDataList.add(new String[]{titleText, courseText, dueText, statusText});
                saveAssessmentsToFile(assessmentDataList);

                nameField.setText("");
                courseField.setText("");
                dueDateField.setText("");
            }
        });

        formPanel.add(label1);
        formPanel.add(nameField);
        formPanel.add(label2);
        formPanel.add(courseField);
        formPanel.add(label3);
        formPanel.add(dueDateField);
        formPanel.add(statusLabel);
        formPanel.add(statusBox);
        formPanel.add(new JLabel());
        formPanel.add(addBtn);

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(monthLabel, BorderLayout.NORTH);

        JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 5, 5));
        calendarGrid.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(calendarGrid, BorderLayout.CENTER);

        Map<LocalDate, List<String[]>> events = loadEvents();

        updateCalendar(calendarGrid, monthLabel, events);

        JButton addEventBtn = new JButton("Add Event");
        addEventBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        addEventBtn.addActionListener(e -> {
            JTextField titleField = new JTextField();
            JTextField dateField = new JTextField("dd-MM-yyyy");
            String[] types = {"Quiz", "Mid", "Lab", "Exam"};
            JComboBox<String> typeCombo = new JComboBox<>(types);

            JPanel inputPanel = new JPanel(new GridLayout(0, 1));
            inputPanel.add(new JLabel("Title:"));
            inputPanel.add(titleField);
            inputPanel.add(new JLabel("Date (dd-MM-yyyy):"));
            inputPanel.add(dateField);
            inputPanel.add(new JLabel("Type:"));
            inputPanel.add(typeCombo);

            int result = JOptionPane.showConfirmDialog(panel, inputPanel, "Add Event", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    LocalDate date = LocalDate.parse(dateField.getText(), formatter);
                    String title = titleField.getText();
                    String type = typeCombo.getSelectedItem().toString();

                    events.computeIfAbsent(date, k -> new ArrayList<>()).add(new String[]{title, type});
                    saveEvents(events);
                    updateCalendar(calendarGrid, monthLabel, events);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "Invalid date format. Please use dd-MM-yyyy");
                }
            }
        });
        panel.add(addEventBtn, BorderLayout.SOUTH);

        return panel;
    }



    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Reports Section");
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

}
