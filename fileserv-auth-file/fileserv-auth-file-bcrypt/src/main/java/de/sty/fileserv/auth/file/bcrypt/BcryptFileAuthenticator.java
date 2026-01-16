package de.sty.fileserv.auth.file.bcrypt;

import de.sty.fileserv.core.AbstractFileAuthenticator;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BcryptFileAuthenticator extends AbstractFileAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(BcryptFileAuthenticator.class);

    public BcryptFileAuthenticator(Path path) {
        super(path);
    }

    @Override
    protected Map<String, String> parseLines(Stream<String> lines) {
        return lines
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split(":", -1))
                .filter(parts -> parts.length >= 2 && !parts[0].isEmpty())
                .collect(Collectors.toUnmodifiableMap(parts -> parts[0], parts -> parts[1], (existing, replacement) -> replacement));
    }

    @Override
    public boolean authenticateCheck(String user, String hashedPassword, String password) {
        if (hashedPassword == null) {
            LOG.debug("User '{}' not found", user);
            return false;
        }

        try {
            boolean matches = BCrypt.checkpw(password, hashedPassword);
            return matches;
        } catch (Exception e) {
            LOG.warn("Failed to check bcrypt password for user '{}'", user, e);
            return false;
        }
    }

}
