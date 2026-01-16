package de.sty.fileserv.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Abstract base class for authenticators that load user data from a file.
 * <p>
 * This class supports reloading the credentials when the file changes
 * and provides a method for authenticating users.
 * </p>
 */
public abstract class AbstractFileAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFileAuthenticator.class);

    /**
     * Path to the authentication file
     */
    protected final Path path;
    /**
     * Cached user credentials.
     */
    protected volatile Map<String, String> users = Map.of();
    /**
     * Last modified time of the authentication file.
     */
    protected volatile FileTime lastModified;

    /**
     * Create a new instance of AbstractFileAuthenticator for the given file.
     *
     * @param path Path to the authentication file
     */
    protected AbstractFileAuthenticator(Path path) {
        this.path = path;
        if (!Files.exists(path)) {
            LOG.error("Authentication file does not exist: '{}'", path);
            throw new IllegalArgumentException("Authentication file does not exist: " + path);
        }
        reload();
    }

    /**
     * Reloads user credentials from the authentication file.
     * Logs changes and handles read errors.
     */
    protected final void reload() {
        try {
            LOG.debug("Loading authentication file: '{}'", path);
            Map<String, String> newUsers = readFile();
            LOG.info("Reload: {} users old, {} users new, reading from file '{}'", users.size(), newUsers.size(), path);

            users = newUsers;
        } catch (IOException e) {
            LOG.error("Failed to read authentication file: '{}'", path, e);
            if (users.isEmpty()) {
                throw new RuntimeException("Failed to read authentication file: " + path, e);
            }
        }
    }

    /**
     * Reads the authentication file,
     * calls {@link #parseLines(Stream)} with the lines read
     * and returns a map of user credentials.
     *
     * @return a map of user credentials
     * @throws IOException implementations may throw IOExceptions
     */
    protected Map<String, String> readFile() throws IOException {
        Stream<String> lines = Files.lines(path);
        Map<String, String> newUsers = parseLines(lines);
        return newUsers;
    }

    /**
     * Parses lines from the authentication file into a map of user credentials.
     *
     * @param lines the lines as read from the file
     * @return a map of users and their credentials
     */
    protected abstract Map<String, String> parseLines(Stream<String> lines);

    /**
     * Should check if the source for authentication changed.
     * <p>
     * Called by {@link #authenticate(String, String)} on every check.
     * </p>
     *
     * @return true if reload is necessary, false otherwise
     */
    protected boolean reloadIfNecessary() {
        return true;
    }

    /**
     * Determines if the authentication file changed.
     * Updates {@link #lastModified}.
     */
    protected boolean checkFileChanged() {
        if (!Files.exists(path)) {
            LOG.warn("Authentication file does not exist, " +
                    "clearing known users cache. File: '{}'", path);
            users.clear();
            return false;
        }

        FileTime currentModified = null;
        try {
            currentModified = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean fileChanged = !currentModified.equals(lastModified);
        lastModified = currentModified;
        return fileChanged;
    }

    /**
     * Authenticates a user with conditional reload
     * and credential check via {@link #authenticateCheck(String, String, String)}.
     */
    @Override
    public final boolean authenticate(String user, String password) {
        if (reloadIfNecessary() && checkFileChanged()) {
            reload();
        }

        String expectedPass = users.get(user);
        boolean matches = authenticateCheck(user, expectedPass, password);
        return matches;
    }

    /**
     * Check the given credentials.
     *
     * @param user           the user to check
     * @param expectedPasswd the expected password for the user
     * @param password       the password given
     * @return true if the password matches, false otherwise
     */
    protected abstract boolean authenticateCheck(String user, String expectedPasswd, String password);

    /**
     * Returns a string representation of this authenticator using the real class name.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[path=" + path + ", lastModified=" + lastModified + ", users=" + users.keySet() + "]";
    }

}
