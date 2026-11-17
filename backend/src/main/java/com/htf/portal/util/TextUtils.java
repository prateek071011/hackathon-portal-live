package com.htf.portal.util;

import java.util.Locale;

/**
 * Small text clean-up helpers shared by several services. Static helpers only.
 */
public final class TextUtils {

    private TextUtils() {
    }

    // Emails are compared in lower case, so "Asha@Mail.com" and "asha@mail.com" are the same account.
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // Optional text fields: "   " is stored as NULL, anything else is trimmed.
    public static String trimToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
