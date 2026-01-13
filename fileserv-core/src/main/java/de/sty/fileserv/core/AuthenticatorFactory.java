package de.sty.fileserv.core;

import java.util.Map;

/**
 * Factory for creating Authenticator instances.
 */
public interface AuthenticatorFactory {
    /**
     * Unique name of the authenticator type (e.g., "ldap", "file").
     */
    String getType();

    /**
     * Creates an Authenticator with the given configuration.
     * @param config A map of configuration options.
     * @return An Authenticator instance.
     */
    Authenticator create(Map<String, String> config);
}
