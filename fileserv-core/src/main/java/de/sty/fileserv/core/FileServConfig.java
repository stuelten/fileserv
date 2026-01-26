package de.sty.fileserv.core;

import java.nio.file.Path;

public record FileServConfig(
        Path dataDir,
        boolean behindProxy,
        boolean allowHttp,
        int httpPort,
        int httpsPort,
        String keyStorePath,
        String keyStorePassword,
        String keyPassword,
        Authenticator authenticator
) {
}
