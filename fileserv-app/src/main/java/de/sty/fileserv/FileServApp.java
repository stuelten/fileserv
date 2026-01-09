package de.sty.fileserv;

import de.sty.fileserv.core.*;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

// version is set by maven via filtering
@Command(name = "fileserv", mixinStandardHelpOptions = true,
        versionProvider = FileServApp.VersionProvider.class,
        description = "A simple WebDAV server.")
public class FileServApp implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(FileServApp.class);

    /** The version of this app */
    public static final String VERSION = de.sty.fileserv.core.FileServVersion.VERSION;

    @Option(names = {"--config"}, description = "Path to a configuration properties file", defaultValue = "fileserv.properties")
    Path configFile;

    @Parameters(index = "0", description = "Data directory to serve", defaultValue = "./data")
    Path root;
    @Option(names = {"--https-port"}, description = "HTTPS port", defaultValue = "8443")
    int httpsPort;
    @Option(names = {"--http-port"}, description = "HTTP port (set to 0 to disable)", defaultValue = "8080")
    int httpPort;
    @Option(names = {"-u", "--user"}, description = "User name for authentication. Must followed by password.")
    private List<String> users;
    @Option(names = {"-p", "--password"}, description = "Password for authentication. A username must be given prior.")
    private List<String> passwords;
    @Option(names = {"--keystore"}, description = "Path to the keystore file with SSL certificate", defaultValue = "keystore.p12")
    private String keyStorePath;

    @Option(names = {"--keystore-password"}, description = "Keystore password", defaultValue = "changeit")
    private String keyStorePassword;

    @Option(names = {"--key-pass"}, description = "Key password (defaults to keystore password)")
    private String keyPassword;

    @Option(names = {"--behind-proxy"}, description = "Trust X-Forwarded-* headers", defaultValue = "true")
    private boolean behindProxy;

    @Option(names = {"--passwd"}, description = "Path to a passwords file")
    private Path passwordsPath;

    public static void main(String[] args) {
        FileServApp app = new FileServApp();
        CommandLine cmd = new CommandLine(app);

        // Pre-parse to find --config option
        CommandLine.ParseResult preParse = cmd.parseArgs(args);
        Path configPath = app.configFile;

        cmd.setDefaultValueProvider(new PropertiesDefaultProvider(configPath));

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Authenticator authenticator = createAuthenticator();

        root = root.toAbsolutePath().normalize();
        if (keyPassword == null) {
            keyPassword = keyStorePassword;
        }

        WebDavServer.Config cfg = new WebDavServer.Config(
                root,
                behindProxy,
                httpPort,
                httpsPort,
                keyStorePath,
                keyStorePassword,
                keyPassword,
                authenticator
        );

        Server server = WebDavServer.build(cfg);

        LOG.info("Starting FileServ with configuration:");
        LOG.info("  HTTPS: {}://localhost:{}/", "https", httpsPort);
        if (httpPort > 0) {
            LOG.info("  HTTP : {}://localhost:{}/", "http", httpPort);
        } else {
            LOG.info("  HTTP : disabled");
        }
        LOG.info("  Root: {}", root);
        LOG.info("  Auth: {}", authenticator);
        LOG.info("  behindProxy={}", behindProxy);

        server.start();
        LOG.info("Jetty server started.");
        server.join();

        return 0;
    }

    /**
     * Creates authenticator from files and CLI parameters
     */
    de.sty.fileserv.core.Authenticator createAuthenticator() {
        List<de.sty.fileserv.core.Authenticator> authenticators = new ArrayList<>();

        Path passwdFile = passwordsPath;
        if (passwdFile == null) {
            Path defaultPasswd = Paths.get("passwd");
            if (Files.exists(defaultPasswd)) {
                passwdFile = defaultPasswd;
            }
        }

        if (passwdFile != null) {
            authenticators.add(new FileAuthenticator(passwdFile));
            LOG.info("AUTH: Use usernames/passwords from file: {}", passwdFile);
        }

        // 3. User/Passwd pairs from CLI
        if (users != null && !users.isEmpty()) {
            for (int i = 0; i < users.size(); i++) {
                String u = users.get(i);
                String p = (passwords != null && i < passwords.size()) ? passwords.get(i) : "";
                authenticators.add(new SimpleAuthenticator(u, p));
                LOG.info("Added authentication for user: {}", u);
            }
        }

        if (authenticators.isEmpty()) {
            authenticators.add(new SimpleAuthenticator("alice", "secret"));
            LOG.info("No authentication configured. Using default: alice:secret");
        }

        return new MultiAuthenticator(authenticators);
    }

    /**
     * A picocli default value provider that looks up values in:
     * 1. System properties (prefixed with 'fileserv.')
     * 2. Environment variables (prefixed with 'FILESERV_')
     * 3. A properties file (defaulting to fileserv.properties)
     */
    public static class PropertiesDefaultProvider implements CommandLine.IDefaultValueProvider {
        private final Properties properties = new Properties();

        public PropertiesDefaultProvider(Path configFile) {
            if (configFile != null && Files.exists(configFile)) {
                try (var reader = Files.newBufferedReader(configFile)) {
                    properties.load(reader);
                } catch (IOException e) {
                    LOG.warn("Failed to load configuration from {}: {}", configFile, e.getMessage());
                }
            }
        }

        @Override
        public String defaultValue(CommandLine.Model.ArgSpec argSpec) {
            String name = null;
            if (argSpec.isOption()) {
                name = ((CommandLine.Model.OptionSpec) argSpec).longestName();
            } else if (argSpec.isPositional()) {
                name = argSpec.paramLabel();
            }

            if (name == null) return null;

            // Remove leading dashes
            while (name.startsWith("-")) {
                name = name.substring(1);
            }

            // 1. System property: fileserv.http-port
            String sysProp = System.getProperty("fileserv." + name);
            if (sysProp != null) return sysProp;

            // 2. Environment variable: FILESERV_HTTP_PORT
            String envVar = System.getenv("FILESERV_" + name.toUpperCase().replace("-", "_"));
            if (envVar != null) return envVar;

            // 3. Properties file: http-port
            return properties.getProperty(name);
        }
    }

    public static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{de.sty.fileserv.core.FileServVersion.VERSION};
        }
    }
}
