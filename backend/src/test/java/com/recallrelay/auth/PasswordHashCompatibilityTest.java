package com.recallrelay.auth;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashCompatibilityTest {
    @Test
    void bcrypt2yVariantMatchesWithoutAStoredPasswordFixture() {
        String password = UUID.randomUUID().toString();
        var encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        String compatibleHash = "$2y$" + hash.substring(4);
        assertThat(encoder.matches(password, compatibleHash)).isTrue();
    }
}
