package de.sty.fileserv.ldap;

import de.sty.fileserv.core.Authenticator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * LDAP Authenticator that attempts to bind to an LDAP server with the provided credentials.
 */
public final class LdapAuthenticator implements Authenticator {
    private static final Logger LOG = LoggerFactory.getLogger(LdapAuthenticator.class);

    private final String ldapUrl;
    private final String userDnPattern;

    /**
     * @param ldapUrl       The LDAP server URL (e.g., ldap://localhost:389)
     * @param userDnPattern The pattern to construct the user DN (e.g., "uid=%s,ou=users,dc=example,dc=com")
     */
    public LdapAuthenticator(String ldapUrl, String userDnPattern) {
        this.ldapUrl = ldapUrl;
        this.userDnPattern = userDnPattern;
    }

    @Override
    public boolean authenticate(String user, String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        String userDn = String.format(userDnPattern, user);
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            LOG.info("LDAP authentication successful for user: {}", user);
            return true;
        } catch (NamingException e) {
            LOG.debug("LDAP authentication failed for user: {}", user, e);
            return false;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    LOG.error("Failed to close LDAP context", e);
                }
            }
        }
    }
    @Override
    public String toString() {
        return "LdapAuthenticator[url=" + ldapUrl + ", dnPattern=" + userDnPattern + "]";
    }
}
