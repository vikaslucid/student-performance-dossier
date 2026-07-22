package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.StudentImportResult;
import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.dto.StudentResponse;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.exception.InvalidRequestException;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Column order under test: Session | Class | Admission Date | Admission No. | Name |
// Father's Name | Father's Mobile | Mother's Name | Mother's Mobile | Address |
// Primary Parent | Primary Parent Mobile
@ExtendWith(MockitoExtension.class)
class StudentImportServiceTest {

    @Mock
    private StudentService studentService;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @InjectMocks
    private StudentImportService studentImportService;

    @Test
    void importFromExcel_whenFileEmpty_throwsInvalidRequestException() {
        MultipartFile empty = new MockMultipartFile("file", "students.xlsx", null, new byte[0]);

        assertThatThrownBy(() -> studentImportService.importFromExcel(empty))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void importFromExcel_whenNotAValidWorkbook_throwsInvalidRequestException() {
        MultipartFile garbage = new MockMultipartFile("file", "students.xlsx", null, "not an xlsx file".getBytes());

        assertThatThrownBy(() -> studentImportService.importFromExcel(garbage))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void importFromExcel_whenValidRow_importsSuccessfullyAndSplitsName() throws IOException {
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("10")).thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class)))
                .thenReturn(sampleResponse());

        MultipartFile file = workbookWithRows(
                new String[]{"2024-25", "10", "2020-01-01", "S-100", "Ada Lovelace",
                        "Father", "9990000001", "Mother", "9990000002", "123 Main St", "Father", "9990000001"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        verify(studentService).create(new StudentRequest(
                "Ada", "Lovelace", null, null,
                LocalDate.of(2020, 1, 1), "S-100", 1L,
                "2024-25", "Father", "9990000001", "Mother", "9990000002",
                "123 Main St", "Father", "9990000001"));
    }

    @Test
    void importFromExcel_whenLegacyXlsFile_importsSuccessfully() throws IOException {
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("10")).thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class))).thenReturn(sampleResponse());

        MultipartFile file = legacyXlsWorkbookWithRows(
                new String[]{"2024-25", "10", "2020-01-01", "S-100", "Ada Lovelace",
                        "Father", "9990000001", "Mother", "9990000002", "123 Main St", "Father", "9990000001"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void importFromExcel_whenSingleWordName_lastNameIsEmpty() throws IOException {
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("10")).thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class))).thenReturn(sampleResponse());

        MultipartFile file = workbookWithRows(
                new String[]{"", "10", "2020-01-01", "S-100", "Madonna", "", "", "", "", "", "", ""}
        );

        studentImportService.importFromExcel(file);

        verify(studentService).create(eq(new StudentRequest(
                "Madonna", "", null, null,
                LocalDate.of(2020, 1, 1), "S-100", 1L,
                null, null, null, null, null, null, null, null)));
    }

    @Test
    void importFromExcel_whenRequiredFieldBlank_recordsRowErrorAndSkipsRow() throws IOException {
        MultipartFile file = workbookWithRows(
                new String[]{"", "10", "2020-01-01", "S-100", "", "", "", "", "", "", "", ""}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(result.errors().get(0).message()).contains("Name");
        verify(studentService, never()).create(any());
    }

    @Test
    void importFromExcel_whenClassNotFound_recordsRowError() throws IOException {
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("99")).thenReturn(List.of());

        MultipartFile file = workbookWithRows(
                new String[]{"", "99", "2020-01-01", "S-100", "Ada Lovelace", "", "", "", "", "", "", ""}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("No school class found");
        verify(studentService, never()).create(any());
    }

    @Test
    void importFromExcel_whenClassAmbiguous_recordsRowError() throws IOException {
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("10")).thenReturn(List.of(schoolClass(1L), schoolClass(2L)));

        MultipartFile file = workbookWithRows(
                new String[]{"", "10", "2020-01-01", "S-100", "Ada Lovelace", "", "", "", "", "", "", ""}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("Multiple school classes match");
    }

    @Test
    void importFromExcel_whenInvalidDate_recordsRowError() throws IOException {
        MultipartFile file = workbookWithRows(
                new String[]{"", "10", "not-a-date", "S-100", "Ada Lovelace", "", "", "", "", "", "", ""}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("Admission Date");
    }

    @Test
    void importFromExcel_whenDuplicateStudentNumber_recordsRowErrorButContinuesOtherRows() throws IOException {
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("10")).thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class)))
                .thenThrow(new DuplicateResourceException("A student with student number 'S-100' already exists"))
                .thenReturn(sampleResponse());

        MultipartFile file = workbookWithRows(
                new String[]{"", "10", "2020-01-01", "S-100", "Ada Lovelace", "", "", "", "", "", "", ""},
                new String[]{"", "10", "2020-01-01", "S-101", "Grace Hopper", "", "", "", "", "", "", ""}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(result.errors().get(0).message()).contains("S-100");
    }

    @Test
    void importFromExcel_whenExtraAndSkippedColumns_matchesByHeaderNameNotPosition() throws IOException {
        // Reproduces a real-world file layout: two unrelated leading columns, then Session/Class,
        // then a blank/hidden column, then the rest - same shape as a real school register export.
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIgnoreCase("Eleventh", "COMMERCE"))
                .thenReturn(List.of(schoolClassWithStream(1L, "Eleventh", "COMMERCE")));
        when(studentService.create(any(StudentRequest.class))).thenReturn(sampleResponse());

        String[] headers = {"S.No", "Roll No", "Session", "Class", "", "Admission Date", "Admission No.",
                "Name", "Father's Name", "Father's Mobile", "Mother's Name", "Mother's Mobile", "Address"};
        String[] row = {"1", "12", "2026-2027", "Eleventh (COMMERCE)", "", "2026-04-21", "0838",
                "AGAM SINGH", "KAWAL SINGH", "9416250946", "RAMANDEEP", "9466507305", "GAJLANA, (166)"};

        MultipartFile file = customWorkbookWithRows(headers, row);

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        verify(studentService).create(new StudentRequest(
                "AGAM", "SINGH", null, null,
                LocalDate.of(2026, 4, 21), "0838", 1L,
                "2026-2027", "KAWAL SINGH", "9416250946", "RAMANDEEP", "9466507305",
                "GAJLANA, (166)", null, null));
    }

    @Test
    void importFromExcel_whenClassCasingDiffersFromStored_matchesCaseInsensitively() throws IOException {
        // Real source data is inconsistent: "ELEVENTH (MEDICAL)" vs "Eleventh (Medical)" for the same
        // stored School Class - matching must not be case-sensitive on either class or stream.
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIgnoreCase("ELEVENTH", "MEDICAL"))
                .thenReturn(List.of(schoolClassWithStream(1L, "Eleventh", "Medical")));
        when(studentService.create(any(StudentRequest.class))).thenReturn(sampleResponse());

        MultipartFile file = workbookWithRows(
                new String[]{"", "ELEVENTH (MEDICAL)", "2020-01-01", "S-100", "Ada Lovelace",
                        "", "", "", "", "", "", ""}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void importFromExcel_whenRequiredHeaderMissing_throwsInvalidRequestException() throws IOException {
        String[] headers = {"Session", "Admission Date", "Admission No.", "Name"};
        String[] row = {"2024-25", "2020-01-01", "S-100", "Ada Lovelace"};

        MultipartFile file = customWorkbookWithRows(headers, row);

        assertThatThrownBy(() -> studentImportService.importFromExcel(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Class");
    }

    @Test
    void importFromExcel_whenBlankRow_skipsWithoutError() throws IOException {
        when(schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("10")).thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class))).thenReturn(sampleResponse());

        MultipartFile file = workbookWithRows(
                new String[]{"", "", "", "", "", "", "", "", "", "", "", ""},
                new String[]{"", "10", "2020-01-01", "S-100", "Ada Lovelace", "", "", "", "", "", "", ""}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    private SchoolClass schoolClass(Long id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade("10");
        schoolClass.setSection("A");
        return schoolClass;
    }

    private SchoolClass schoolClassWithStream(Long id, String grade, String stream) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade(grade);
        schoolClass.setStream(stream);
        schoolClass.setSection("A");
        return schoolClass;
    }

    private StudentResponse sampleResponse() {
        return new StudentResponse(1L, "Ada", "Lovelace", null, null,
                LocalDate.of(2020, 1, 1), "S-100", 1L,
                null, null, null, null, null, null, null, null, null, null);
    }

    private MultipartFile workbookWithRows(String[]... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] headers = {"Session", "Class", "Admission Date", "Admission No.", "Name",
                    "Father's Name", "Father's Mobile", "Mother's Name", "Mother's Mobile", "Address",
                    "Primary Parent", "Primary Parent Mobile"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = rows[r];
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile("file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private MultipartFile customWorkbookWithRows(String[] headers, String[]... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = rows[r];
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile("file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private MultipartFile legacyXlsWorkbookWithRows(String[]... rows) throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            HSSFSheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] headers = {"Session", "Class", "Admission Date", "Admission No.", "Name",
                    "Father's Name", "Father's Mobile", "Mother's Name", "Mother's Mobile", "Address",
                    "Primary Parent", "Primary Parent Mobile"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = rows[r];
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(values[c]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile("file", "students.xls", "application/vnd.ms-excel", out.toByteArray());
        }
    }
}
