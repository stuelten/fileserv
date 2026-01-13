package de.sty.fileserv.dummy.auth;

import de.sty.fileserv.core.Authenticator;
import de.sty.fileserv.core.AuthenticatorFactory;

import java.util.Map;

public class DummyAuthenticatorFactory implements AuthenticatorFactory {
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
        return new DummyAuthenticator(acceptAll);
    }
}
