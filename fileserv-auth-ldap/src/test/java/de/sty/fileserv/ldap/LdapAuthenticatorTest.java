package de.sty.fileserv.ldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LdapAuthenticatorTest {

    @Test
    void rejectsEmptyPassword() {
        LdapAuthenticator auth = new LdapAuthenticator("ldap://localhost:389", "uid=%s,ou=users");
        assertThat(auth.authenticate("alice", "")).isFalse();
        assertThat(auth.authenticate("alice", null)).isFalse();
    }

    @Test
    void failsOnInvalidServer() {
        // This test verifies that it attempts to connect and fails gracefully (returns false) 
        // when the server is unreachable, instead of throwing an unhandled exception.
        LdapAuthenticator auth = new LdapAuthenticator("ldap://localhost:1", "uid=%s,ou=users");
        assertThat(auth.authenticate("alice", "secret")).isFalse();
    }
}
