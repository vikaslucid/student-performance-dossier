package com.vikas.studentperformancedossier.pdf;

import com.vikas.studentperformancedossier.dto.BehaviourResponse;
import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DossierPdfGeneratorTest {

    private final DossierPdfGenerator generator = new DossierPdfGenerator();

    @Test
    void generate_producesBytesStartingWithPdfMagicHeader() {
        byte[] pdfBytes = generator.generate(sampleDossier(), "2026-03-15", "Mrs. Sharma");

        assertThat(pdfBytes).isNotEmpty();
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    void generate_whenNoMarksOrBehaviour_stillProducesValidPdf() {
        DossierResponse emptyDossier = new DossierResponse(
                1L, "Ada Lovelace", "S-100", "Central High", "Grade 10", null, "A", "2025-2026",
                null, null, List.of(), List.of(), null);

        byte[] pdfBytes = generator.generate(emptyDossier, null, null);

        assertThat(pdfBytes).isNotEmpty();
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    private DossierResponse sampleDossier() {
        SubjectAverageResponse subjectAverage = new SubjectAverageResponse(1L, "Mathematics", 85.0);
        ExamSummaryResponse examSummary = new ExamSummaryResponse(
                1L, "Midterm", LocalDate.of(2026, 3, 1), 1L, "Mathematics",
                4, 4, 4, 4, 4, 20, 25, 80.0, true, "A2");
        BehaviourResponse behaviour = new BehaviourResponse(
                1L, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                "Participates actively.", "Good listener.", "Time management.", "None",
                1L, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0));

        return new DossierResponse(
                1L, "Ada Lovelace", "S-100", "Central High", "Grade 10", null, "A", "2025-2026",
                85.0, "A", List.of(subjectAverage), List.of(examSummary), behaviour);
    }
}
