package com.chroniccare.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    private PasswordUtils() {
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("$2y$") || storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            String normalizedHash = storedPassword.replaceFirst("^\\$2y\\$", "\\$2a\\$");
            try {
                return BCrypt.checkpw(rawPassword, normalizedHash);
            } catch (Exception e) {
                return false;
            }
        }

        return rawPassword.equals(storedPassword);
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }
}