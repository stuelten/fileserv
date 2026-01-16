package de.sty.fileserv;

import de.sty.fileserv.auth.file.plaintext.InsecureFileAuthenticator;
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
import java.util.*;
import java.util.concurrent.Callable;

// version is set by maven via filtering
@Command(name = "fileserv", mixinStandardHelpOptions = true,
        versionProvider = FileServApp.VersionProvider.class,
        description = "A simple WebDAV server.")
public class FileServApp implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(FileServApp.class);

    /**
     * The version of this app
     */
    public static final String VERSION = de.sty.fileserv.core.FileServVersion.VERSION;

    @Option(names = {"--config"}, description = "Path to a configuration properties file", defaultValue = "fileserv.properties")
    Path configFile;

    @Parameters(index = "0", description = "Data directory to serve", defaultValue = "./data")
    Path dataDir;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"--https-port"}, description = "HTTPS port (set to -1 to disable)", defaultValue = "8443")
    int httpsPort;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"--http-port"}, description = "HTTP port (set to -1 to disable)", defaultValue = "8080")
    int httpPort;
    // Set by PicoCLI
    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    @Option(names = {"-u", "--user"}, description = "User name for authentication. Must followed by password.")
    private List<String> users;
    // Set by PicoCLI
    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    @Option(names = {"-p", "--password"}, description = "Password for authentication. A username must be given prior.")
    private List<String> passwords;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"--keystore"}, description = "Path to the keystore file with SSL certificate", defaultValue = "keystore.p12")
    private String keyStorePath;

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"--keystore-password"}, description = "Keystore password", defaultValue = "changeit")
    private String keyStorePassword;

    @Option(names = {"--key-pass"}, description = "Key password (defaults to keystore password)")
    private String keyPassword;

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"--behind-proxy"}, description = "Trust X-Forwarded-* headers", defaultValue = "true")
    private boolean behindProxy;

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"--passwd"}, description = "Path to a passwords file")
    private Path passwordsPath;

    // Set by PicoCLI
    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    @Option(names = {"--auth"}, description = "Authenticator configuration (e.g., ldap:url=ldap://...,dnPattern=...)")
    private List<String> authConfigs;

    private Server server;

    /**
     * Starts application; parses arguments; exits with status
     */
    public static void main(String[] args) {
        System.out.println("FileServ " + VERSION);
        LOG.info("Starting FileServ {}...", VERSION);

        FileServApp app = new FileServApp();
        CommandLine cmd = new CommandLine(app);

        // Pre-parse to find --config option
        //noinspection unused
        CommandLine.ParseResult preParse = cmd.parseArgs(args);
        Path configPath = app.configFile;

        cmd.setDefaultValueProvider(new PropertiesDefaultProvider(configPath));

        int exitCode = cmd.execute(args);
        LOG.info("FileServ stopped.");
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Authenticator authenticator = createAuthenticator();

        dataDir = dataDir.toAbsolutePath().normalize();
        if (keyPassword == null) {
            keyPassword = keyStorePassword;
        }

        FileServConfig cfg = new FileServConfig(
                dataDir,
                behindProxy,
                httpPort,
                httpsPort,
                keyStorePath,
                keyStorePassword,
                keyPassword,
                authenticator
        );

        server = WebDavServer.build(cfg);

        LOG.info("Starting FileServ with configuration:");
        if (httpsPort >= 0) {
            LOG.info("  HTTPS: {}://localhost:{}/", "https", httpsPort);
        } else {
            LOG.info("  HTTPS: disabled");
        }
        if (httpPort >= 0) {
            LOG.info("  HTTP : {}://localhost:{}/", "http", httpPort);
        } else {
            LOG.info("  HTTP : disabled");
        }
        LOG.info("  DataDir: {}", dataDir);
        LOG.info("  Auth: {}", authenticator);
        LOG.info("  behindProxy={}", behindProxy);

        server.start();
        LOG.info("File server runs...");
        server.join();

        return 0;
    }

    public void stop() throws Exception {
        if (server != null) {
            LOG.info("Stopping FileServ...");
            server.stop();
        }
    }

    public Server getServer() {
        return server;
    }

    /**
     * Creates authenticator from files and CLI parameters
     */
    Authenticator createAuthenticator() {
        List<Authenticator> authenticators = new ArrayList<>();

        // 1. Look up ServiceLoader-based authenticators
        if (authConfigs != null && !authConfigs.isEmpty()) {
            Map<String, AuthenticatorFactory> factories = new HashMap<>();
            LOG.info("AUTH: Loading authenticator factories via ServiceLoader");
            ServiceLoader.load(AuthenticatorFactory.class).forEach(f -> {
                LOG.info("AUTH: Found authenticator factory: {}", f);
                factories.put(f.getType(), f);
            });

            for (String authConfig : authConfigs) {
                int colonIdx = authConfig.indexOf(':');
                if (colonIdx == -1) {
                    LOG.error("Invalid auth config: {}. Expected type:key=value,...", authConfig);
                    throw new IllegalArgumentException("Invalid auth config: " + authConfig);
                }
                String type = authConfig.substring(0, colonIdx);
                String configStr = authConfig.substring(colonIdx + 1);
                Map<String, String> config = new HashMap<>();
                for (String pair : configStr.split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        config.put(kv[0], kv[1]);
                    }
                }

                AuthenticatorFactory factory = factories.get(type);
                if (factory != null) {
                    authenticators.add(factory.create(config));
                    LOG.info("AUTH: Added authenticator of type: {}", type);
                } else {
                    LOG.error("AUTH: No AuthenticatorFactory found for type: {}", type);
                    throw new IllegalArgumentException("No AuthenticatorFactory found for type: " + type);
                }
            }
        }

        // 2. legacy passwd file
        Path passwdFile = passwordsPath;
        if (passwdFile == null) {
            Path defaultPasswd = Paths.get("fileserv-passwd");
            if (Files.exists(defaultPasswd)) {
                passwdFile = defaultPasswd;
            }
        }
        if (passwdFile != null) {
            authenticators.add(new InsecureFileAuthenticator(passwdFile));
            LOG.info("AUTH: Use usernames/passwords from file: {}", passwdFile);
        }

        // 3. User/Passwd pairs from CLI
        if (users != null && !users.isEmpty()) {
            for (int i = 0; i < users.size(); i++) {
                String u = users.get(i);
                String p = (passwords != null && i < passwords.size()) ? passwords.get(i) : "";
                authenticators.add(new SimpleAuthenticator(u, p));
                LOG.info("AUTH: Added authentication for user: {}", u);
            }
        }

        if (authenticators.isEmpty()) {
            // Use Password between 100000 and 999999 to avoid leading zeros
            int randomPassword = 100000 + new Random().nextInt(900000);
            String password = String.valueOf(randomPassword);
            authenticators.add(new SimpleAuthenticator("demo", password));
            LOG.info("AUTH: No authentication configured. Using user 'demo', password '{}'", password);
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
