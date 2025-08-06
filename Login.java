package auth;

import dashboard.Dashboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class Login extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public Login() {
        setTitle("Login");
        setSize(800, 400); // Split layout size
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // -------- LEFT IMAGE PANEL --------
        JLabel imageLabel = new JLabel();
        ImageIcon imageIcon = new ImageIcon("login.jpg"); // Adjust path if needed
        Image image = imageIcon.getImage().getScaledInstance(400, 400, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(image));

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(imageLabel, BorderLayout.CENTER);

        // -------- RIGHT FORM PANEL --------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("Login");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32)); // Larger title
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(title, gbc);

        gbc.gridwidth = 1;

        // Username Label
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 20)); // Larger bold label
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(userLabel, gbc);

        // Username Field
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 20));  // Larger input font
        usernameField.setPreferredSize(new Dimension(250, 40)); // Taller field
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        // Password Label
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 20)); // Larger bold label
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(passLabel, gbc);

        // Password Field
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 20));  // Larger input font
        passwordField.setPreferredSize(new Dimension(250, 40)); // Taller field
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        // Login Button
        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        loginBtn.setBackground(new Color(70, 130, 180));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(loginBtn, gbc);

        // Register Button
        JButton registerBtn = new JButton("Register");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        registerBtn.setBackground(new Color(100, 149, 237));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        gbc.gridx = 1;
        formPanel.add(registerBtn, gbc);

        // -------- SPLIT PANE --------
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imagePanel, formPanel);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(0);
        splitPane.setEnabled(false);  // Disable user dragging divider

        add(splitPane);

        // -------- BUTTON ACTIONS --------
        loginBtn.addActionListener(this::handleLogin);
        registerBtn.addActionListener(e -> {
            dispose();
            new Register();
        });

        setVisible(true);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.");
            return;
        }
        if (UserManager.validateUser(username, password)) {
            dispose();
            new Dashboard(username);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials. Try again or register.");
        }
    }
}
