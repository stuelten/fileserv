package de.sty.fileserv.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileAuthenticatorTest {

    @TempDir
    Path tempDir;

    @Test
    void authenticatesCorrectlyFromFile() throws IOException {
        Path authFile = tempDir.resolve("auth.txt");
        Files.writeString(authFile, """
                # Comment
                alice:secret
                bob:pass123
                
                admin:admin:password
                """);

        FileAuthenticator auth = new FileAuthenticator(authFile);

        assertThat(auth.authenticate("alice", "secret")).isTrue();
        assertThat(auth.authenticate("bob", "pass123")).isTrue();
        assertThat(auth.authenticate("admin", "admin")).isTrue(); // Second field is password
        
        assertThat(auth.authenticate("alice", "wrong")).isFalse();
        assertThat(auth.authenticate("unknown", "secret")).isFalse();
    }

    @Test
    void throwsExceptionOnNonExistentFile() {
        Path missingFile = tempDir.resolve("missing.txt");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> 
                new FileAuthenticator(missingFile)
        );
    }
}
