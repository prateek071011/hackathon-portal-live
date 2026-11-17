package com.htf.portal.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Our own app settings, read from portal.properties with plain java.util.Properties.
 * Spring's settings (database, logging) stay in application.properties; this file holds
 * values only our code cares about: user-code prefix, upload folder, max upload size, snapshot folder.
 */
@Component
public class PortalConfig {

    private static final Logger log = LoggerFactory.getLogger(PortalConfig.class);
    private static final String FILE_NAME = "portal.properties";

    private final String userCodePrefix;
    private final Path uploadDir;
    private final long maxUploadBytes;
    private final Path snapshotDir;
    private final Set<String> allowedPictureTypes;

    public PortalConfig() {
        Properties props = loadProperties();

        // Every getProperty has a default so the app still starts if a line is missing.
        userCodePrefix = props.getProperty("portal.userCodePrefix", "HTF-");
        uploadDir = Path.of(props.getProperty("portal.uploadDir", "data/uploads"));
        maxUploadBytes = Long.parseLong(props.getProperty("portal.maxUploadBytes", "2097152"));
        snapshotDir = Path.of(props.getProperty("portal.snapshotDir", "data/snapshots"));

        allowedPictureTypes = new HashSet<>();
        String types = props.getProperty("portal.allowedPictureTypes", "image/jpeg,image/png");
        for (String type : types.split(",")) {
            allowedPictureTypes.add(type.trim());
        }

        createFolder(uploadDir);
        createFolder(snapshotDir);
        log.info("Loaded {}: userCodePrefix={}, uploadDir={}, snapshotDir={}, maxUploadBytes={}",
                FILE_NAME, userCodePrefix, uploadDir, snapshotDir, maxUploadBytes);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        // The file sits inside the jar (src/main/resources), so we read it through the class loader.
        try (InputStream in = PortalConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in == null) {
                log.warn("{} not found on the classpath, using default values", FILE_NAME);
                return props;
            }
            props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + FILE_NAME, e);
        }
        return props;
    }

    private void createFolder(Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create folder " + folder.toAbsolutePath(), e);
        }
    }

    public String getUserCodePrefix() {
        return userCodePrefix;
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public Path getSnapshotDir() {
        return snapshotDir;
    }

    public Set<String> getAllowedPictureTypes() {
        // Return a copy so callers cannot change our settings by accident.
        return new HashSet<>(allowedPictureTypes);
    }
}
