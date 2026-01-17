package de.sty.fileserv.auth.file.smbpasswd;

import de.sty.fileserv.auth.file.smb.utils.SmbUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmbPasswdCliTest {

    @TempDir
    Path tempDir;

    /**
     * Tests user addition; verifies file creation and hash correctness
     */
    @Test
    void testAddUser() throws Exception {
        Path smbPasswdFile = tempDir.resolve("smbpasswd");
        SmbPasswdCli cli = new SmbPasswdCli();
        cli.smbPasswdFile = smbPasswdFile;
        cli.addUser = true;
        cli.username = "testuser";
        cli.password = "testpass";

        Integer exitCode = cli.call();

        assertThat(exitCode).isEqualTo(0);
        assertThat(smbPasswdFile).exists();

        List<String> lines = Files.readAllLines(smbPasswdFile);
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst()).startsWith("testuser:1000:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:");
        
        String expectedHash = SmbUtils.ntlmHash("testpass");
        assertThat(lines.getFirst()).contains(":" + expectedHash + ":");
    }

    /**
     * Tests user update by verifying hash replacement and absence of old hash
     */
    @Test
    void testUpdateUser() throws Exception {
        Path smbPasswdFile = tempDir.resolve("smbpasswd");
        Files.writeString(smbPasswdFile, "testuser:1000:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:OLDHASH:[U          ]:LCT-0\n");

        SmbPasswdCli cli = new SmbPasswdCli();
        cli.smbPasswdFile = smbPasswdFile;
        cli.username = "testuser";
        cli.password = "newpass";

        Integer exitCode = cli.call();

        assertThat(exitCode).isEqualTo(0);
        List<String> lines = Files.readAllLines(smbPasswdFile);
        assertThat(lines).hasSize(1);
        
        String expectedHash = SmbUtils.ntlmHash("newpass");
        assertThat(lines.getFirst()).contains(":" + expectedHash + ":");
        assertThat(lines.getFirst()).doesNotContain("OLDHASH");
    }

    /**
     * Tests user update fails when the user doesn't exist
     */
    @Test
    void testUserNotFound() throws Exception {
        Path smbPasswdFile = tempDir.resolve("smbpasswd");
        Files.createFile(smbPasswdFile);

        SmbPasswdCli cli = new SmbPasswdCli();
        cli.smbPasswdFile = smbPasswdFile;
        cli.username = "nonexistent";
        cli.password = "pass";
        cli.addUser = false;

        Integer exitCode = cli.call();

        assertThat(exitCode).isEqualTo(1);
    }

    /**
     * Tests CLI user addition; validates file creation and user presence
     */
    @Test
    void testCommandLineAddUser() throws Exception {
        Path smbPasswdFile = tempDir.resolve("smbpasswd");
        SmbPasswdCli app = new SmbPasswdCli();
        CommandLine cmd = new CommandLine(app);
        
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("-c", smbPasswdFile.toString(), "-a", "cliuser", "clipass");

        assertThat(exitCode).isEqualTo(0);
        assertThat(smbPasswdFile).exists();
        assertThat(Files.readAllLines(smbPasswdFile)).anyMatch(line -> line.startsWith("cliuser:"));
    }
}
