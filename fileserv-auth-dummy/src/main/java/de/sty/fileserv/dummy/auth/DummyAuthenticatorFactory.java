package de.sty.fileserv.dummy.auth;

import de.sty.fileserv.core.Authenticator;
import de.sty.fileserv.core.AuthenticatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DummyAuthenticatorFactory implements AuthenticatorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DummyAuthenticatorFactory.class);

    @Override
    public String getType() {
        return "dummy";
    }

    @Override
    public Authenticator create(Map<String, String> config) {
        boolean acceptAll = true;
        String acceptValue = config.get("accept");
        if (acceptValue != null) {
            acceptAll = Boolean.parseBoolean(acceptValue);
        }
        LOG.info("Creating DummyAuthenticator with acceptAll: {}", acceptAll);
        return new DummyAuthenticator(acceptAll);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[type=" + getType() + "]";
    }

}
