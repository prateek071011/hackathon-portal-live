package com.htf.portal.dto;

import java.util.List;

/**
 * Outcome of a CSV import: how many rows were saved, and one message per bad line that was skipped.
 */
public record ImportResult(int importedCount, List<String> errors) {
}
