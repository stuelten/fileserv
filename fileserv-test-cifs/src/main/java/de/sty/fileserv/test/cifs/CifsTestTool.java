package de.sty.fileserv.test.cifs;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.EnumSet;
import java.util.concurrent.Callable;

@Command(name = "cifs-test", mixinStandardHelpOptions = true,
        description = "Simple CIFS test tool to verify a CIFS implementation.")
public class CifsTestTool implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(CifsTestTool.class);

    @Parameters(index = "0", description = "Host to connect to", defaultValue = "localhost")
    private String host;

    @Option(names = {"-p", "--port"}, description = "CIFS port", defaultValue = "445")
    private int port;

    @Option(names = {"-u", "--user"}, description = "Username", defaultValue = "demo")
    private String user;

    @Option(names = {"-pw", "--password"}, description = "Password", defaultValue = "demo")
    private String password;

    @Option(names = {"-d", "--domain"}, description = "Domain/Workgroup", defaultValue = "WORKGROUP")
    private String domain;

    @Option(names = {"-s", "--share"}, description = "Share name", defaultValue = "PUBLIC")
    private String shareName;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CifsTestTool()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        LOG.info("Connecting to smb://{}:{}/{} as {}/{}...", host, port, shareName, domain, user);

        try (SMBClient client = new SMBClient(); Connection connection = client.connect(host, port)) {
            AuthenticationContext ac = new AuthenticationContext(user, password.toCharArray(), domain);
            Session session = connection.authenticate(ac);

            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                LOG.info("Successfully connected to share.");

                // Test List
                LOG.info("Listing root directory:");
                share.list("").forEach(f -> LOG.info("  - {} ({})", f.getFileName(), f.getFileAttributes()));

                // Test Write
                String testFile = "test_" + System.currentTimeMillis() + ".txt";
                LOG.info("Creating test file: {}", testFile);
                byte[] content = "Hello from CIFS test tool".getBytes();
                try (File file = share.openFile(testFile,
                        EnumSet.of(AccessMask.GENERIC_WRITE),
                        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OVERWRITE_IF,
                        null)) {
                    file.write(content, 0);
                    LOG.info("File written.");
                }

                // Test Read
                LOG.info("Reading back test file:");
                try (File file = share.openFile(testFile,
                        EnumSet.of(AccessMask.GENERIC_READ),
                        null,
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        null)) {
                    byte[] buffer = new byte[content.length];
                    file.read(buffer, 0, 0, buffer.length);
                    LOG.info("Read content: {}", new String(buffer));
                }

                // Test Delete
                LOG.info("Deleting test file...");
                share.rm(testFile);
                LOG.info("Deleted.");
            }
        }

        LOG.info("CIFS test completed successfully.");
        return 0;
    }
}
