package de.sty.fileserv.auth.file.smb;

import de.sty.fileserv.core.AbstractFileAuthenticator;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
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

    /**
     * Calculates NTLM hash (MD4 of UTF-16LE password)
     */
    public static String ntlmHash(String password) {
        String result = "";
        if (password != null) {
            try {
                // NTLM hash is MD4(UTF-16LE(password))
                byte[] input = password.getBytes(StandardCharsets.UTF_16LE);

                MD4Digest md4 = new MD4Digest();
                md4.update(input, 0, input.length);
                byte[] digest = new byte[md4.getDigestSize()];
                md4.doFinal(digest, 0);
                result = Hex.toHexString(digest).toUpperCase();
            } catch (Exception e) {
                LOG.warn("Ignoring error while calculating NTLM hash.", e);
            }
        }
        return result;
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
            String actualHash = ntlmHash(password);
            result = expectedHash.equalsIgnoreCase(actualHash);
        }

        return result;
    }

}
