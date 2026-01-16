package de.sty.fileserv.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiAuthenticatorTest {

    @Test
    void authenticatesAgainstMultipleAuthenticators() {
        Authenticator auth1 = new SimpleAuthenticator("alice", "secret");
        Authenticator auth2 = new SimpleAuthenticator("bob", "pass123");

        MultiAuthenticator multi = new MultiAuthenticator(auth1, auth2);

        // From first
        assertThat(multi.authenticate("alice", "secret")).isTrue();
        // From second
        assertThat(multi.authenticate("bob", "pass123")).isTrue();
        
        // Mismatches
        assertThat(multi.authenticate("alice", "wrong")).isFalse();
        assertThat(multi.authenticate("bob", "wrong")).isFalse();
        assertThat(multi.authenticate("unknown", "any")).isFalse();
    }
    
    @Test
    void testToString() {
        Authenticator auth1 = new SimpleAuthenticator("alice", "secret");
        Authenticator auth2 = new SimpleAuthenticator("charlie", "secret");

        MultiAuthenticator multi = new MultiAuthenticator(auth1, auth2);

        assertThat(multi.toString())
                .contains("MultiAuthenticator")
                .contains("SimpleAuthenticator")
                .contains("alice")
                .contains("charlie");
    }
}
