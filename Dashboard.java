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

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel redPanel = createColorPanel("Courses: 0", new Color(255, 99, 71));
        JLabel greenPanel = createColorPanel("Resources Due: 0", new Color(50, 205, 50));
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
        JLabel resourcesDueTitle = new JLabel("Resources Due Soon");
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
    }

    private void updateDashboardData() {
        JLabel coursesLabel = (JLabel)((JPanel)((JPanel)pages.get("Dashboard")).getComponent(0)).getComponent(0);
        coursesLabel.setText("Courses: " + courseListModel.getSize());

        int totalResources = courseResourcesMap.values().stream().mapToInt(DefaultListModel::getSize).sum();
        JLabel resourcesLabel = (JLabel)((JPanel)((JPanel)pages.get("Dashboard")).getComponent(0)).getComponent(1);
        resourcesLabel.setText("Resources Due: " + totalResources);

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
