package de.sty.fileserv.test.webdav.utils;

import java.io.InputStream;
import java.util.Random;

/**
 * An InputStream that generates deterministic pseudo-random data.
 */
public class DeterministicInputStream extends InputStream {
    private final long size;
    private final long seed;
    @SuppressWarnings("FieldNotUsedInToString")
    private final Random random;
    private long bytesRead = 0;

    public DeterministicInputStream(long size, long seed) {
        this.size = size;
        this.seed = seed;
        this.random = new Random(seed);
    }

    @Override
    public int read() {
        if (bytesRead >= size) {
            return -1;
        }
        bytesRead++;
        return random.nextInt(256);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (bytesRead >= size) {
            return -1;
        }
        long remaining = size - bytesRead;
        int toRead = (int) Math.min(len, remaining);
        byte[] temp = new byte[toRead];
        random.nextBytes(temp);
        System.arraycopy(temp, 0, b, off, toRead);
        bytesRead += toRead;
        return toRead;
    }

    @Override
    public long skip(long n) {
        if (n <= 0) return 0;
        long remaining = size - bytesRead;
        long toSkip = Math.min(n, remaining);
        // Random doesn't have a skip, so we have to generate bytes to maintain state
        // but for performance we might want to reconsider this if skip is heavily used.
        // For our tests, skip is likely not used much.
        byte[] dump = new byte[(int) Math.min(toSkip, 8192)];
        long skipped = 0;
        while (skipped < toSkip) {
            int r = read(dump, 0, (int) Math.min(toSkip - skipped, dump.length));
            if (r == -1) break;
            skipped += r;
        }
        return skipped;
    }

    @Override
    public int available() {
        long remaining = size - bytesRead;
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    @Override
    public String toString() {
        return "DeterministicInputStream{" + "size=" + size + ", seed=" + seed + ", bytesRead=" + bytesRead + '}';
    }

}
