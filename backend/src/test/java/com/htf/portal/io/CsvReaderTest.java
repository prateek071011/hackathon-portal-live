package com.htf.portal.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.htf.portal.exception.CsvImportException;
import com.htf.portal.model.Hackathon;

/**
 * Tests the CSV import reading: good lines become hackathons, bad lines are reported (not fatal),
 * quoted values may contain commas, and a wrong header stops the import.
 */
class CsvReaderTest {

    private static BufferedReader text(String... lines) {
        return new BufferedReader(new StringReader(String.join("\n", lines)));
    }

    @Test
    void goodLinesAreReadAndBadLinesAreReported() throws Exception {
        CsvReader<Hackathon> reader = new CsvReader<>(new HackathonCsvParser());
        List<Hackathon> rows = reader.readAll(text(
                HackathonCsvParser.HEADER,
                "\"Hack, Build, Ship\",Tech Club,\"Say \"\"hi\"\"\",2027-01-20,2027-01-21,2027-01-10,2,4",
                "Bad Date,Club,,2027-13-01,2027-01-21,2027-01-10,2,4",
                "",
                "Min Over Max,Club,,2027-01-20,2027-01-21,2027-01-10,5,3",
                "Too Few,Club,2027-01-20"));

        assertEquals(1, rows.size());
        assertEquals("Hack, Build, Ship", rows.get(0).getTitle());
        assertEquals("Say \"hi\"", rows.get(0).getDescription());

        List<String> errors = reader.getErrors();
        assertEquals(3, errors.size());
        assertTrue(errors.get(0).startsWith("Line 3: startDate"));
        assertTrue(errors.get(1).startsWith("Line 5: Minimum team size"));
        assertTrue(errors.get(2).startsWith("Line 6: expected 8 values"));
    }

    @Test
    void wrongHeaderStopsTheImport() {
        CsvReader<Hackathon> reader = new CsvReader<>(new HackathonCsvParser());
        assertThrows(CsvImportException.class, () -> reader.readAll(text("name,date", "x,y")));
    }
}
