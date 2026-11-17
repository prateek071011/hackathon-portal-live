package com.htf.portal.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.htf.portal.exception.CsvImportException;

/**
 * Reads a CSV file line by line with a BufferedReader (a character stream) and turns each line into a T
 * using a CsvLineParser<T>. A bad line does not stop the import: its error is collected and reading
 * continues. Only a wrong header line stops everything.
 * Values may be wrapped in double quotes to contain commas: "Hack, Build, Ship". Two quotes ("") mean one quote.
 */
public class CsvReader<T> {

    private static final char BYTE_ORDER_MARK = '﻿';   // Excel puts this invisible character at the start

    private final CsvLineParser<T> parser;
    private final List<String> errors = new ArrayList<>();

    public CsvReader(CsvLineParser<T> parser) {
        this.parser = parser;
    }

    public List<T> readAll(BufferedReader reader) throws IOException, CsvImportException {
        List<T> rows = new ArrayList<>();

        String header = reader.readLine();
        if (header == null) {
            return rows;   // empty file
        }
        checkHeader(header);

        int lineNumber = 1;
        String line = reader.readLine();
        while (line != null) {
            lineNumber++;
            if (!line.isBlank()) {
                try {
                    rows.add(parser.parse(splitLine(line, lineNumber), lineNumber));
                } catch (CsvImportException e) {
                    // Remember the problem and carry on with the next line.
                    errors.add(e.getMessage());
                }
            }
            line = reader.readLine();
        }
        return rows;
    }

    public List<String> getErrors() {
        return errors;
    }

    /** Splits one line at commas, except commas inside "double quotes". */
    public static List<String> splitLine(String line, int lineNumber) throws CsvImportException {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (insideQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');   // "" inside quotes is one real quote character
                    i++;                    // skip the second quote
                } else if (c == '"') {
                    insideQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                insideQuotes = true;
            } else if (c == ',') {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (insideQuotes) {
            throw new CsvImportException(lineNumber, "a quoted value is never closed");
        }
        values.add(current.toString().trim());
        return values;
    }

    private void checkHeader(String header) throws CsvImportException {
        if (!header.isEmpty() && header.charAt(0) == BYTE_ORDER_MARK) {
            header = header.substring(1);
        }
        if (!simplify(header).equals(simplify(parser.header()))) {
            throw new CsvImportException(1, "the first line must be: " + parser.header());
        }
    }

    // Header comparison ignores spaces and upper/lower case.
    private static String simplify(String text) {
        return text.replace(" ", "").toLowerCase(Locale.ROOT);
    }
}
