package com.htf.portal.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.htf.portal.config.PortalConfig;
import com.htf.portal.model.RegistrationSnapshot;

/**
 * Writes a RegistrationSnapshot to data/snapshots/{registrationCode}.ser with ObjectOutputStream (serialization)
 * and reads it back with ObjectInputStream (deserialization).
 */
@Component
public class SnapshotStorage {

    private static final Logger log = LoggerFactory.getLogger(SnapshotStorage.class);

    // Reading a .ser file can create objects of any class named inside it, which is a known security risk.
    // This filter allows only our snapshot class and the plain JDK types it contains; "!*" rejects everything else.
    private static final ObjectInputFilter ONLY_SNAPSHOTS = ObjectInputFilter.Config.createFilter(
            "com.htf.portal.model.RegistrationSnapshot;java.util.ArrayList;java.lang.*;java.time.*;!*");

    private final PortalConfig config;

    public SnapshotStorage(PortalConfig config) {
        this.config = config;
    }

    /** Saves the snapshot and returns the file name, e.g. REG-2026-000003.ser. */
    public String save(RegistrationSnapshot snapshot) throws IOException {
        Path file = fileFor(snapshot.getRegistrationCode());
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeObject(snapshot);   // writes every non-transient field, then the stream closes and flushes
        }
        log.info("Snapshot written: {}", file.toAbsolutePath());
        return file.getFileName().toString();
    }

    public RegistrationSnapshot read(String registrationCode) throws IOException {
        Path file = fileFor(registrationCode);
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            in.setObjectInputFilter(ONLY_SNAPSHOTS);
            return (RegistrationSnapshot) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Snapshot " + registrationCode + " contains an unknown class", e);
        }
    }

    public static String fileNameFor(String registrationCode) {
        return registrationCode + ".ser";
    }

    // The code always looks like REG-2026-000123; checking it means a strange value can never point outside the folder.
    private Path fileFor(String registrationCode) {
        if (registrationCode == null || !registrationCode.matches("REG-\\d{4}-\\d{6}")) {
            throw new IllegalArgumentException("Not a registration code: " + registrationCode);
        }
        return config.getSnapshotDir().resolve(fileNameFor(registrationCode));
    }
}
