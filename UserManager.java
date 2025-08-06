package auth;

import java.io.*;

public class UserManager {
    private static final String FILE_NAME = "data/users.txt";
    private static final File FILE = new File(FILE_NAME);

    public static boolean isUserExists(String username) {
        try {
            if (!FILE.exists()) return false;
            BufferedReader reader = new BufferedReader(new FileReader(FILE));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equalsIgnoreCase(username)) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean validateUser(String username, String password) {
        try {
            if (!FILE.exists()) return false;
            BufferedReader reader = new BufferedReader(new FileReader(FILE));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2 && parts[0].equalsIgnoreCase(username) && parts[1].equals(password)) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean registerUser(String username, String password) {
        if (isUserExists(username)) return false;
        try {
            FILE.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(FILE, true));
            writer.write(username + "," + password);
            writer.newLine();
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}