package de.sty.fileserv.auth.file.smb;

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
        assertThat(lines.get(0)).startsWith("testuser:1000:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:");
        
        String expectedHash = SmbAuthenticator.ntlmHash("testpass");
        assertThat(lines.get(0)).contains(":" + expectedHash + ":");
    }

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
        
        String expectedHash = SmbAuthenticator.ntlmHash("newpass");
        assertThat(lines.get(0)).contains(":" + expectedHash + ":");
        assertThat(lines.get(0)).doesNotContain("OLDHASH");
    }

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
