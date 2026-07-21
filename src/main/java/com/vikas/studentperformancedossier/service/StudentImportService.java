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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Expected columns (header row required, values start on row 2):
// Session | Class | Admission Date (yyyy-MM-dd) | Admission No. | Name | Father's Name |
// Father's Mobile | Mother's Name | Mother's Mobile | Address | Primary Parent | Primary Parent Mobile
//
// Class is matched against an existing School Class by grade alone (no section column in the
// source data) - if a grade has more than one section, the row is reported as ambiguous rather
// than guessing. Session and the parent/guardian/address columns are optional; Class, Admission
// Date, Admission No. and Name are required to create a valid student.
@Service
public class StudentImportService {

    private static final int COL_SESSION = 0;
    private static final int COL_CLASS = 1;
    private static final int COL_ADMISSION_DATE = 2;
    private static final int COL_ADMISSION_NUMBER = 3;
    private static final int COL_NAME = 4;
    private static final int COL_FATHER_NAME = 5;
    private static final int COL_FATHER_MOBILE = 6;
    private static final int COL_MOTHER_NAME = 7;
    private static final int COL_MOTHER_MOBILE = 8;
    private static final int COL_ADDRESS = 9;
    private static final int COL_PRIMARY_PARENT = 10;
    private static final int COL_PRIMARY_PARENT_MOBILE = 11;

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

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, dataFormatter)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;
                try {
                    StudentRequest request = parseRow(row, dataFormatter);
                    studentService.create(request);
                    importedCount++;
                } catch (RuntimeException ex) {
                    errors.add(new StudentImportRowError(excelRowNumber, ex.getMessage()));
                }
            }
        } catch (IOException | RuntimeException ex) {
            throw new InvalidRequestException("Could not read the uploaded file - is it a valid .xlsx file?");
        }

        return new StudentImportResult(importedCount, errors);
    }

    private StudentRequest parseRow(Row row, DataFormatter dataFormatter) {
        String session = optionalText(row, COL_SESSION, dataFormatter);
        String grade = requireText(row, COL_CLASS, "Class", dataFormatter);
        LocalDate enrollmentDate = requireDate(row, COL_ADMISSION_DATE, "Admission Date");
        String studentNumber = requireText(row, COL_ADMISSION_NUMBER, "Admission No.", dataFormatter);
        String fullName = requireText(row, COL_NAME, "Name", dataFormatter);
        String fatherName = optionalText(row, COL_FATHER_NAME, dataFormatter);
        String fatherMobile = optionalText(row, COL_FATHER_MOBILE, dataFormatter);
        String motherName = optionalText(row, COL_MOTHER_NAME, dataFormatter);
        String motherMobile = optionalText(row, COL_MOTHER_MOBILE, dataFormatter);
        String address = optionalText(row, COL_ADDRESS, dataFormatter);
        String primaryParent = optionalText(row, COL_PRIMARY_PARENT, dataFormatter);
        String primaryParentMobile = optionalText(row, COL_PRIMARY_PARENT_MOBILE, dataFormatter);

        String[] nameParts = splitName(fullName);
        Long schoolClassId = findSchoolClassId(grade);

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

    private Long findSchoolClassId(String grade) {
        List<SchoolClass> matches = schoolClassRepository.findByGrade(grade);
        if (matches.isEmpty()) {
            throw new InvalidRequestException("No school class found for grade '" + grade + "'");
        }
        if (matches.size() > 1) {
            throw new InvalidRequestException(
                    "Multiple school classes match grade '" + grade + "' - cannot determine which one");
        }
        return matches.get(0).getId();
    }

    private String requireText(Row row, int colIndex, String columnName, DataFormatter dataFormatter) {
        String value = optionalText(row, colIndex, dataFormatter);
        if (value == null) {
            throw new InvalidRequestException(columnName + " is required");
        }
        return value;
    }

    private String optionalText(Row row, int colIndex, DataFormatter dataFormatter) {
        Cell cell = row.getCell(colIndex);
        String value = cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDate requireDate(Row row, int colIndex, String columnName) {
        Cell cell = row.getCell(colIndex);
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

    private boolean isBlankRow(Row row, DataFormatter dataFormatter) {
        for (int col = COL_SESSION; col <= COL_PRIMARY_PARENT_MOBILE; col++) {
            Cell cell = row.getCell(col);
            if (cell != null && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
