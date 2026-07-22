package com.vikas.studentperformancedossier.pdf;

import com.vikas.studentperformancedossier.dto.BehaviourResponse;
import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.dto.ExamSummaryResponse;
import com.vikas.studentperformancedossier.dto.SubjectAverageResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

// Renders the dossier as a PTM (Parent-Teacher Meeting) style report: student/class header,
// a per-exam academic breakdown table (mirroring the 5-component Mark rubric), the student's
// behaviour/conduct profile, and a grade-band legend - modelled on the real-world PTM report
// this project's Stage B/C features were built to replace.
@Component
public class DossierPdfGenerator {

    private static final float MARGIN = 40;
    private static final float LEADING = 14;
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    public byte[] generate(DossierResponse dossier, String ptmDate, String classTeacher) {
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDDocument document = new PDDocument()) {
            PdfContext ctx = new PdfContext(document, regular, bold);
            try {
                writeHeader(ctx, dossier, ptmDate, classTeacher);
                writeAcademicTable(ctx, dossier);
                writeOverallStanding(ctx, dossier);
                writeSubjectAverages(ctx, dossier);
                writeBehaviourSection(ctx, dossier.behaviour());
                writeGradeLegend(ctx);
            } finally {
                ctx.close();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate dossier PDF", e);
        }
    }

    private void writeHeader(PdfContext ctx, DossierResponse dossier, String ptmDate, String classTeacher)
            throws IOException {
        ctx.writeLine(ctx.bold, 16, dossier.schoolName(), true);
        ctx.writeLine(ctx.bold, 13, "Parent-Teacher Meeting Report", true);
        ctx.gap();

        String className = dossier.grade()
                + (dossier.stream() != null ? " (" + dossier.stream() + ")" : "")
                + " - " + dossier.section();

        ctx.writeKeyValueRow("Student Name:", dossier.studentName(), "Student No.:", dossier.studentNumber());
        ctx.writeKeyValueRow("Class:", className, "Session:", valueOrDash(dossier.session()));
        ctx.writeKeyValueRow("PTM Date:", valueOrDash(ptmDate), "Class Teacher:", valueOrDash(classTeacher));
        ctx.gap();
    }

    private void writeAcademicTable(PdfContext ctx, DossierResponse dossier) throws IOException {
        ctx.writeLine(ctx.bold, 12, "Academic Performance", false);

        String[] headers = {"Subject", "Exam", "Con.", "App.", "Acc.", "HW", "Test", "Total", "%", "Grade"};
        float[] widths = {0.20f, 0.20f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.08f, 0.09f, 0.08f};

        if (dossier.examSummaries().isEmpty()) {
            ctx.writeLine(ctx.regular, 10, "No exams recorded yet.", false);
            ctx.gap();
            return;
        }

        ctx.writeTableRow(headers, widths, ctx.bold, true);
        for (ExamSummaryResponse exam : dossier.examSummaries()) {
            String[] row = {
                    exam.subjectName(),
                    exam.examName(),
                    String.valueOf(exam.concept()),
                    String.valueOf(exam.application()),
                    String.valueOf(exam.accuracy()),
                    String.valueOf(exam.homework()),
                    String.valueOf(exam.test()),
                    exam.obtainedMarks() + "/" + exam.maximumMarks(),
                    formatPercentage(exam.percentage()),
                    exam.grade()
            };
            ctx.writeTableRow(row, widths, ctx.regular, false);
        }
        ctx.gap();
    }

    private void writeOverallStanding(PdfContext ctx, DossierResponse dossier) throws IOException {
        ctx.writeLine(ctx.bold, 12, "Overall Standing", false);
        ctx.writeLine(ctx.regular, 11,
                "Overall Average: " + formatPercentage(dossier.overallAveragePercentage())
                        + "    Overall Grade: " + valueOrDash(dossier.overallGrade()),
                false);
        ctx.gap();
    }

    private void writeSubjectAverages(PdfContext ctx, DossierResponse dossier) throws IOException {
        ctx.writeLine(ctx.bold, 12, "Subject-wise Averages", false);
        if (dossier.subjectAverages().isEmpty()) {
            ctx.writeLine(ctx.regular, 10, "No subjects recorded yet.", false);
        }
        for (SubjectAverageResponse subject : dossier.subjectAverages()) {
            ctx.writeLine(ctx.regular, 10,
                    "- " + subject.subjectName() + ": " + formatPercentage(subject.averagePercentage()), false);
        }
        ctx.gap();
    }

    private void writeBehaviourSection(PdfContext ctx, BehaviourResponse behaviour) throws IOException {
        ctx.writeLine(ctx.bold, 12, "Behaviour & Conduct", false);

        if (behaviour == null) {
            ctx.writeLine(ctx.regular, 10, "No behaviour record on file yet.", false);
            ctx.gap();
            return;
        }

        String[] headers = {"Attention", "Participation", "Discipline", "Homework Resp.", "Communication"};
        float[] widths = {0.2f, 0.2f, 0.2f, 0.2f, 0.2f};
        ctx.writeTableRow(headers, widths, ctx.bold, true);
        ctx.writeTableRow(new String[]{
                String.valueOf(behaviour.attention()),
                String.valueOf(behaviour.participation()),
                String.valueOf(behaviour.discipline()),
                String.valueOf(behaviour.homeworkResponsibility()),
                String.valueOf(behaviour.communicationSkills())
        }, widths, ctx.regular, false);

        String[] headers2 = {"Confidence", "Teamwork", "Curiosity", "Leadership", "Critical Thinking"};
        ctx.writeTableRow(headers2, widths, ctx.bold, true);
        ctx.writeTableRow(new String[]{
                String.valueOf(behaviour.confidence()),
                String.valueOf(behaviour.teamwork()),
                String.valueOf(behaviour.curiosity()),
                String.valueOf(behaviour.leadership()),
                String.valueOf(behaviour.criticalThinking())
        }, widths, ctx.regular, false);

        ctx.writeLine(ctx.regular, 10, "Overall Behaviour: " + behaviour.overallBehaviour() + " / 5", false);
        ctx.gap();

        writeNoteField(ctx, "Anecdotal Observation", behaviour.anecdotalObservation());
        writeNoteField(ctx, "Strength", behaviour.strength());
        writeNoteField(ctx, "Needs Improvement", behaviour.needsImprovement());
        writeNoteField(ctx, "Parent Support Required", behaviour.parentSupportRequired());
        ctx.gap();
    }

    private void writeNoteField(PdfContext ctx, String label, String value) throws IOException {
        ctx.writeLine(ctx.bold, 10, label + ":", false);
        ctx.writeWrapped(ctx.regular, 10, valueOrDash(value));
    }

    private void writeGradeLegend(PdfContext ctx) throws IOException {
        ctx.writeLine(ctx.bold, 12, "Grade Legend", false);
        ctx.writeLine(ctx.regular, 10,
                "Subject Grades: A1 (90-100%)  A2 (80-89%)  B1 (70-79%)  B2 (60-69%)  C (50-59%)  D (Below 50%)",
                false);
        ctx.writeLine(ctx.regular, 10,
                "Overall Grade: A+ (90-100%)  A (75-89%)  B (60-74%)  C (45-59%)  Needs Attention (Below 45%)",
                false);
    }

    private String formatPercentage(Double value) {
        return value == null ? "N/A" : String.format("%.2f%%", value);
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    // Owns the current page/content-stream and advances to a new page whenever the next write
    // would run past the bottom margin, so the report can safely span multiple pages.
    private static final class PdfContext {
        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private PDPageContentStream content;
        private float y;

        private PdfContext(PDDocument document, PDFont regular, PDFont bold) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            startNewPage();
        }

        private void startNewPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN) {
                startNewPage();
            }
        }

        private void gap() {
            y -= LEADING / 2;
        }

        private void writeLine(PDFont font, int fontSize, String text, boolean centered) throws IOException {
            ensureSpace(LEADING);
            content.beginText();
            content.setFont(font, fontSize);
            float x = MARGIN;
            if (centered) {
                float textWidth = font.getStringWidth(text) / 1000 * fontSize;
                x = MARGIN + (CONTENT_WIDTH - textWidth) / 2;
            }
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
            y -= LEADING;
        }

        private void writeKeyValueRow(String label1, String value1, String label2, String value2)
                throws IOException {
            ensureSpace(LEADING);
            float col2X = MARGIN + CONTENT_WIDTH / 2;
            content.beginText();
            content.setFont(bold, 10);
            content.newLineAtOffset(MARGIN, y);
            content.showText(label1);
            content.endText();

            content.beginText();
            content.setFont(regular, 10);
            content.newLineAtOffset(MARGIN + 80, y);
            content.showText(value1);
            content.endText();

            content.beginText();
            content.setFont(bold, 10);
            content.newLineAtOffset(col2X, y);
            content.showText(label2);
            content.endText();

            content.beginText();
            content.setFont(regular, 10);
            content.newLineAtOffset(col2X + 80, y);
            content.showText(value2);
            content.endText();

            y -= LEADING;
        }

        private void writeTableRow(String[] cells, float[] widthFractions, PDFont font, boolean header)
                throws IOException {
            ensureSpace(LEADING);
            float x = MARGIN;
            int fontSize = 9;
            for (int i = 0; i < cells.length; i++) {
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(x, y);
                content.showText(truncate(cells[i], font, fontSize, widthFractions[i] * CONTENT_WIDTH));
                content.endText();
                x += widthFractions[i] * CONTENT_WIDTH;
            }
            y -= LEADING;
            if (header) {
                content.setLineWidth(0.5f);
                content.moveTo(MARGIN, y + 4);
                content.lineTo(MARGIN + CONTENT_WIDTH, y + 4);
                content.stroke();
                y -= 2;
            }
        }

        private void writeWrapped(PDFont font, int fontSize, String text) throws IOException {
            for (String line : wrap(text, font, fontSize, CONTENT_WIDTH)) {
                ensureSpace(LEADING);
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(MARGIN, y);
                content.showText(line);
                content.endText();
                y -= LEADING;
            }
        }

        private List<String> wrap(String text, PDFont font, int fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (font.getStringWidth(candidate) / 1000 * fontSize > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private String truncate(String text, PDFont font, int fontSize, float maxWidth) throws IOException {
            if (font.getStringWidth(text) / 1000 * fontSize <= maxWidth) {
                return text;
            }
            String truncated = text;
            while (!truncated.isEmpty() && font.getStringWidth(truncated + "...") / 1000 * fontSize > maxWidth) {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            return truncated + "...";
        }

        private void close() throws IOException {
            content.close();
        }
    }
}
