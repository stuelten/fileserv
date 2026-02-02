package de.sty.fileserv.test.hierarchy;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to generate a random directory hierarchy with controlled characteristics
 * for testing file servers and WebDAV implementations.
 * <p>
 * This class is a Java implementation of the bash script {@code fileserv-test-generate-hierarchy}.
 * It uses {@code picocli} for command-line argument parsing and can be compiled to a
 * native executable using GraalVM.
 * </p>
 */
@Command(name = "fileserv-test-generate-hierarchy", mixinStandardHelpOptions = true, version = "1.0",
        description = "Create a test directory hierarchy with a lot of files, directories and data.")
@SuppressWarnings("ClassHasNoToStringMethod")
public class HierarchyGenerator implements Callable<Integer> {

    public static final int OUTPUT_INTERVAL_MS = 2000;
    private final Random random = new Random();

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-s", "--size"}, description = "Total size of all files combined (e.g., 20mb, 500kb). NOTE: This is not size of each file! Default: 2mb", defaultValue = "2mb")
    String sizeStr;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-c", "--count"}, description = "Total number of files and directories to create. Default: 100", defaultValue = "100")
    int count;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-r", "--ratio-dir-to-files"}, description = "Ratio of files to directories (e.g., 12 means ~1 dir per 12 files). Default: 10", defaultValue = "10")
    int ratio;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-d", "--depth"}, description = "Maximum depth of the directory tree. Default: 4", defaultValue = "4")
    int depth;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Parameters(index = "0", description = "Target directory")
    Path targetDir;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-p", "--prefix"}, description = "Prefix to use for file and dir names. Without a prefix given, 'test' and a timestamp will be used.")
    String prefix;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-q", "--quiet"}, description = "Minimize output for successful execution.")
    boolean quiet = false;
    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-v", "--verbose"}, description = "Show more detailed output.")
    boolean verbose = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        long millisStart = System.currentTimeMillis();
        long nextOutput = System.currentTimeMillis() + OUTPUT_INTERVAL_MS;

        long sizeBytes = parseSize(sizeStr);
        if (sizeBytes < 0 || count <= 0 || ratio < 0 || depth < 0) {
            error("ERROR: Cannot generate hierarchy with negative size, zero or negative count, negative ratio, or negative depth.");
            CommandLine.usage(this, System.err);
            return 1;
        }

        if (prefix == null || prefix.isBlank()) {
            prefix = "Test_" + System.currentTimeMillis() + "_";
        } else {
            prefix = prefix + "_";
        }

        int numDirs = count / (ratio + 1);
        if (numDirs < 1) numDirs = 1;
        int numFiles = count - numDirs;
        if (numFiles < 0) {
            numFiles = 0;
            numDirs = count;
        }

        if (numFiles == 0 && sizeBytes > 0) {
            // If we have size but no files, ensure at least one file if count allows,
            // or just adjust to have at least one file if count > 0.
            numFiles = 1;
            numDirs = count - 1;
            if (numDirs < 1) numDirs = 1; // Always at least targetDir
        }

        log(!quiet, "Generating %d files and %d directories (Total: %d)".formatted(numFiles, numDirs, count));
        log(verbose, "INFO: Target size: %d bytes, Max depth: %d".formatted(sizeBytes, depth));

        Files.createDirectories(targetDir);

        List<Path> createdDirs = new ArrayList<>();
        createdDirs.add(targetDir);

        int dirsToCreate = numDirs - 1;
        log(verbose, "INFO: Building the directory structure for %d directories...".formatted(dirsToCreate));

        Path baseDir = targetDir;
        Path lastDir = baseDir;
        for (int i = 0; i < dirsToCreate; i++) {
            Path parent = depth <= 1 || i % depth == 0 ? baseDir : lastDir;
            Path dir = parent.resolve(prefix + i);
            Files.createDirectories(dir);
            createdDirs.add(dir);
            lastDir = dir;

            if (nextOutput < System.currentTimeMillis()) {
                if (verbose) {
                    log(verbose, "INFO: Created %d directories...".formatted(i));
                }
                nextOutput = System.currentTimeMillis() + OUTPUT_INTERVAL_MS;
            }
        }
        log(verbose, "INFO: Creating %d directories finished.".formatted(dirsToCreate));

        // Step 2: Distribute files and data
        log(verbose, "INFO: Creating %d files...".formatted(numFiles));
        long remainingSizeBytes = sizeBytes;
        for (int i = 0; i < numFiles; i++) {
            Path parent = createdDirs.get(random.nextInt(createdDirs.size()));
            Path fileName = parent.resolve(prefix + i + ".bin");

            long size;
            if (i == numFiles - 1) {
                size = remainingSizeBytes;
            } else {
                long avgFileSize = remainingSizeBytes / (numFiles - i);
                long var = Math.max(1, avgFileSize / 2);
                size = avgFileSize - var + random.nextLong(2 * var);
                if (size > remainingSizeBytes) size = remainingSizeBytes;
            }

            createRandomFile(fileName, size);
            if (nextOutput < System.currentTimeMillis()) {
                log(verbose, "INFO: Created %d files, %d bytes remaining...".formatted(i + 1, remainingSizeBytes));
                nextOutput = System.currentTimeMillis() + OUTPUT_INTERVAL_MS;
            }

            remainingSizeBytes -= size;
        }
        log(verbose, "INFO: Creating %d files finished.".formatted(numFiles));

        log(verbose, "INFO: Generation complete in %d ms.".formatted(System.currentTimeMillis() - millisStart));
        log(!quiet, "Generation complete.");

        return 0;
    }

    private void createRandomFile(Path path, long size) throws IOException {
        if (size <= 0) {
            Files.createFile(path);
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw")) {
            raf.setLength(size);
            // Optionally fill with random data.
            // For large files, filling with random data can be slow.
            // Let's at least write some random bytes if it's small and then just seek and write one byte at the end.
            byte[] buffer = new byte[(int) Math.min(size, 1024)];
            random.nextBytes(buffer);
            raf.write(buffer);
            if (size > buffer.length) {
                raf.seek(size - 1);
                raf.write(255);
            }
        }
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    long parseSize(String sizeMaybeWithUnit) {
        long result = -1;
        try {
            Pattern pattern = Pattern.compile("^(\\d+)([kK][bB]|[mM][bB]|[gG][bB])?$", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sizeMaybeWithUnit);
            if (matcher.matches()) {
                long val = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2);
                if (unit == null) {
                    // Default MB
                    result = val * 1024 * 1024;
                } else {
                    result = switch (unit.toLowerCase()) {
                        case "kb" -> val * 1024;
                        case "mb" -> val * 1024 * 1024;
                        case "gb" -> val * 1024 * 1024 * 1024;
                        default -> val * 1024 * 1024;
                    };
                }
            } else {
                result = Long.parseLong(sizeMaybeWithUnit) * 1024 * 1024;
            }
        } catch (NumberFormatException e) {
            error("ERROR: Cannot parse size: " + sizeMaybeWithUnit);
        }
        return result;
    }

    private void log(boolean print, String msg) {
        if (print) {
            if (spec != null) {
                spec.commandLine().getOut().println(msg);
            } else {
                System.out.println(msg);
            }
        }
    }

    private void error(String msg) {
        if (spec != null) {
            spec.commandLine().getErr().println(msg);
        } else {
            System.err.println(msg);
        }
    }

}
