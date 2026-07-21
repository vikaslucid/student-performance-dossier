package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.StudentImportResult;
import com.vikas.studentperformancedossier.dto.StudentRequest;
import com.vikas.studentperformancedossier.dto.StudentResponse;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.exception.InvalidRequestException;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void importFromExcel_whenValidRow_importsSuccessfully() throws IOException {
        when(schoolClassRepository.findByGradeAndSection("Grade 10", "A"))
                .thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class)))
                .thenReturn(new StudentResponse(1L, "Ada", "Lovelace", "ada@example.com",
                        LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), "S-100", 1L, null, null));

        MultipartFile file = workbookWithRows(
                new String[]{"Ada", "Lovelace", "ada@example.com", "1990-01-01", "2020-01-01", "S-100", "Grade 10", "A"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        verify(studentService).create(new StudentRequest(
                "Ada", "Lovelace", "ada@example.com",
                LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), "S-100", 1L));
    }

    @Test
    void importFromExcel_whenRequiredFieldBlank_recordsRowErrorAndSkipsRow() throws IOException {
        MultipartFile file = workbookWithRows(
                new String[]{"", "Lovelace", "ada@example.com", "1990-01-01", "2020-01-01", "S-100", "Grade 10", "A"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(result.errors().get(0).message()).contains("First Name");
        verify(studentService, never()).create(any());
    }

    @Test
    void importFromExcel_whenSchoolClassNotFound_recordsRowError() throws IOException {
        when(schoolClassRepository.findByGradeAndSection("Grade 10", "Z")).thenReturn(List.of());

        MultipartFile file = workbookWithRows(
                new String[]{"Ada", "Lovelace", "ada@example.com", "1990-01-01", "2020-01-01", "S-100", "Grade 10", "Z"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("No school class found");
        verify(studentService, never()).create(any());
    }

    @Test
    void importFromExcel_whenGradeSectionAmbiguous_recordsRowError() throws IOException {
        when(schoolClassRepository.findByGradeAndSection("Grade 10", "A"))
                .thenReturn(List.of(schoolClass(1L), schoolClass(2L)));

        MultipartFile file = workbookWithRows(
                new String[]{"Ada", "Lovelace", "ada@example.com", "1990-01-01", "2020-01-01", "S-100", "Grade 10", "A"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("Multiple school classes match");
    }

    @Test
    void importFromExcel_whenInvalidDate_recordsRowError() throws IOException {
        MultipartFile file = workbookWithRows(
                new String[]{"Ada", "Lovelace", "ada@example.com", "not-a-date", "2020-01-01", "S-100", "Grade 10", "A"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(0);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("Date of Birth");
    }

    @Test
    void importFromExcel_whenDuplicateEmail_recordsRowErrorButContinuesOtherRows() throws IOException {
        when(schoolClassRepository.findByGradeAndSection("Grade 10", "A"))
                .thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class)))
                .thenThrow(new DuplicateResourceException("A student with email 'ada@example.com' already exists"))
                .thenReturn(new StudentResponse(2L, "Grace", "Hopper", "grace@example.com",
                        LocalDate.of(1991, 1, 1), LocalDate.of(2020, 1, 1), "S-101", 1L, null, null));

        MultipartFile file = workbookWithRows(
                new String[]{"Ada", "Lovelace", "ada@example.com", "1990-01-01", "2020-01-01", "S-100", "Grade 10", "A"},
                new String[]{"Grace", "Hopper", "grace@example.com", "1991-01-01", "2020-01-01", "S-101", "Grade 10", "A"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(result.errors().get(0).message()).contains("ada@example.com");
    }

    @Test
    void importFromExcel_whenBlankRow_skipsWithoutError() throws IOException {
        when(schoolClassRepository.findByGradeAndSection("Grade 10", "A"))
                .thenReturn(List.of(schoolClass(1L)));
        when(studentService.create(any(StudentRequest.class)))
                .thenReturn(new StudentResponse(1L, "Ada", "Lovelace", "ada@example.com",
                        LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), "S-100", 1L, null, null));

        MultipartFile file = workbookWithRows(
                new String[]{"", "", "", "", "", "", "", ""},
                new String[]{"Ada", "Lovelace", "ada@example.com", "1990-01-01", "2020-01-01", "S-100", "Grade 10", "A"}
        );

        StudentImportResult result = studentImportService.importFromExcel(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
    }

    private SchoolClass schoolClass(Long id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setGrade("Grade 10");
        schoolClass.setSection("A");
        return schoolClass;
    }

    private MultipartFile workbookWithRows(String[]... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] headers = {"First Name", "Last Name", "Email", "Date of Birth", "Enrollment Date",
                    "Student Number", "Grade", "Section"};
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
}
