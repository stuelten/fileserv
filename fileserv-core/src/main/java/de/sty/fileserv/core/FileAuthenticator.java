package de.sty.fileserv.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(FileAuthenticator.class);

    private final Map<String, String> users = new LinkedHashMap<>();

    public FileAuthenticator(Path path) {
        if (!Files.exists(path)) {
            LOG.error("Authentication file does not exist: '{}'", path);
            throw new IllegalArgumentException("Authentication file does not exist: " + path);
        }
        LOG.debug("Loading authentication file: '{}'", path);
        try (Stream<String> lines = Files.lines(path)) {
            users.putAll(lines
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.split(":", -1))
                    .filter(parts -> parts.length >= 2 && !parts[0].isEmpty())
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (existing, replacement) -> replacement)));
        } catch (IOException e) {
            LOG.error("Failed to read authentication file: '{}'", path, e);
            throw new RuntimeException("Failed to read authentication file: " + path, e);
        }
        LOG.info("Loaded {} users from authentication file: '{}'", users.size(), path);
    }

    @Override
    public boolean authenticate(String user, String password) {
        String expectedPass = users.get(user);
        boolean matches = expectedPass != null && expectedPass.equals(password);
        LOG.debug("User '{}' authenticates: {}", user, matches);
        return matches;
    }

}
