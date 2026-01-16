package de.sty.fileserv.auth.file.bcrypt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptFileAuthenticatorTest {

    @TempDir
    Path tempDir;

    @Test
    void authenticatesCorrectlyFromBcryptFile() throws IOException {
        Path authFile = tempDir.resolve("bcrypt-auth.txt");
        String alicePass = BCrypt.hashpw("secret", BCrypt.gensalt());
        String bobPass = BCrypt.hashpw("pass123", BCrypt.gensalt());
        
        Files.writeString(authFile, "alice:" + alicePass + "\nbob:" + bobPass + "\n");

        BcryptFileAuthenticator auth = new BcryptFileAuthenticator(authFile);

        assertThat(auth.authenticate("alice", "secret")).isTrue();
        assertThat(auth.authenticate("bob", "pass123")).isTrue();
        
        assertThat(auth.authenticate("alice", "wrong")).isFalse();
        assertThat(auth.authenticate("unknown", "secret")).isFalse();
    }

}
