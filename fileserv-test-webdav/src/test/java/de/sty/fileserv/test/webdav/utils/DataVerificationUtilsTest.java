package de.sty.fileserv.test.webdav.utils;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataVerificationUtilsTest {

    @Test
    void generateDeterministicDataReturnsExpectedData() {
        int size = 1024;
        long seed = 123L;
        byte[] data = DataVerificationUtils.generateDeterministicData(size, seed);

        assertThat(data).hasSize(size);

        byte[] expected = new byte[size];
        new Random(seed).nextBytes(expected);
        assertThat(data).isEqualTo(expected);
    }

    @Test
    void verifyContentByteArraysSuccess() {
        byte[] data = {1, 2, 3};
        DataVerificationUtils.verifyContent(data, data.clone());
    }

    @Test
    void verifyContentByteArraysThrowsOnMismatch() {
        byte[] expected = {1, 2, 3};
        byte[] actual = {1, 2, 4};
        assertThatThrownBy(() -> DataVerificationUtils.verifyContent(expected, actual))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Content mismatch");
    }

    @Test
    void verifyContentInputStreamSuccess() throws IOException {
        long size = 1024;
        long seed = 456L;
        try (InputStream is = new DeterministicInputStream(size, seed)) {
            DataVerificationUtils.verifyContent(size, seed, is);
        }
    }

    @Test
    void verifyContentInputStreamThrowsOnContentMismatch() throws IOException {
        long size = 10;
        long seed = 789L;
        byte[] manipulatedData = DataVerificationUtils.generateDeterministicData((int) size, seed);
        manipulatedData[5] ^= (byte) 0xFF; // Flip bits

        try (InputStream is = new ByteArrayInputStream(manipulatedData)) {
            assertThatThrownBy(() -> DataVerificationUtils.verifyContent(size, seed, is))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Content mismatch at offset around");
        }
    }

    @Test
    void verifyContentInputStreamThrowsOnPrematureEof() throws IOException {
        long size = 100;
        long seed = 111L;
        byte[] shorterData = DataVerificationUtils.generateDeterministicData(50, seed);

        try (InputStream is = new ByteArrayInputStream(shorterData)) {
            assertThatThrownBy(() -> DataVerificationUtils.verifyContent(size, seed, is))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("actual stream ended prematurely");
        }
    }

    @Test
    void verifyContentInputStreamThrowsOnExtraData() throws IOException {
        long size = 50;
        long seed = 222L;
        byte[] longerData = DataVerificationUtils.generateDeterministicData(60, seed);

        try (InputStream is = new ByteArrayInputStream(longerData)) {
            assertThatThrownBy(() -> DataVerificationUtils.verifyContent(size, seed, is))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("actual stream has more data than expected");
        }
    }

}
