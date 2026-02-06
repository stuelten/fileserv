package de.sty.fileserv.test.webdav.scenarios;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class for WebDAV scenario operations.
 */
public final class ScenarioUtils {

    private ScenarioUtils() {
        // Utility class
    }

    /**
     * Encodes a path component for use in a URL, preserving slashes.
     *
     * @param name the path component or relative path
     * @return the URL-encoded path
     */
    public static String encodePath(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    /**
     * Ensures that the given URL ends with a trailing slash.
     *
     * @param url the URL to check
     * @return the URL with a trailing slash
     */
    public static String ensureTrailingSlash(String url) {
        if (url == null) return "/";
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * Creates a Sardine instance for the specified user using the provided credentials.
     *
     * @param user        the username
     * @param credentials the map of usernames to passwords
     * @return a Sardine instance configured for the user
     * @throws IllegalArgumentException if no password is found for the user
     */
    public static Sardine getSardine(String user, Map<String, String> credentials) {
        String pass = credentials.get(user);
        if (pass == null) throw new IllegalArgumentException("No password for user: " + user);
        return SardineFactory.begin(user, pass);
    }

}
