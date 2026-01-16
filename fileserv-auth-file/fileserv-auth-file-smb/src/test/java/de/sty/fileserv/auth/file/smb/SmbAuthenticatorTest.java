package de.sty.fileserv.auth.file.smb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SmbAuthenticatorTest {

    @TempDir
    Path tempDir;

    @Test
    void authenticatesCorrectlyFromSmbpasswdFile() throws IOException {
        Path authFile = tempDir.resolve("smbpasswd");
        String hash = SmbAuthenticator.ntlmHash("secret");
        Files.writeString(authFile, "alice:1000:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:" + hash + ":[U          ]:LCT-65A6E6A0\n");

        SmbAuthenticator auth = new SmbAuthenticator(authFile);

        assertThat(auth.authenticate("alice", "secret")).isTrue();
        assertThat(auth.authenticate("alice", "wrong")).isFalse();
        assertThat(auth.authenticate("unknown", "secret")).isFalse();
    }

    @Test
    void testNtlmHash() {
        // 'password' NTLM hash
        assertThat(SmbAuthenticator.ntlmHash("password")).isEqualTo("8846F7EAEE8FB117AD06BDD830B7586C");
    }
}
