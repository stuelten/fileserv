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
public class HierarchyGenerator implements Callable<Integer> {

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-s", "--size"}, description = "Total size of all files (e.g., 20mb, 500kb). Default: 2mb", defaultValue = "2mb")
    private String sizeStr;

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-c", "--count"}, description = "Total number of files and directories to create. Default: 100", defaultValue = "100")
    private int count;

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-r", "--ratio-dir-to-files"}, description = "Ratio of files to directories (e.g., 12 means ~1 dir per 12 files). Default: 10", defaultValue = "10")
    private int ratio;

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Option(names = {"-d", "--depth"}, description = "Maximum depth of the directory tree. Default: 4", defaultValue = "4")
    private int depth;

    // Set by PicoCLI
    @SuppressWarnings("unused")
    @Parameters(index = "0", description = "Target directory")
    private Path targetDir;

    private final Random random = new Random();

    @Override
    public Integer call() throws Exception {
        long sizeBytes = parseSize(sizeStr);
        if (sizeBytes < 0 || count <= 0 || ratio < 0 || depth < 0) {
            CommandLine.usage(this, System.err);
            return 1;
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

        System.out.printf("Generating %d files and %d directories (Total: %d)%n", numFiles, numDirs, count);
        System.out.printf("Target size: %d bytes, Max depth: %d%n", sizeBytes, depth);

        Files.createDirectories(targetDir);

        List<Path> createdDirs = new ArrayList<>();
        createdDirs.add(targetDir);

        int dirsToCreate = numDirs - 1;

        // Step 1: Build the directory structure
        List<Path> currentDepthDirs = new ArrayList<>();
        currentDepthDirs.add(targetDir);

        for (int d = 1; d <= depth; d++) {
            if (dirsToCreate <= 0) break;

            List<Path> nextDepthDirs = new ArrayList<>();
            for (Path parent : currentDepthDirs) {
                if (dirsToCreate <= 0) break;

                int maxSub = Math.min(dirsToCreate, 5);
                int subCount = random.nextInt(maxSub) + 1;

                if (d == depth) subCount = 0;

                for (int i = 0; i < subCount; i++) {
                    Path dirName = parent.resolve("dir_" + d + "_" + i + "_" + random.nextInt(10000));
                    Files.createDirectories(dirName);
                    nextDepthDirs.add(dirName);
                    createdDirs.add(dirName);
                    dirsToCreate--;
                }
            }
            if (nextDepthDirs.isEmpty()) break;
            currentDepthDirs = nextDepthDirs;
        }

        // Fill the remaining directories randomly
        int failSafe = 0;
        while (dirsToCreate > 0 && failSafe < 1000) {
            Path parent = createdDirs.get(random.nextInt(createdDirs.size()));
            int currentDepth = targetDir.relativize(parent).getNameCount();
            if (parent.equals(targetDir)) currentDepth = 0;

            if (currentDepth < depth) {
                Path subDirName = parent.resolve("extra_dir_" + dirsToCreate);
                if (!Files.exists(subDirName)) {
                    Files.createDirectories(subDirName);
                    createdDirs.add(subDirName);
                    dirsToCreate--;
                }
            } else {
                failSafe++;
            }
        }

        // Step 2: Distribute files and data
        long remainingSizeBytes = sizeBytes;
        for (int i = 0; i < numFiles; i++) {
            Path parent = createdDirs.get(random.nextInt(createdDirs.size()));
            Path fileName = parent.resolve("file_" + i + ".bin");

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
            remainingSizeBytes -= size;
        }

        System.out.println("Generation complete.");
        return 0;
    }

    private void createRandomFile(Path path, long size) throws IOException {
        if (size <= 0) {
            Files.createFile(path);
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw")) {
            raf.setLength(size);
            // Optionally fill with random data. The bash script uses /dev/urandom.
            // For large files, filling with random data can be slow.
            // Let's at least write some random bytes if it's small, or just seek and write one byte at the end.
            byte[] buffer = new byte[(int) Math.min(size, 1024)];
            random.nextBytes(buffer);
            raf.write(buffer);
            if (size > buffer.length) {
                raf.seek(size - 1);
                raf.write(0);
            }
        }
    }

    private long parseSize(String sizeStr) {
        Pattern pattern = Pattern.compile("^(\\d+)([kK][bB]|[mM][bB]|[gG][bB])?$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sizeStr);
        if (matcher.matches()) {
            long val = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            if (unit == null) return val * 1024 * 1024; // Default MB
            return switch (unit.toLowerCase()) {
                case "kb" -> val * 1024;
                //case "mb" -> val * 1024 * 1024;
                case "gb" -> val * 1024 * 1024 * 1024;
                default -> val * 1024 * 1024;
            };
        }
        try {
            return Long.parseLong(sizeStr) * 1024 * 1024;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new HierarchyGenerator()).execute(args);
        System.exit(exitCode);
    }
}
