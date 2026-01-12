package de.sty.fileserv.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Provides the project name and version loaded from version.properties.
 */
public final class FileServVersion {
    public static final String NAME;
    public static final String VERSION;
    private static final Logger LOG = LoggerFactory.getLogger(FileServVersion.class);

    static {
        ResourceBundle rb = ResourceBundle.getBundle("version");
        Objects.requireNonNull(rb, "ResourceBundle cannot be null");
        try {
            NAME = rb.getString("name");
            VERSION = rb.getString("version");
        } catch (Exception e) {
            LOG.error("Failed to load version info from version.properties", e);
            throw new RuntimeException(e);
        }
    }

    private FileServVersion() {
    }

}
