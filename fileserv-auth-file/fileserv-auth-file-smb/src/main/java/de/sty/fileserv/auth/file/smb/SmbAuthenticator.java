package de.sty.fileserv.auth.file.smb;

import de.sty.fileserv.auth.file.smb.utils.SmbUtils;
import de.sty.fileserv.core.AbstractFileAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Authenticator using Samba smbpasswd file format.
 * Format: username:uid:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:NTLMHASH:[flags]:LCT-timestamp
 * We only support NTLM hash (the 4th field).
 */
public final class SmbAuthenticator extends AbstractFileAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(SmbAuthenticator.class);

    public SmbAuthenticator(Path path) {
        super(path);
    }

    @Override
    protected Map<String, String> parseLines(Stream<String> lines) {
        return lines
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split(":", -1))
                .filter(parts -> parts.length >= 4 && !parts[0].isEmpty())
                .collect(Collectors.toUnmodifiableMap(
                        parts -> parts[0],
                        parts -> parts[3].toUpperCase(),
                        (existing, replacement) -> replacement));
    }

    @Override
    public boolean authenticateCheck(String user, String expectedHash, String password) {
        boolean result = false;

        if (expectedHash == null || expectedHash.equals("NO PASSWORDXXXXXXXXXXXXXXXXXXXXX")) {
            LOG.debug("User '{}' not found or has no password in smbpasswd", user);
        } else {
            String actualHash = SmbUtils.ntlmHash(password);
            result = expectedHash.equalsIgnoreCase(actualHash);
        }

        return result;
    }

}
