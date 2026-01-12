package de.sty.fileserv.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Authenticates against multiple authenticators.
 */
public class MultiAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(MultiAuthenticator.class);

    private final List<Authenticator> authenticators;

    public MultiAuthenticator(List<Authenticator> authenticators) {
        this.authenticators = List.copyOf(authenticators);
    }

    public MultiAuthenticator(Authenticator... authenticators) {
        this.authenticators = List.of(authenticators);
    }

    @Override
    public boolean authenticate(String user, String password) {
        for (Authenticator authenticator : authenticators) {
            if (authenticator.authenticate(user, password)) {
                LOG.debug("User '{}' authenticated by '{}'", user, authenticator);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "MultiAuthenticator" + authenticators;
    }

}
