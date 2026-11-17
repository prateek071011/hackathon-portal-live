package com.htf.portal.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.htf.portal.config.PortalConfig;
import com.htf.portal.model.RegistrationSnapshot;

/**
 * Serialization round trip: write a snapshot to a .ser file, read it back, and check that every field
 * survived except the transient one. Also checks the CSV escaping used by the export.
 */
class SnapshotStorageTest {

    @TempDir
    Path tempDir;   // JUnit makes an empty folder for this test and deletes it afterwards

    @Test
    void snapshotSurvivesTheFileExceptTheTransientField() throws Exception {
        PortalConfig config = mock(PortalConfig.class);
        when(config.getSnapshotDir()).thenReturn(tempDir);
        SnapshotStorage storage = new SnapshotStorage(config);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 11, 17, 10, 30);

        String fileName = storage.save(new RegistrationSnapshot("REG-2026-000007", "Byte Me", "CodeSprint",
                "Greenfield", List.of("Asha", "Ravi"), approvedAt, "asha@example.com"));
        RegistrationSnapshot back = storage.read("REG-2026-000007");

        assertTrue(Files.exists(tempDir.resolve(fileName)));
        assertEquals("Byte Me", back.getTeamName());
        assertEquals(List.of("Asha", "Ravi"), back.getMemberNames());
        assertEquals(approvedAt, back.getApprovedAt());
        assertNull(back.getLeaderEmail());   // transient: never written, so null after reading
    }

    @Test
    void csvValuesAreQuotedWhenNeeded() {
        assertEquals("plain", CsvWriter.escape("plain"));
        assertEquals("\"Hack, Build\"", CsvWriter.escape("Hack, Build"));
        assertEquals("\"Say \"\"hi\"\"\"", CsvWriter.escape("Say \"hi\""));
        assertEquals("'=SUM(A1)", CsvWriter.escape("=SUM(A1)"));   // no formulas in Excel
    }
}
