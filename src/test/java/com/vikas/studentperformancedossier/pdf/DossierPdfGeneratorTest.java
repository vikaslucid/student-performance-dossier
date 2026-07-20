package com.vikas.studentperformancedossier.pdf;

import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DossierPdfGeneratorTest {

    private final DossierPdfGenerator generator = new DossierPdfGenerator();

    @Test
    void generate_producesBytesStartingWithPdfMagicHeader() {
        byte[] pdfBytes = generator.generate(sampleDossier());

        assertThat(pdfBytes).isNotEmpty();
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    void generate_whenNoMarks_stillProducesValidPdf() {
        DossierResponse emptyDossier = new DossierResponse(1L, "Ada Lovelace", null, List.of(), List.of());

        byte[] pdfBytes = generator.generate(emptyDossier);

        assertThat(pdfBytes).isNotEmpty();
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    private DossierResponse sampleDossier() {
        SubjectAverageResponse subjectAverage = new SubjectAverageResponse(1L, "Mathematics", 85.0);
        ExamSummaryResponse examSummary = new ExamSummaryResponse(
                1L, "Midterm", LocalDate.of(2026, 3, 1), 1L, "Mathematics", 85, 100, 85.0, true, "A");

        return new DossierResponse(1L, "Ada Lovelace", 85.0, List.of(subjectAverage), List.of(examSummary));
    }
}
