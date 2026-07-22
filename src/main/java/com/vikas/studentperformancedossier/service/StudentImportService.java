package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.StudentImportResult;
import com.vikas.studentperformancedossier.dto.StudentImportRowError;
import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.exception.InvalidRequestException;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Columns are matched by header text (case/punctuation/whitespace-insensitive), not fixed
// position - extra columns before/after/between the ones below, or a different column order,
// don't matter as long as the header row contains these names somewhere:
// Session | Class | Admission Date (yyyy-MM-dd) | Admission No. | Name | Father's Name |
// Father's Mobile | Mother's Name | Mother's Mobile | Address | Primary Parent | Primary Parent Mobile
//
// Class is split into class name + stream from a "Name (STREAM)" pattern, e.g. "Eleventh (ARTS)"
// -> class="Eleventh", stream="ARTS"; a value with no parentheses, e.g. "EIGHTH", is treated as
// class="EIGHTH" with no stream. Matching against an existing School Class is case-insensitive on
// both class and stream (source data casing is inconsistent, e.g. "Eleventh" vs "ELEVENTH") and
// ignores section - if a class+stream combination has more than one section, the row is reported
// as ambiguous rather than guessing. Session and the parent/guardian/address columns are optional
// (and can be missing from the file entirely); Class, Admission Date, Admission No. and Name are
// required - if one of those headers isn't found at all, the whole import is rejected up front.
@Service
public class StudentImportService {

    private static final String COL_SESSION = "session";
    private static final String COL_CLASS = "class";
    private static final String COL_ADMISSION_DATE = "admission date";
    private static final String COL_ADMISSION_NUMBER = "admission no";
    private static final String COL_NAME = "name";
    private static final String COL_FATHER_NAME = "fathers name";
    private static final String COL_FATHER_MOBILE = "fathers mobile";
    private static final String COL_MOTHER_NAME = "mothers name";
    private static final String COL_MOTHER_MOBILE = "mothers mobile";
    private static final String COL_ADDRESS = "address";
    private static final String COL_PRIMARY_PARENT = "primary parent";
    private static final String COL_PRIMARY_PARENT_MOBILE = "primary parent mobile";

    private static final Map<String, String> REQUIRED_COLUMN_DISPLAY_NAMES = Map.of(
            COL_CLASS, "Class",
            COL_ADMISSION_DATE, "Admission Date",
            COL_ADMISSION_NUMBER, "Admission No.",
            COL_NAME, "Name");

    // "Eleventh (ARTS)" -> class="Eleventh", stream="ARTS"; "EIGHTH" (no parens) -> class="EIGHTH", stream=null
    private static final Pattern CLASS_STREAM_PATTERN = Pattern.compile("^(.*?)\\s*\\(([^)]+)\\)\\s*$");

    private final StudentService studentService;
    private final SchoolClassRepository schoolClassRepository;

    public StudentImportService(StudentService studentService, SchoolClassRepository schoolClassRepository) {
        this.studentService = studentService;
        this.schoolClassRepository = schoolClassRepository;
    }

    public StudentImportResult importFromExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidRequestException("The uploaded file is empty");
        }

        int importedCount = 0;
        List<StudentImportRowError> errors = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new InvalidRequestException("The file has no header row");
            }
            Map<String, Integer> columns = mapHeaderColumns(headerRow, dataFormatter);
            checkRequiredColumnsPresent(columns);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, columns, dataFormatter)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;
                try {
                    StudentRequest request = parseRow(row, columns, dataFormatter);
                    studentService.create(request);
                    importedCount++;
                } catch (RuntimeException ex) {
                    errors.add(new StudentImportRowError(excelRowNumber, ex.getMessage()));
                }
            }
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof InvalidRequestException) {
                throw (InvalidRequestException) ex;
            }
            throw new InvalidRequestException("Could not read the uploaded file - is it a valid .xlsx or .xls file?");
        }

        return new StudentImportResult(importedCount, errors);
    }

    private Map<String, Integer> mapHeaderColumns(Row headerRow, DataFormatter dataFormatter) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(dataFormatter.formatCellValue(cell));
            if (!normalized.isEmpty()) {
                columns.putIfAbsent(normalized, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private String normalizeHeader(String header) {
        return header.toLowerCase()
                .replace("'", "")
                .replace("’", "")
                .replace(".", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private void checkRequiredColumnsPresent(Map<String, Integer> columns) {
        List<String> missing = REQUIRED_COLUMN_DISPLAY_NAMES.entrySet().stream()
                .filter(entry -> !columns.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException(
                    "The header row is missing required column(s): " + String.join(", ", missing));
        }
    }

    private StudentRequest parseRow(Row row, Map<String, Integer> columns, DataFormatter dataFormatter) {
        String session = optionalText(row, columns, COL_SESSION, dataFormatter);
        String grade = requireText(row, columns, COL_CLASS, "Class", dataFormatter);
        LocalDate enrollmentDate = requireDate(row, columns, COL_ADMISSION_DATE, "Admission Date");
        String studentNumber = requireText(row, columns, COL_ADMISSION_NUMBER, "Admission No.", dataFormatter);
        String fullName = requireText(row, columns, COL_NAME, "Name", dataFormatter);
        String fatherName = optionalText(row, columns, COL_FATHER_NAME, dataFormatter);
        String fatherMobile = optionalText(row, columns, COL_FATHER_MOBILE, dataFormatter);
        String motherName = optionalText(row, columns, COL_MOTHER_NAME, dataFormatter);
        String motherMobile = optionalText(row, columns, COL_MOTHER_MOBILE, dataFormatter);
        String address = optionalText(row, columns, COL_ADDRESS, dataFormatter);
        String primaryParent = optionalText(row, columns, COL_PRIMARY_PARENT, dataFormatter);
        String primaryParentMobile = optionalText(row, columns, COL_PRIMARY_PARENT_MOBILE, dataFormatter);

        String[] nameParts = splitName(fullName);
        String[] classStream = splitClassStream(grade);
        Long schoolClassId = findSchoolClassId(classStream[0], classStream[1]);

        return new StudentRequest(
                nameParts[0],
                nameParts[1],
                null,
                null,
                enrollmentDate,
                studentNumber,
                schoolClassId,
                session,
                fatherName,
                fatherMobile,
                motherName,
                motherMobile,
                address,
                primaryParent,
                primaryParentMobile);
    }

    private String[] splitName(String fullName) {
        int spaceIndex = fullName.indexOf(' ');
        if (spaceIndex < 0) {
            return new String[]{fullName, ""};
        }
        return new String[]{fullName.substring(0, spaceIndex), fullName.substring(spaceIndex + 1).trim()};
    }

    // "Eleventh (ARTS)" -> {"Eleventh", "ARTS"}; "EIGHTH" (no parens) -> {"EIGHTH", null}
    private String[] splitClassStream(String classColumnValue) {
        Matcher matcher = CLASS_STREAM_PATTERN.matcher(classColumnValue);
        if (matcher.matches()) {
            return new String[]{matcher.group(1).trim(), matcher.group(2).trim()};
        }
        return new String[]{classColumnValue.trim(), null};
    }

    private Long findSchoolClassId(String grade, String stream) {
        List<SchoolClass> matches = stream == null
                ? schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull(grade)
                : schoolClassRepository.findByGradeIgnoreCaseAndStreamIgnoreCase(grade, stream);

        String description = stream == null ? "class '" + grade + "'" : "class '" + grade + "', stream '" + stream + "'";
        if (matches.isEmpty()) {
            throw new InvalidRequestException("No school class found for " + description);
        }
        if (matches.size() > 1) {
            throw new InvalidRequestException(
                    "Multiple school classes match " + description + " - cannot determine which one");
        }
        return matches.get(0).getId();
    }

    private String requireText(Row row, Map<String, Integer> columns, String columnKey, String columnName,
                                DataFormatter dataFormatter) {
        String value = optionalText(row, columns, columnKey, dataFormatter);
        if (value == null) {
            throw new InvalidRequestException(columnName + " is required");
        }
        return value;
    }

    private String optionalText(Row row, Map<String, Integer> columns, String columnKey, DataFormatter dataFormatter) {
        Integer colIndex = columns.get(columnKey);
        if (colIndex == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        String value = cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDate requireDate(Row row, Map<String, Integer> columns, String columnKey, String columnName) {
        Integer colIndex = columns.get(columnKey);
        Cell cell = colIndex == null ? null : row.getCell(colIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new InvalidRequestException(columnName + " is required");
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : null;
        if (text == null || text.isEmpty()) {
            throw new InvalidRequestException(columnName + " must be a date (yyyy-MM-dd)");
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestException(columnName + " must be a date in yyyy-MM-dd format, got '" + text + "'");
        }
    }

    private boolean isBlankRow(Row row, Map<String, Integer> columns, DataFormatter dataFormatter) {
        for (Integer colIndex : columns.values()) {
            Cell cell = row.getCell(colIndex);
            if (cell != null && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
