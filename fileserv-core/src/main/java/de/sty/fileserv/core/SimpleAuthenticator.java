package de.sty.fileserv.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple authenticator for one user and password.
 */
public class SimpleAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleAuthenticator.class);

    private final String expectedUser;
    private final String expectedPass;

    public SimpleAuthenticator(String expectedUser, String expectedPass) {
        this.expectedUser = expectedUser;
        this.expectedPass = expectedPass;
    }

    @Override
    public boolean authenticate(String user, String password) {
        boolean matches = expectedUser.equals(user) && expectedPass.equals(password);
        LOG.trace("Authenticating user={}: {}", user, matches);
        return matches;
    }

    @Override
    public String toString() {
        return "SimpleAuthenticator[user=" + expectedUser + "]";
    }
}
