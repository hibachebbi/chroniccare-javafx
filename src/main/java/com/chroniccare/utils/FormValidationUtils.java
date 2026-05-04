package com.chroniccare.utils;

public final class FormValidationUtils {

    private FormValidationUtils() {}

    public static boolean hasLetter(String value) {
        return value != null && value.matches(".*\\p{L}.*");
    }

    public static boolean isValidEventTitle(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.length() >= 5
                && trimmed.length() <= 100
                && hasLetter(trimmed);
    }

    public static boolean isValidLocation(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.matches("^[\\p{L}][\\p{L} '\\-]{1,79}$");
    }

    public static boolean isValidDescription(String value, int minLength) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.length() >= minLength
                && trimmed.length() <= 1200
                && hasLetter(trimmed);
    }

    public static boolean isPositiveIntInRange(String value, int min, int max) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= min && parsed <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidExerciseName(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        return trimmed.length() >= 3
                && trimmed.length() <= 100
                && hasLetter(trimmed)
                && trimmed.matches("^[\\p{L}0-9][\\p{L}0-9 '\\-]{2,99}$");
    }
}
