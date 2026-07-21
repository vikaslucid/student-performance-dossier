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
// First Name | Last Name | Email | Date of Birth (yyyy-MM-dd) | Enrollment Date (yyyy-MM-dd) |
// Student Number | Grade | Section
@Service
public class StudentImportService {

    private static final int COL_FIRST_NAME = 0;
    private static final int COL_LAST_NAME = 1;
    private static final int COL_EMAIL = 2;
    private static final int COL_DATE_OF_BIRTH = 3;
    private static final int COL_ENROLLMENT_DATE = 4;
    private static final int COL_STUDENT_NUMBER = 5;
    private static final int COL_GRADE = 6;
    private static final int COL_SECTION = 7;

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
        String firstName = requireText(row, COL_FIRST_NAME, "First Name", dataFormatter);
        String lastName = requireText(row, COL_LAST_NAME, "Last Name", dataFormatter);
        String email = requireText(row, COL_EMAIL, "Email", dataFormatter);
        LocalDate dateOfBirth = requireDate(row, COL_DATE_OF_BIRTH, "Date of Birth");
        LocalDate enrollmentDate = requireDate(row, COL_ENROLLMENT_DATE, "Enrollment Date");
        String studentNumber = requireText(row, COL_STUDENT_NUMBER, "Student Number", dataFormatter);
        String grade = requireText(row, COL_GRADE, "Grade", dataFormatter);
        String section = requireText(row, COL_SECTION, "Section", dataFormatter);

        Long schoolClassId = findSchoolClassId(grade, section);

        return new StudentRequest(firstName, lastName, email, dateOfBirth, enrollmentDate, studentNumber, schoolClassId);
    }

    private Long findSchoolClassId(String grade, String section) {
        List<SchoolClass> matches = schoolClassRepository.findByGradeAndSection(grade, section);
        if (matches.isEmpty()) {
            throw new InvalidRequestException(
                    "No school class found for grade '" + grade + "' section '" + section + "'");
        }
        if (matches.size() > 1) {
            throw new InvalidRequestException(
                    "Multiple school classes match grade '" + grade + "' section '" + section + "' - cannot determine which one");
        }
        return matches.get(0).getId();
    }

    private String requireText(Row row, int colIndex, String columnName, DataFormatter dataFormatter) {
        Cell cell = row.getCell(colIndex);
        String value = cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
        if (value.isEmpty()) {
            throw new InvalidRequestException(columnName + " is required");
        }
        return value;
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
        for (int col = COL_FIRST_NAME; col <= COL_SECTION; col++) {
            Cell cell = row.getCell(col);
            if (cell != null && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
