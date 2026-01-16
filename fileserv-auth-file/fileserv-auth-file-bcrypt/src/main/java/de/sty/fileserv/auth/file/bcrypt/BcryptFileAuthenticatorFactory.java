package de.sty.fileserv.auth.file.bcrypt;

import de.sty.fileserv.core.Authenticator;
import de.sty.fileserv.core.AuthenticatorFactory;

import java.nio.file.Paths;
import java.util.Map;

public class BcryptFileAuthenticatorFactory implements AuthenticatorFactory {
    @Override
    public String getType() {
        return "file-bcrypt";
    }

    @Override
    public Authenticator create(Map<String, String> config) {
        String path = config.get("path");
        if (path == null) {
            throw new IllegalArgumentException("Missing 'path' configuration for 'bcrypt' authenticator");
        }
        return new BcryptFileAuthenticator(Paths.get(path));
    }

    @Override
    public String toString() {
        return getClass().getName() + "[type=" + getType() + "]";
    }

}
