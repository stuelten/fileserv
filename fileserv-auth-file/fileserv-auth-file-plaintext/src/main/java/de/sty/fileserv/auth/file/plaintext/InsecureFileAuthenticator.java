package de.sty.fileserv.auth.file.plaintext;

import de.sty.fileserv.core.AbstractFileAuthenticator;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InsecureFileAuthenticator extends AbstractFileAuthenticator {

    public InsecureFileAuthenticator(Path path) {
        super(path);
    }

    @Override
    protected Map<String, String> parseLines(Stream<String> lines) {
        return lines
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split(":", -1))
                .filter(parts -> parts.length >= 2 && !parts[0].isEmpty())
                .collect(Collectors.toUnmodifiableMap(parts -> parts[0], parts -> parts[1], (existing, replacement) -> replacement));
    }

    @Override
    public boolean authenticateCheck(String user, String expectedPass, String password) {
        boolean matches = expectedPass != null && expectedPass.equals(password);
        return matches;
    }

}
