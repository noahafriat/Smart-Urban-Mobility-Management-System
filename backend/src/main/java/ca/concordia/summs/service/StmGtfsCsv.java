package ca.concordia.summs.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared STM static GTFS CSV parsing helpers used by multiple loaders.
 */
public final class StmGtfsCsv {

    private static final Pattern FIVE_DIGITS = Pattern.compile("\\d{5}");

    private StmGtfsCsv() {}

    /** Same options as STM {@code .txt} exports: header row, skip blank lines. */
    public static CSVFormat stmFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build();
    }

    /** Safe cell read: missing column → empty string. */
    public static String cell(CSVRecord rec, String header) {
        try {
            String v = rec.get(header);
            return v == null ? "" : v.trim();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Last {@code \\d{5}} group in {@code s}, for matching sign codes embedded in composite stop ids.
     */
    public static Optional<String> lastFiveDigitSequence(String s) {
        if (s == null || s.isBlank()) {
            return Optional.empty();
        }
        String last = null;
        Matcher m = FIVE_DIGITS.matcher(s);
        while (m.find()) {
            last = m.group();
        }
        return Optional.ofNullable(last);
    }
}
