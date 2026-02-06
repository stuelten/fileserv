package de.sty.fileserv.test.webdav.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicInputStreamTest {

    @Test
    void singleByteReadProducesDeterministicSequenceUntilEof() throws IOException {
        long size = 1_000L;
        long seed = 42L;

        try (DeterministicInputStream dis = new DeterministicInputStream(size, seed)) {
            Random expected = new Random(seed);
            for (int i = 0; i < size; i++) {
                int actual = dis.read();
                int exp = expected.nextInt(256);
                assertThat(actual).isBetween(0, 255);
                assertThat(actual).isEqualTo(exp);
            }
            assertThat(dis.read()).isEqualTo(-1);
        }
    }

    @Test
    void bulkReadRespectsBoundariesAndReturnsMinusOneAfterEof() throws IOException {
        long size = 1_500L;
        long seed = 123L;
        byte[] buffer = new byte[512];
        long total = 0;

        try (DeterministicInputStream dis = new DeterministicInputStream(size, seed)) {
            Random expected = new Random(seed);
            while (true) {
                int n = dis.read(buffer, 0, buffer.length);
                if (n == -1) break;
                assertThat(n).isBetween(1, buffer.length);

                // Verify chunk content matches java.util.Random
                byte[] exp = new byte[n];
                expected.nextBytes(exp);
                for (int i = 0; i < n; i++) {
                    assertThat(Byte.toUnsignedInt(buffer[i]))
                            .as("byte @ %s", total + i)
                            .isEqualTo(Byte.toUnsignedInt(exp[i]));
                }
                total += n;
            }
            assertThat(total).isEqualTo(size);
            assertThat(dis.read(buffer, 0, buffer.length)).isEqualTo(-1);
        }
    }

    @Test
    void skipAdvancesStreamStateCorrectly() throws IOException {
        long size = 5_000L;
        long seed = 7L;

        byte[] aNext = new byte[256];
        byte[] bNext = new byte[256];

        try (DeterministicInputStream a = new DeterministicInputStream(size, seed);
             DeterministicInputStream b = new DeterministicInputStream(size, seed)) {

            long skipped = a.skip(1_234);
            assertThat(skipped).isEqualTo(1_234);

            // Manually advance b by reading and discarding
            long discarded = 0;
            byte[] dump = new byte[8192];
            while (discarded < 1_234) {
                int n = b.read(dump, 0, (int) Math.min(dump.length, 1_234 - discarded));
                assertThat(n).isGreaterThan(0);
                discarded += n;
            }

            int n1 = a.read(aNext);
            int n2 = b.read(bNext);
            assertThat(n1).isEqualTo(aNext.length);
            assertThat(n2).isEqualTo(bNext.length);
            Assertions.assertThat(aNext).containsExactly(bNext);
        }

        // Skipping beyond EOF
        try (DeterministicInputStream c = new DeterministicInputStream(100, seed)) {
            long s = c.skip(1_000);
            assertThat(s).isEqualTo(100);
            assertThat(c.read()).isEqualTo(-1);
        }
    }

    @Test
    void availableReflectsRemainingAndIsCappedAtIntegerMax() throws IOException {
        long size = (long) Integer.MAX_VALUE + 1_000L;
        long seed = 1L;
        try (DeterministicInputStream dis = new DeterministicInputStream(size, seed)) {
            // Initially capped to Integer.MAX_VALUE
            assertThat(dis.available()).isEqualTo(Integer.MAX_VALUE);

            // Read a little; still capped
            byte[] buf = new byte[100];
            int n = dis.read(buf);
            assertThat(n).isEqualTo(100);
            assertThat(dis.available()).isEqualTo(Integer.MAX_VALUE);

            // Reduce remaining to below Integer.MAX_VALUE and check exact value
            long remaining = size - 100; // after the read above
            long needToSkipToCross = remaining - Integer.MAX_VALUE + 10; // leave 10 fewer than MAX
            long skipped = dis.skip(needToSkipToCross);
            assertThat(skipped).isEqualTo(needToSkipToCross);

            long expectedRemaining = remaining - skipped;
            assertThat(expectedRemaining).isLessThan(Integer.MAX_VALUE);
            assertThat(dis.available()).isEqualTo((int) expectedRemaining);
        }
    }

    @Test
    void zeroSizeStreamImmediateEofAndNoAvailableAndNoSkip() throws IOException {
        try (DeterministicInputStream dis = new DeterministicInputStream(0, 99L)) {
            assertThat(dis.read()).isEqualTo(-1);
            byte[] b = new byte[16];
            assertThat(dis.read(b)).isEqualTo(-1);
            assertThat(dis.available()).isEqualTo(0);
            assertThat(dis.skip(10)).isEqualTo(0);
        }
    }
}
