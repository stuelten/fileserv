package de.sty.fileserv.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(FileAuthenticator.class);
    private static final long RELOAD_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);

    private final Path path;
    private volatile Map<String, String> users = Map.of();
    private volatile FileTime lastModified;
    private volatile long lastChecked;

    public FileAuthenticator(Path path) {
        this.path = path;
        reload();
    }

    private void reload() {
        if (!Files.exists(path)) {
            LOG.error("Authentication file does not exist: '{}'", path);
            if (users.isEmpty()) {
                throw new IllegalArgumentException("Authentication file does not exist: " + path);
            }
            return;
        }

        try {
            FileTime currentModified = Files.getLastModifiedTime(path);
            if (currentModified.equals(lastModified)) {
                return;
            }

            LOG.debug("Loading authentication file: '{}'", path);
            try (Stream<String> lines = Files.lines(path)) {
                Map<String, String> newUsers = lines
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .map(line -> line.split(":", -1))
                        .filter(parts -> parts.length >= 2 && !parts[0].isEmpty())
                        .collect(Collectors.toUnmodifiableMap(parts -> parts[0], parts -> parts[1], (existing, replacement) -> replacement));
                LOG.debug("Reload: {} users old, {} users new, reading from file '{}'", users.size(), newUsers.size(), path);
                users = newUsers;
                lastModified = currentModified;
                LOG.info("Loaded {} users from authentication file: '{}'", users.size(), path);
            }
        } catch (IOException e) {
            LOG.error("Failed to read authentication file: '{}'", path, e);
            if (users.isEmpty()) {
                throw new RuntimeException("Failed to read authentication file: " + path, e);
            }
        }
    }

    private void reloadIfNecessary() {
        long now = System.currentTimeMillis();
        if (now - lastChecked > RELOAD_INTERVAL_MS) {
            lastChecked = now;
            reload();
        }
    }

    @Override
    public boolean authenticate(String user, String password) {
        reloadIfNecessary();
        String expectedPass = users.get(user);
        boolean matches = expectedPass != null && expectedPass.equals(password);
        LOG.debug("User '{}' authenticates: {}", user, matches);
        return matches;
    }

    @Override
    public String toString() {
        return "FileAuthenticator[path=" + path + ", lastModified=" + lastModified + ", users=" + users.keySet() + "]";
    }
}
