package com.vikas.studentperformancedossier.pdf;

import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class DossierPdfGenerator {

    private static final float MARGIN = 50;
    private static final float LEADING = 16;

    public byte[] generate(DossierResponse dossier) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                float y = page.getMediaBox().getHeight() - MARGIN;

                y = writeLine(content, bold, 16, y, "Student Performance Dossier");
                y -= LEADING;

                y = writeLine(content, regular, 12, y,
                        "Student: " + dossier.studentName() + " (ID: " + dossier.studentId() + ")");
                y = writeLine(content, regular, 12, y,
                        "Overall Average: " + formatPercentage(dossier.overallAveragePercentage()));
                y -= LEADING;

                y = writeLine(content, bold, 12, y, "Subject-wise Averages");
                if (dossier.subjectAverages().isEmpty()) {
                    y = writeLine(content, regular, 11, y, "No subjects recorded yet.");
                }
                for (SubjectAverageResponse subject : dossier.subjectAverages()) {
                    y = writeLine(content, regular, 11, y,
                            "- " + subject.subjectName() + ": " + formatPercentage(subject.averagePercentage()));
                }
                y -= LEADING;

                y = writeLine(content, bold, 12, y, "Exam Results");
                if (dossier.examSummaries().isEmpty()) {
                    writeLine(content, regular, 11, y, "No exams recorded yet.");
                }
                for (ExamSummaryResponse exam : dossier.examSummaries()) {
                    String gradeText = exam.grade() != null ? ", Grade: " + exam.grade() : "";
                    y = writeLine(content, regular, 11, y,
                            "- " + exam.examName() + " (" + exam.subjectName() + "): "
                                    + exam.obtainedMarks() + "/" + exam.maximumMarks()
                                    + " (" + formatPercentage(exam.percentage()) + ") - "
                                    + (exam.passed() ? "PASS" : "FAIL") + gradeText);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate dossier PDF", e);
        }
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, int fontSize, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(MARGIN, y);
        content.showText(text);
        content.endText();
        return y - LEADING;
    }

    private String formatPercentage(Double value) {
        return value == null ? "N/A" : String.format("%.2f%%", value);
    }
}
