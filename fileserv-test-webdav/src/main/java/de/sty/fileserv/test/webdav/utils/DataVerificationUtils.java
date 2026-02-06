package de.sty.fileserv.test.webdav.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * Utility class for generating and verifying deterministic test data.
 */
public class DataVerificationUtils {

    private DataVerificationUtils() {
        // Utility class
    }

    /**
     * Generates a deterministic block of data based on a seed.
     *
     * @param size the size of the data block in bytes
     * @param seed the seed for the random number generator
     * @return a byte array containing pseudo-random data
     */
    public static byte[] generateDeterministicData(int size, long seed) {
        byte[] data = new byte[size];
        new Random(seed).nextBytes(data);
        return data;
    }

    /**
     * Verifies that two byte arrays are equal.
     *
     * @param expected the expected data
     * @param actual   the actual data
     * @throws RuntimeException if the data does not match
     */
    public static void verifyContent(byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new RuntimeException("Content mismatch: expected " + expected.length + " bytes, got " + (actual != null ? actual.length : "null"));
        }
    }

    /**
     * Verifies that the content of an InputStream matches deterministic data.
     *
     * @param size   the expected size of the data
     * @param seed   the seed used to generate the deterministic data
     * @param actual the InputStream to verify
     * @throws IOException      if an I/O error occurs
     * @throws RuntimeException if the content does not match
     */
    public static void verifyContent(long size, long seed, InputStream actual) throws IOException {
        try (actual; DeterministicInputStream expected = new DeterministicInputStream(size, seed)) {
            byte[] expectedBuffer = new byte[8192];
            byte[] actualBuffer = new byte[8192];
            long totalRead = 0;
            int readExpected;
            while ((readExpected = expected.read(expectedBuffer)) != -1) {
                int readActual = 0;
                while (readActual < readExpected) {
                    int n = actual.read(actualBuffer, readActual, readExpected - readActual);
                    if (n == -1) {
                        throw new RuntimeException("Content mismatch: actual stream ended prematurely at " + (totalRead + readActual) + " bytes, expected " + size);
                    }
                    readActual += n;
                }
                if (!Arrays.equals(expectedBuffer, 0, readExpected, actualBuffer, 0, readExpected)) {
                    throw new RuntimeException("Content mismatch at offset around " + totalRead);
                }
                totalRead += readExpected;
            }
            if (actual.read() != -1) {
                throw new RuntimeException("Content mismatch: actual stream has more data than expected " + size);
            }
        }
    }
}
