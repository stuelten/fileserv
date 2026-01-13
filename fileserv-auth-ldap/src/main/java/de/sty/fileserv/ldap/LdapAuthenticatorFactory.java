package de.sty.fileserv.ldap;

import de.sty.fileserv.core.Authenticator;
import de.sty.fileserv.core.AuthenticatorFactory;

import java.util.Map;

public class LdapAuthenticatorFactory implements AuthenticatorFactory {
    @Override
    public String getType() {
        return "ldap";
    }

    @Override
    public Authenticator create(Map<String, String> config) {
        String url = config.get("url");
        String dnPattern = config.get("dnPattern");
        if (url == null || dnPattern == null) {
            throw new IllegalArgumentException("Missing 'url' or 'dnPattern' configuration for 'ldap' authenticator");
        }
        return new LdapAuthenticator(url, dnPattern);
    }
}
