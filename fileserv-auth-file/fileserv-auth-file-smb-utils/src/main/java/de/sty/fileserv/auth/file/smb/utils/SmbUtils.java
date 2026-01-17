package de.sty.fileserv.auth.file.smb.utils;

import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class SmbUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SmbUtils.class);

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

}
