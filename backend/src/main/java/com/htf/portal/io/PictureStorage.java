package com.htf.portal.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.htf.portal.config.PortalConfig;
import com.htf.portal.exception.BusinessRuleException;

/**
 * Saves and reads profile pictures with byte streams (BufferedInputStream / BufferedOutputStream).
 * Files get random names and live in the upload folder from portal.properties, which the web server
 * never serves directly: pictures are only sent through our own endpoint.
 */
@Component
public class PictureStorage {

    private static final Logger log = LoggerFactory.getLogger(PictureStorage.class);
    private static final int BUFFER_SIZE = 8192;
    private static final int HEADER_SIZE = 8;

    private final PortalConfig config;

    public PictureStorage(PortalConfig config) {
        this.config = config;
    }

    /** Checks the upload, saves it under a new random name and returns that name. */
    public String save(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessRuleException("Please choose a picture to upload");
        }
        long maxBytes = config.getMaxUploadBytes();
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleException("Picture is too large (max " + (maxBytes / (1024 * 1024)) + " MB)");
        }
        String declaredType = file.getContentType();
        if (declaredType == null || !config.getAllowedPictureTypes().contains(declaredType)) {
            throw new BusinessRuleException("Only JPEG or PNG pictures are allowed");
        }

        try (BufferedInputStream in = new BufferedInputStream(file.getInputStream())) {
            // The declared type is only what the browser claims. We also look at the first bytes,
            // which every real JPEG / PNG starts with. mark() + reset() let us peek, then rewind.
            in.mark(HEADER_SIZE);
            byte[] header = in.readNBytes(HEADER_SIZE);
            in.reset();

            String extension = detectExtension(header);
            if (extension == null) {
                throw new BusinessRuleException("The file is not a real JPEG or PNG image");
            }

            // A random name means users can never pick or guess file names on our disk.
            String fileName = UUID.randomUUID() + "." + extension;
            long bytesWritten = writeToFile(in, resolveInUploadDir(fileName));
            log.info("Saved picture {} ({} bytes)", fileName, bytesWritten);
            return fileName;
        }
    }

    /** Reads a saved picture fully into memory (pictures are at most 2 MB). */
    public byte[] read(String fileName) throws IOException {
        Path path = resolveInUploadDir(fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            copy(in, out);
        }
        return out.toByteArray();
    }

    /** Deletes an old picture. A failure is only logged: a leftover file does no harm. */
    public void delete(String fileName) {
        try {
            Files.deleteIfExists(resolveInUploadDir(fileName));
        } catch (IOException e) {
            log.warn("Could not delete old picture {}", fileName, e);
        }
    }

    public static String contentTypeFor(String fileName) {
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }

    private long writeToFile(InputStream in, Path target) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
            return copy(in, out);
        }
    }

    // Copies in chunks of 8 KB instead of byte by byte; the buffered streams make each read/write cheap.
    private long copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int count = in.read(buffer);
        while (count != -1) {
            out.write(buffer, 0, count);
            total += count;
            count = in.read(buffer);
        }
        return total;
    }

    // Our own names never contain "..", but checking anyway means a bad value in the
    // database can never make us read or write a file outside the upload folder.
    private Path resolveInUploadDir(String fileName) {
        Path folder = config.getUploadDir().toAbsolutePath().normalize();
        Path file = folder.resolve(fileName).normalize();
        if (!file.startsWith(folder)) {
            throw new IllegalArgumentException("Invalid picture file name: " + fileName);
        }
        return file;
    }

    // JPEG files start with FF D8 FF. PNG files start with 89 'P' 'N' 'G'.
    private static String detectExtension(byte[] header) {
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (header.length >= 4
                && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return "png";
        }
        return null;
    }
}
