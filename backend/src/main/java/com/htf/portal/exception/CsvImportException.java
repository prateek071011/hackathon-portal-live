package com.htf.portal.exception;

/**
 * Thrown when one line of an imported CSV file is bad.
 * This is our only CHECKED exception (extends Exception): the compiler forces the caller
 * to catch it, which the import does to record the bad line and carry on with the next one.
 * Compare with PortalException, which is unchecked and may travel up to the global handler.
 */
public class CsvImportException extends Exception {

    private final int lineNumber;

    public CsvImportException(int lineNumber, String reason) {
        super("Line " + lineNumber + ": " + reason);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
