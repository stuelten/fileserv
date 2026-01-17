package de.sty.fileserv.auth.file.smb;

import de.sty.fileserv.auth.file.smb.utils.SmbUtils;
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
        String hash = SmbUtils.ntlmHash("secret");
        Files.writeString(authFile, "alice:1000:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:" + hash + ":[U          ]:LCT-65A6E6A0\n");

        SmbAuthenticator auth = new SmbAuthenticator(authFile);

        assertThat(auth.authenticate("alice", "secret")).isTrue();
        assertThat(auth.authenticate("alice", "wrong")).isFalse();
        assertThat(auth.authenticate("unknown", "secret")).isFalse();
    }

}
