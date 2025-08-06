package auth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class Register extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public Register() {
        setTitle("Register");
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
        JLabel title = new JLabel("Register");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(title, gbc);

        gbc.gridwidth = 1;

        // Username Label
        JLabel userLabel = new JLabel("New Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(userLabel, gbc);

        // Username Field
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        usernameField.setPreferredSize(new Dimension(250, 40));
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        // Password Label
        JLabel passLabel = new JLabel("New Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(passLabel, gbc);

        // Password Field
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        passwordField.setPreferredSize(new Dimension(250, 40));
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        // Register Button
        JButton registerBtn = new JButton("Register");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        registerBtn.setBackground(new Color(70, 130, 180));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(registerBtn, gbc);

        // Back Button
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        backBtn.setBackground(new Color(100, 149, 237));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        gbc.gridx = 1;
        formPanel.add(backBtn, gbc);

        // -------- SPLIT PANE --------
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imagePanel, formPanel);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(0);
        splitPane.setEnabled(false);

        add(splitPane);

        // -------- BUTTON ACTIONS --------
        registerBtn.addActionListener(this::handleRegister);
        backBtn.addActionListener(e -> {
            dispose();
            new Login();
        });

        setVisible(true);
    }

    private void handleRegister(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            return;
        }
        if (UserManager.registerUser(username, password)) {
            JOptionPane.showMessageDialog(this, "Registration successful. You can now login.");
            dispose();
            new Login();
        } else {
            JOptionPane.showMessageDialog(this, "Username already exists. Try another.");
        }
    }
}
