package com.vikas.studentperformancedossier.dto;

import java.util.List;

public record StudentImportResult(int importedCount, List<StudentImportRowError> errors) {
}
