package de.sty.fileserv.test.hierarchy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HierarchyGeneratorTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    Path tempDir;

    private Path testDir;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        String testName = testInfo.getTestMethod()
                .map(method -> "_" + method.getName())
                .orElse("");
        testDir = tempDir.resolve("test_data_java" + testName);
    }

    @Test
    void testMinimalSettings() throws Exception {
        String[] args = {"-q", "--size", "10kb", "--count", "1", "--ratio-dir-to-files", "0", "--depth", "1", testDir.toString()};

        HierarchyGenerator generator = new HierarchyGenerator();
        int exitCode = new CommandLine(generator).execute(args);

        assertThat(exitCode).isEqualTo(0);
        assertThat(testDir).isDirectory();

        // With count 1 and size > 0, implementation ensures 1 file and 1 dir (targetDir itself)
        try (Stream<Path> walk = Files.walk(testDir)) {
            long totalItems = walk.count();
            // 2 items because it counts targetDir + 1 file.
            assertThat(totalItems).isEqualTo(2);
        }

        // Check size approx 10KB
        long size = getTotalSize(testDir);
        assertThat(size).isGreaterThanOrEqualTo(10 * 1024);
    }

    @Test
    void testDefaultUsageMinimalMB() throws Exception {
        String[] args = {"-q", "--size", "1", "--count", "10", "--ratio-dir-to-files", "5", "--depth", "2", testDir.toString()};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isEqualTo(0);
        assertThat(testDir).isDirectory();

        try (Stream<Path> walk = Files.walk(testDir)) {
            long totalItems = walk.count();
            assertThat(totalItems).isEqualTo(10);
        }
    }

    @Test
    void testHierarchyDepthConstraint() throws Exception {
        int count = 20;
        int depth = 3;

        String[] args = {"-q", "--size", "1", "--count", "" + count, "--ratio-dir-to-files", "1", "--depth", "" + depth, testDir.toString()};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isEqualTo(0);

        try (Stream<Path> walk = Files.walk(testDir)) {
            walk.forEach(path -> {
                // -1 --> do not count testDir for hierarchy
                int actualDepth = testDir.relativize(path).getNameCount() - 1;
                assertThat(actualDepth).isLessThanOrEqualTo(depth);
            });
        }

        // Exactly count files and dirs?
        long[] filesAndDirs = getNumberOfFilesAndDirs(testDir);
        long countTotal = filesAndDirs[0] + filesAndDirs[1];
        assertThat(countTotal).isEqualTo(count);
    }

    @Test
    void testHierarchyDepthConstraintDeep() throws Exception {
        // Arguments
        // more than 4kb, a lot of files with a deep hierarchy
        int count = 1000;
        int depth = 12;
        String[] args = {"-q", "--size", "4100", "--count", "" + count, "--ratio-dir-to-files", "10", "--depth", "" + depth, testDir.toString()};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isEqualTo(0);

        try (Stream<Path> walk = Files.walk(testDir)) {
            walk.forEach(path -> {
                int actualDepth = testDir.relativize(path).getNameCount() - 1;
                assertThat(actualDepth).isLessThanOrEqualTo(depth);
            });
        }

        // Exactly count files and dirs?
        long[] filesAndDirs = getNumberOfFilesAndDirs(testDir);
        long countTotal = filesAndDirs[0] + filesAndDirs[1];
        assertThat(countTotal).isEqualTo(count);
    }

    @Test
    void testHierarchyDepthConstraintDeep2() throws Exception {
        // Arguments
        // small files, a lot of files with a deep hierarchy
        int count = 10000;
        int depth = 33;
        String[] args = {"-q", "--size", "1", "--count", "" + count, "--ratio-dir-to-files", "10", "--depth", "" + depth, testDir.toString()};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isEqualTo(0);

        try (Stream<Path> walk = Files.walk(testDir)) {
            walk.forEach(path -> {
                int actualDepth = testDir.relativize(path).getNameCount() - 1;
                assertThat(actualDepth).isLessThanOrEqualTo(depth);
            });
        }

        // Exactly count files and dirs?
        long[] filesAndDirs = getNumberOfFilesAndDirs(testDir);
        long countTotal = filesAndDirs[0] + filesAndDirs[1];
        assertThat(countTotal).isEqualTo(count);
    }

    @Test
    void testHierarchyDepthConstraintBreath() throws Exception {
        // Arguments
        // small files, a lot of files with a deep hierarchy
        int count = 10000;
        int depth = 2;
        String[] args = {"-q", "--size", "1", "--count", "" + count, "--depth", "" + depth, testDir.toString()};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isEqualTo(0);

        try (Stream<Path> walk = Files.walk(testDir)) {
            walk.forEach(path -> {
                int actualDepth = testDir.relativize(path).getNameCount() - 1;
                assertThat(actualDepth).isLessThanOrEqualTo(depth);
            });
        }

        // Exactly count files and dirs?
        long[] filesAndDirs = getNumberOfFilesAndDirs(testDir);
        long countTotal = filesAndDirs[0] + filesAndDirs[1];
        assertThat(countTotal).isEqualTo(count);
    }

    @Test
    void testFailsOnInvalidArguments() {
        String[] args = {"-q", "--size", "0", "--count", "0"};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    void testShortOptions() throws Exception {
        String[] args = {"-q", "-s", "10kb", "-c", "5", "-r", "1", "-d", "2", testDir.toString()};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isEqualTo(0);
        assertThat(testDir).isDirectory();

        try (Stream<Path> walk = Files.walk(testDir)) {
            long totalItems = walk.count();
            assertThat(totalItems).isEqualTo(5);
        }
    }

    @Test
    void testParseSize() {
        HierarchyGenerator generator = new HierarchyGenerator();

        // Default MB
        assertThat(generator.parseSize("10")).isEqualTo(10L * 1024 * 1024);

        // KB
        assertThat(generator.parseSize("10kb")).isEqualTo(10L * 1024);
        assertThat(generator.parseSize("10KB")).isEqualTo(10L * 1024);
        assertThat(generator.parseSize("10Kb")).isEqualTo(10L * 1024);

        // MB
        assertThat(generator.parseSize("10mb")).isEqualTo(10L * 1024 * 1024);
        assertThat(generator.parseSize("10MB")).isEqualTo(10L * 1024 * 1024);

        // GB
        assertThat(generator.parseSize("2gb")).isEqualTo(2L * 1024 * 1024 * 1024);
        assertThat(generator.parseSize("2GB")).isEqualTo(2L * 1024 * 1024 * 1024);

    }

    @Test
    void testParseSizeInvalidInputs() {
        HierarchyGenerator generator = new HierarchyGenerator();

        // Note: parseSize calls error() which might fail if spec is not initialized,
        // Actually, for strings that don't match the pattern and aren't parseable as Long, it returns -1.

        PrintStream originalErr = System.err;
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(errContent));
            assertThat(generator.parseSize("invalid")).isEqualTo(-1L);
            assertThat(errContent.toString()).contains("Cannot parse size");
            errContent.reset();

            assertThat(generator.parseSize("10tb")).isEqualTo(-1L);
            assertThat(errContent.toString()).contains("Cannot parse size");
            errContent.reset();

            assertThat(generator.parseSize("999999999999999999999999999")).isEqualTo(-1L);
            assertThat(errContent.toString()).contains("Size value too large");
            errContent.reset();
        } finally {
            System.setErr(originalErr);
        }

    }

    private long getTotalSize(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
        }
    }

    /**
     * Counts files and directories recursively under a given path
     *
     * @return array with [0] = number of files, [1] = number of directories
     */
    private long[] getNumberOfFilesAndDirs(Path path) throws IOException {
        long[] ret = new long[2];
        try (Stream<Path> walk = Files.walk(path)) {
            walk.forEach(relPath -> {
                if (Files.isDirectory(relPath)) {
                    ret[1] = ret[1] + 1;
                } else if (Files.isRegularFile(relPath)) {
                    ret[0] = ret[0] + 1;
                } else {
                    throw new IllegalArgumentException("Unsupported file type: " + relPath);
                }
            });
        }
        return ret;
    }

}
