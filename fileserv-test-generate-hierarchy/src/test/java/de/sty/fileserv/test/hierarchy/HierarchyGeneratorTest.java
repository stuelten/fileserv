package de.sty.fileserv.test.hierarchy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
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
        // Arguments: --size 10kb --count 1 --ratio-dir-to-files 0 --depth 1 "$TEST_DIR"
        String[] args = {"--size", "10kb", "--count", "1", "--ratio-dir-to-files", "0", "--depth", "1", testDir.toString()};

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
        // Arguments: --size 1 --count 10 --ratio-dir-to-files 5 --depth 2 "$TEST_DIR"
        String[] args = {"--size", "1", "--count", "10", "--ratio-dir-to-files", "5", "--depth", "2", testDir.toString()};

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
        // Arguments: --size 1 --count 20 --ratio-dir-to-files 1 --depth 3 "$TEST_DIR"
        int count = 20;
        int depth = 3;

        String[] args = {"--size", "1", "--count", "" + count, "--ratio-dir-to-files", "1", "--depth", "" + depth, testDir.toString()};

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
        String[] args = {"--size", "4100", "--count", "" + count, "--ratio-dir-to-files", "10", "--depth", "" + depth, testDir.toString()};

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
        String[] args = {"--size", "1", "--count", "" + count, "--ratio-dir-to-files", "10", "--depth", "" + depth, testDir.toString()};

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
        String[] args = {"--size", "1", "--count", "" + count, "--depth", "" + depth, testDir.toString()};

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
        // Arguments: --size 0 --count 0
        String[] args = {"--size", "0", "--count", "0"};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    void testShortOptions() throws Exception {
        // Arguments: -s 10kb -c 5 -r 1 -d 2 "$TEST_DIR"
        String[] args = {"-s", "10kb", "-c", "5", "-r", "1", "-d", "2", testDir.toString()};

        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);

        assertThat(exitCode).isEqualTo(0);
        assertThat(testDir).isDirectory();

        try (Stream<Path> walk = Files.walk(testDir)) {
            long totalItems = walk.count();
            assertThat(totalItems).isEqualTo(5);
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
     * Counts files and directories recursively under given path
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
        System.out.print("Found " + ret[0] + " files and " + ret[1] + " directories in " + path);
        return ret;
    }

}
