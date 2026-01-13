package de.sty.fileserv.dummy.auth;

import de.sty.fileserv.core.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(DummyAuthenticator.class);
    private final boolean acceptAll;

    public DummyAuthenticator(boolean acceptAll) {
        this.acceptAll = acceptAll;
    }

    @Override
    public boolean authenticate(String user, String password) {
        LOG.info("DummyAuthenticator: acceptAll={}, user={}", acceptAll, user);
        return acceptAll;
    }

    @Override
    public String toString() {
        return "DummyAuthenticator[acceptAll=" + acceptAll + "]";
    }
}
