package com.chroniccare.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MedicalRecordPdfService {

    public void exportToPdf(String content, String path) throws IOException {
        exportToPdf(content, path, null, null, List.of());
    }

    public void exportToPdf(
            String content,
            String path,
            String patientName,
            String mainCondition,
            List<String> secondaryConditions
    ) throws IOException {
        String safeContent = sanitize(content);
        String safePatientName = sanitize(patientName);
        String safeMainCondition = sanitize(mainCondition);
        List<String> safeSecondary = new ArrayList<>();
        if (secondaryConditions != null) {
            for (String c : secondaryConditions) {
                if (c != null && !c.isBlank()) safeSecondary.add(sanitize(c.trim()));
            }
        }

        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        float margin = 50f;
        float fontSize = 11f;
        float leading = 14f;
        float headingSize = 12f;

        Pattern sectionPattern = Pattern.compile("^(\\s*\\d+\\s*[).\\-]\\s*.+|\\s*Annexe\\b.*)$", Pattern.CASE_INSENSITIVE);
        Pattern bulletPattern = Pattern.compile("^\\s*[-•]\\s*(.*)$");

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH));

        try (PDDocument document = new PDDocument()) {
            PageContext ctx = startNewPage(document, 1, true, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);

            float usableWidth = ctx.page.getMediaBox().getWidth() - 2 * margin;
            boolean firstNonEmptyLineHandled = false;

            for (String rawLine : safeContent.split("\\R")) {
                String line = rawLine == null ? "" : rawLine.stripTrailing();
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    ctx.y -= leading / 2f;
                    if (ctx.y <= margin) {
                        ctx.stream.close();
                        ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                    }
                    continue;
                }

                boolean isSection = sectionPattern.matcher(trimmed).matches();
                var bulletMatcher = bulletPattern.matcher(trimmed);
                boolean isBullet = bulletMatcher.matches();

                if (!firstNonEmptyLineHandled) {
                    firstNonEmptyLineHandled = true;
                    if (!isSection && !isBullet && trimmed.toLowerCase(Locale.FRENCH).startsWith("dossier")) {
                        continue;
                    }
                }

                PDType1Font lineFont = isSection ? fontBold : fontRegular;
                float lineSize = isSection ? headingSize : fontSize;
                float indent = isBullet ? 14f : 0f;

                if (isSection) {
                    ctx.y -= 6f;
                }

                String text = trimmed;
                if (isBullet) {
                    String body = bulletMatcher.group(1);
                    text = "- " + (body == null ? "" : body.trim());
                }

                List<String> wrapped = wrapByWidth(text, lineFont, lineSize, usableWidth - indent);
                for (String w : wrapped) {
                    if (ctx.y <= margin) {
                        ctx.stream.close();
                        ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                    }

                    drawText(ctx.stream, lineFont, lineSize, margin + indent, ctx.y, w);
                    ctx.y -= leading;
                }

                if (isSection) {
                    ctx.y -= 2f;
                }
            }

            ctx.stream.close();
            document.save(path);
        }
    }

    public String importPdfText(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static class PageContext {
        private final PDPage page;
        private final PDPageContentStream stream;
        private final int pageNumber;
        private float y;

        private PageContext(PDPage page, PDPageContentStream stream, int pageNumber, float y) {
            this.page = page;
            this.stream = stream;
            this.pageNumber = pageNumber;
            this.y = y;
        }
    }

    private PageContext startNewPage(
            PDDocument document,
            int pageNumber,
            boolean firstPage,
            float margin,
            PDType1Font fontBold,
            PDType1Font fontRegular,
            String dateStr,
            String patientName,
            String mainCondition,
            List<String> secondaryConditions
    ) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(document, page);

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float usableWidth = pageWidth - 2 * margin;
        float y = pageHeight - margin;

        if (firstPage) {
            drawText(stream, fontBold, 18f, margin, y, "ChronicCare");
            y -= 24f;

            drawText(stream, fontBold, 14f, margin, y, "Dossier médical");
            drawTextRight(stream, fontRegular, 11f, pageWidth - margin, y, dateStr);
            y -= 18f;

            stream.setLineWidth(1f);
            stream.moveTo(margin, y);
            stream.lineTo(pageWidth - margin, y);
            stream.stroke();
            y -= 16f;

            if (patientName != null && !patientName.isBlank()) {
                y = drawWrappedText(stream, fontRegular, 11f, margin, y, usableWidth, "Patient : " + patientName);
                y -= 2f;
            }
            if (mainCondition != null && !mainCondition.isBlank()) {
                y = drawWrappedText(stream, fontRegular, 11f, margin, y, usableWidth, "Condition principale : " + mainCondition);
                y -= 2f;
            }
            if (secondaryConditions != null && !secondaryConditions.isEmpty()) {
                y = drawWrappedText(stream, fontRegular, 11f, margin, y, usableWidth, "Maladies secondaires : " + String.join(", ", secondaryConditions));
                y -= 2f;
            }

            y -= 6f;
        } else {
            drawText(stream, fontBold, 12f, margin, y, "ChronicCare - Dossier médical");
            drawTextRight(stream, fontRegular, 10f, pageWidth - margin, y, dateStr);
            y -= 14f;

            stream.setLineWidth(1f);
            stream.moveTo(margin, y);
            stream.lineTo(pageWidth - margin, y);
            stream.stroke();
            y -= 18f;
        }

        return new PageContext(page, stream, pageNumber, y);
    }

    private static void drawText(PDPageContentStream stream, PDType1Font font, float fontSize, float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text == null ? "" : text);
        stream.endText();
    }

    private static void drawTextRight(PDPageContentStream stream, PDType1Font font, float fontSize, float rightX, float y, String text) throws IOException {
        String t = text == null ? "" : text;
        float width = font.getStringWidth(t) / 1000f * fontSize;
        drawText(stream, font, fontSize, rightX - width, y, t);
    }

    private static float drawWrappedText(
            PDPageContentStream stream,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            float maxWidth,
            String text
    ) throws IOException {
        float leading = 14f;
        for (String line : wrapByWidth(text, font, fontSize, maxWidth)) {
            drawText(stream, font, fontSize, x, y, line);
            y -= leading;
        }
        return y;
    }

    private static List<String> wrapByWidth(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) return List.of("");

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : s.split("\\s+")) {
            if (word.isEmpty()) continue;

            String candidate = current.length() == 0 ? word : current + " " + word;
            if (textWidth(font, fontSize, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }

            if (textWidth(font, fontSize, word) <= maxWidth) {
                current.append(word);
                continue;
            }

            String remaining = word;
            while (!remaining.isEmpty()) {
                int cut = 1;
                while (cut < remaining.length() && textWidth(font, fontSize, remaining.substring(0, cut + 1)) <= maxWidth) {
                    cut++;
                }
                lines.add(remaining.substring(0, cut));
                remaining = remaining.substring(cut);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private static float textWidth(PDType1Font font, float fontSize, String text) throws IOException {
        String t = text == null ? "" : text;
        return font.getStringWidth(t) / 1000f * fontSize;
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        String normalized = input
                .replace('\u00A0', ' ')
                .replace('’', '\'')
                .replace('‘', '\'')
                .replace('“', '"')
                .replace('”', '"')
                .replace('–', '-')
                .replace('—', '-')
                .replace('−', '-')
                .replace('•', '-')
                .replace('✓', ' ')
                .replace('✗', ' ')
                .replace('⏳', ' ')
                .replace("  ", " ")
                .trim();

        CharsetEncoder encoder = Charset.forName("windows-1252").newEncoder();
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            sb.append(encoder.canEncode(ch) ? ch : '?');
        }
        return sb.toString();
    }
}
