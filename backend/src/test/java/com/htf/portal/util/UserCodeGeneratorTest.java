package com.htf.portal.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Checks that generated user codes always have the right shape and never use the
 * easily-confused characters 0, O, 1 and I.
 */
class UserCodeGeneratorTest {

    @Test
    void codesHavePrefixAndSixSafeCharacters() {
        for (int i = 0; i < 1000; i++) {
            String code = UserCodeGenerator.generate("HTF-");
            assertTrue(code.matches("HTF-[2-9A-HJ-NP-Z]{6}"), "Bad code: " + code);
        }
    }
}
