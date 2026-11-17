package com.htf.portal.io;

import java.util.List;

import com.htf.portal.exception.CsvImportException;

/**
 * Turns the values of one CSV line into an object of type T (e.g. a Hackathon).
 * CsvReader handles the file; a parser only knows its own columns and rules.
 */
public interface CsvLineParser<T> {

    // The exact first line the file must have, e.g. "title,organizer,...".
    String header();

    // Throws the CHECKED CsvImportException for a bad line, so the reader is forced to deal with it.
    T parse(List<String> values, int lineNumber) throws CsvImportException;
}
