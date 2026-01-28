package de.sty.fileserv.auth.file.plaintext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InsecureFileAuthenticatorTest {

    @TempDir
    Path tempDir;

    @Test
    void authenticatesCorrectlyFromFile() throws IOException {
        Path authFile = tempDir.resolve("auth.txt");
        Files.writeString(authFile, """
                # Comment
                alice:secret
                bob:pass123
                
                admin:admin:password
                """);

        InsecureFileAuthenticator auth = new InsecureFileAuthenticator(authFile);

        assertThat(auth.authenticate("alice", "secret")).isTrue();
        assertThat(auth.authenticate("bob", "pass123")).isTrue();
        // The second field is password, third field not used
        assertThat(auth.authenticate("admin", "admin")).isTrue();

        assertThat(auth.authenticate("alice", "wrong")).isFalse();
        assertThat(auth.authenticate("unknown", "secret")).isFalse();
    }

    @Test
    void throwsExceptionOnNonExistentFile() {
        Path missingFile = tempDir.resolve("missing.txt");
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(errContent));
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                    new InsecureFileAuthenticator(missingFile)
            );
            assertThat(errContent.toString()).contains("Authentication file does not exist");
            assertThat(errContent.toString()).contains(missingFile.toString());
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void refreshesFromFileWhenModified() throws IOException, InterruptedException {
        Path authFile = tempDir.resolve("auth.txt");
        Files.writeString(authFile, "alice:secret\n");

        InsecureFileAuthenticator auth = new InsecureFileAuthenticator(authFile);
        assertThat(auth.authenticate("alice", "secret")).isTrue();
        assertThat(auth.authenticate("bob", "pass")).isFalse();

        // Wait a bit to ensure timestamp changes if FS resolution is low
        Thread.sleep(100);

        // Update file
        Files.writeString(authFile, "alice:secret\nbob:pass\n");
        // Ensure the timestamp is definitely different
        Files.setLastModifiedTime(authFile, FileTime.from(Instant.now().plusSeconds(1)));

        // Should now authenticate bob
        assertThat(auth.authenticate("bob", "pass")).isTrue();
    }

    @Test
    void handlesConcurrentAccessAndReloads() throws IOException, InterruptedException {
        Path authFile = tempDir.resolve("auth.txt");
        Files.writeString(authFile, "user1:pass1\n");

        InsecureFileAuthenticator auth = new InsecureFileAuthenticator(authFile);

        int threadCount = 10;
        int iterations = 1000;
        @SuppressWarnings("resource")
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        if (auth.authenticate("user1", "pass1")) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        // Simulate file updates during concurrent access
        for (int i = 0; i < 5; i++) {
            Thread.sleep(100);
            Files.writeString(authFile, "user1:pass1\nuser" + i + ":pass" + i + "\n");
            Files.setLastModifiedTime(authFile, FileTime.from(Instant.now().plusSeconds(10 + i)));
        }

        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount * iterations);
    }

    @Test
    void toStringContainsTimestamp() throws IOException {
        Path authFile = tempDir.resolve("auth_toString.txt");
        Files.writeString(authFile, "alice:secret\n");

        InsecureFileAuthenticator auth = new InsecureFileAuthenticator(authFile);
        String toString = auth.toString();

        assertThat(toString).contains("path=" + authFile);
        assertThat(toString).contains("lastModified=");
        assertThat(toString).contains("users=[alice]");
    }
}
