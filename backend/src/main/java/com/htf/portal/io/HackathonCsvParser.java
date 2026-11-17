package com.htf.portal.io;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.htf.portal.exception.CsvImportException;
import com.htf.portal.model.Hackathon;

/**
 * Turns one CSV line into a Hackathon and checks it with the same rules as the admin form
 * (Hackathon.checkRules). Dates must look like 2026-12-31.
 */
public class HackathonCsvParser implements CsvLineParser<Hackathon> {

    public static final String HEADER =
            "title,organizer,description,startDate,endDate,registrationDeadline,minTeamSize,maxTeamSize";
    private static final int COLUMN_COUNT = 8;
    private static final int MAX_TEXT_LENGTH = 150;

    @Override
    public String header() {
        return HEADER;
    }

    @Override
    public Hackathon parse(List<String> values, int lineNumber) throws CsvImportException {
        if (values.size() != COLUMN_COUNT) {
            throw new CsvImportException(lineNumber,
                    "expected " + COLUMN_COUNT + " values but found " + values.size());
        }
        String title = requireText(values.get(0), "title", lineNumber);
        String organizer = requireText(values.get(1), "organizer", lineNumber);
        String description = values.get(2).isEmpty() ? null : values.get(2);
        LocalDate startDate = parseDate(values.get(3), "startDate", lineNumber);
        LocalDate endDate = parseDate(values.get(4), "endDate", lineNumber);
        LocalDate deadline = parseDate(values.get(5), "registrationDeadline", lineNumber);
        int minTeamSize = parseNumber(values.get(6), "minTeamSize", lineNumber);
        int maxTeamSize = parseNumber(values.get(7), "maxTeamSize", lineNumber);

        Hackathon hackathon = new Hackathon(title, organizer, description, startDate, endDate, deadline,
                minTeamSize, maxTeamSize);
        String problem = hackathon.checkRules();
        if (problem != null) {
            throw new CsvImportException(lineNumber, problem);
        }
        return hackathon;
    }

    private static String requireText(String value, String column, int lineNumber) throws CsvImportException {
        if (value.isEmpty()) {
            throw new CsvImportException(lineNumber, column + " is empty");
        }
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new CsvImportException(lineNumber, column + " is longer than " + MAX_TEXT_LENGTH + " characters");
        }
        return value;
    }

    private static LocalDate parseDate(String value, String column, int lineNumber) throws CsvImportException {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new CsvImportException(lineNumber, column + " '" + value + "' is not a date like 2026-12-31");
        }
    }

    private static int parseNumber(String value, String column, int lineNumber) throws CsvImportException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CsvImportException(lineNumber, column + " '" + value + "' is not a whole number");
        }
    }
}
