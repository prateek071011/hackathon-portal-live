package com.htf.portal.util;

import java.security.SecureRandom;

/**
 * Makes the short public code every student gets, e.g. HTF-7K3Q9M. Leaders type it to invite someone.
 * Everything here is static: the class has no state of its own besides one shared random generator.
 */
public final class UserCodeGenerator {

    // No 0/O and no 1/I, so a code read aloud or copied from a screenshot cannot be mixed up.
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    // static: one generator shared by the whole app. SecureRandom (not Random) so codes cannot be predicted.
    private static final SecureRandom RANDOM = new SecureRandom();

    // Private constructor: this class only has static methods, so nobody should create an object of it.
    private UserCodeGenerator() {
    }

    public static String generate(String prefix) {
        StringBuilder code = new StringBuilder(prefix);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            code.append(ALPHABET.charAt(index));
        }
        return code.toString();
    }
}
