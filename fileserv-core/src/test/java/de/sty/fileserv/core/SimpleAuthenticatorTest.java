package de.sty.fileserv.core;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SimpleAuthenticatorTest {

    @Test
    void authenticateReturnsTrueOnMatch() {
        SimpleAuthenticator auth = new SimpleAuthenticator("user", "pass");
        assertThat(auth.authenticate("user", "pass")).isTrue();
    }

    @Test
    void authenticateReturnsFalseOnMismatch() {
        SimpleAuthenticator auth = new SimpleAuthenticator("user", "pass");
        assertThat(auth.authenticate("wrong", "pass")).isFalse();
        assertThat(auth.authenticate("user", "wrong")).isFalse();
        assertThat(auth.authenticate("", "")).isFalse();
    }
}
