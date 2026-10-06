package com.example.ecommerce.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordConstraintValidatorTest {

    private final PasswordConstraintValidator validator = new PasswordConstraintValidator();

    @Test
    void acceptsStrongPassword() {
        assertThat(validator.isValid("Passw0rd123", null)).isTrue();
    }

    @Test
    void rejectsWeakPasswords() {
        assertThat(validator.isValid("short1A", null)).isFalse();          // too short
        assertThat(validator.isValid("alllowercase1", null)).isFalse();    // no upper-case
        assertThat(validator.isValid("ALLUPPERCASE1", null)).isFalse();    // no lower-case
        assertThat(validator.isValid("NoDigitsHere", null)).isFalse();     // no digit
    }

    @Test
    void rejectsPasswordsLongerThanBcryptLimit() {
        String tooLong = "Aa1" + "x".repeat(70); // 73 characters
        assertThat(validator.isValid(tooLong, null)).isFalse();
    }
}
