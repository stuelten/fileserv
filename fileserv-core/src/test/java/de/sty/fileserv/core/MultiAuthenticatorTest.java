package de.sty.fileserv.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MultiAuthenticatorTest {

    @TempDir
    Path tempDir;

    @Test
    void authenticatesAgainstMultipleAuthenticators() throws IOException {
        Path authFile = tempDir.resolve("auth.txt");
        Files.writeString(authFile, "bob:pass123\n");

        Authenticator simple = new SimpleAuthenticator("alice", "secret");
        Authenticator file = new FileAuthenticator(authFile);

        MultiAuthenticator multi = new MultiAuthenticator(simple, file);

        // From SimpleAuthenticator
        assertThat(multi.authenticate("alice", "secret")).isTrue();
        // From FileAuthenticator
        assertThat(multi.authenticate("bob", "pass123")).isTrue();
        
        // Mismatches
        assertThat(multi.authenticate("alice", "wrong")).isFalse();
        assertThat(multi.authenticate("bob", "wrong")).isFalse();
        assertThat(multi.authenticate("unknown", "any")).isFalse();
    }
}
