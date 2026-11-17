package com.htf.portal.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes rows as CSV text with a BufferedWriter (a character stream), e.g. the college's
 * "Download CSV" of registered teams. Values with commas, quotes or line breaks are quoted.
 */
public class CsvWriter {

    public void write(String header, List<List<String>> rows, Writer target) throws IOException {
        // BufferedWriter collects text in memory and sends it in big chunks instead of character by character.
        BufferedWriter out = new BufferedWriter(target);
        out.write(header);
        out.newLine();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    out.write(',');
                }
                out.write(escape(row.get(i)));
            }
            out.newLine();
        }
        // flush, not close: the target (the HTTP response) belongs to Spring, which closes it.
        out.flush();
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        String text = value;
        // A cell starting with = + - @ would run as a formula in Excel ("CSV injection"); a leading ' stops that.
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
