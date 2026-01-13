package de.sty.fileserv.core;

import java.nio.file.Paths;
import java.util.Map;

public class FileAuthenticatorFactory implements AuthenticatorFactory {
    @Override
    public String getType() {
        return "file";
    }

    @Override
    public Authenticator create(Map<String, String> config) {
        String path = config.get("path");
        if (path == null) {
            throw new IllegalArgumentException("Missing 'path' configuration for 'file' authenticator");
        }
        return new FileAuthenticator(Paths.get(path));
    }
}
