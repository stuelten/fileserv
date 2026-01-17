package de.sty.fileserv.auth.file.smbpasswd;

import de.sty.fileserv.auth.file.smb.utils.SmbUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "fileserv-smbpasswd", mixinStandardHelpOptions = true, description = "Manage Samba passwd files.")
public class SmbPasswdCli implements Callable<Integer> {

    @Option(names = {"-c", "--file"}, description = "Path to the smbpasswd file", defaultValue = "/etc/samba/smbpasswd")
    Path smbPasswdFile;

    @Option(names = {"-a", "--add"}, description = "Add a new user")
    boolean addUser;

    @Option(names = {"-q", "--quiet"}, description = "Be quiet if no error occurs")
    boolean quiet;

    @Parameters(index = "0", description = "User name")
    String username;

    @Parameters(index = "1", description = "Password", arity = "0..1")
    String password;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SmbPasswdCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        List<String> lines = new ArrayList<>();
        FileTime currentModified = null;

        if (Files.exists(smbPasswdFile)) {
            currentModified = Files.getLastModifiedTime(smbPasswdFile);
            lines = Files.readAllLines(smbPasswdFile);
        }

        boolean found = false;
        String hash = SmbUtils.ntlmHash(password == null ? "" : password);
        String timestamp = Long.toHexString(Instant.now().getEpochSecond()).toUpperCase();
        // Format: username:uid:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:NTLMHASH:[flags]:LCT-timestamp
        String newLine = username + ":1000:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:" + hash + ":[U          ]:LCT-" + timestamp;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(username + ":")) {
                lines.set(i, newLine);
                found = true;
                break;
            }
        }

        if (!found) {
            if (addUser) {
                lines.add(newLine);
            } else {
                System.err.println("ERROR: User '" + username + "' not found in '" + smbPasswdFile + "'. Use -a to add.");
                return 1;
            }
        }

        if (currentModified != null) {
            FileTime checkNotChangedConcurrently = Files.getLastModifiedTime(smbPasswdFile);
            if (!currentModified.equals(checkNotChangedConcurrently)) {
                System.err.println("ERROR: File '" + smbPasswdFile + "' was modified concurrently. Aborting update.");
                return 1;
            }
        }

        Files.write(smbPasswdFile, lines);
        if (!quiet) {
            System.out.println("INFO: User '" + username + "' updated in '" + smbPasswdFile + "'");
        }
        return 0;
    }

}
